
/**
 *  Intune   Intonation Exploration System
 *  Copyright (c) 2005-2014 Michael E. Goldsby
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package intune;
import java.io.File;
import java.io.IOException;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiFileFormat;
import javax.sound.midi.spi.MidiFileReader;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Track;

/**
 *  Class <tt>Mid2Tun</tt> converts a Midi file to an Intune file.
 */
public class Mid2Tun implements Runnable {

  // default tempo (microsec per metronome click, usually a quarter note), 
  // equivalent to 120 clicks per minute
  private static final double DEFAULT_TEMPO = 500000;  

  // timbre if input file specifies none
  private static final int DEFAULT_TIMBRE = 6;   // harpsichord

  // constructor args
  private String keyName;
  private String infile;
  private String outfile;

  // info about midi file format
  private MidiFileFormat format;

  // the midi sequence from the file
  private Sequence sequence;

  // current track of the input sequence
  private Track track;

  // microsec per metronome click (usually a quarter note)
  private double tempo = DEFAULT_TEMPO;
 
  // microsec per midi delta time unit
  private double microsPerDeltaUnit;

  // in-memory intune file
  private IntuneFile file;

  /**
   *  Constructor
   *  @param   keyName
   *  @param   infile
   *  @param   outfile
   */
  public Mid2Tun(String keyName, String infile, String outfile) {
    this.keyName = keyName;
    this.infile = infile;
    this.outfile = outfile;
    new Thread(this).start();
  }

  /**
   *  Reads midi file, converts it and write it out.
   */
  public void run() {
    try {
      // get file format and make sure it's single-track
      MidiFileReader reader = new IntuneMidiFileReader();
      File midi = new File(this.infile);
      this.format = reader.getMidiFileFormat(midi);
      if (this.format.getType() != IntuneMidiFileReader.SINGLE_TRACK) {
        System.err.println("At present we only handle single-track " +
          "(type "  + IntuneMidiFileReader.SINGLE_TRACK + ") files " +
          "and this one is of type " + this.format.getType());
        System.exit(-1);
      }

      // Must learn how to interpret the midi delta time units.
      // If timing is PPQ (which it normally is), the conversion from
      // delta time units to microseconds, say, depends on the tempo
      // and will change every time we encounter a midi tempo meta event.
      // If timing is SMPTE, the conversion factor is constant for
      // the file. 
      this.microsPerDeltaUnit = computeMicrosPerDeltaUnit();

      // get the file's sequence
      this.sequence = reader.getSequence(midi);
      displaySequence();   // for testing

      // consume sequence data and produce intunation events
      IntuneFile intuneFile = convertSequence();

      // write out the intunation file
      intuneFile.write(this.outfile);

    } catch(InvalidMidiDataException e) {
      e.printStackTrace();
    } catch(IOException e) {
      e.printStackTrace();
    }
  }

  /**
   *  Returns number of microseconds per midi delta time unit.
   *  @result   us / delta unit
   *  @throws   InvalidMidiDataException
   *            if invalid division type in midi format info
   */
  private double computeMicrosPerDeltaUnit() throws InvalidMidiDataException {
    double factor = 1;
    float divisionType = this.format.getDivisionType();
    int resolution = this.format.getResolution();

    // for PPQ timing, the number of microsec per delta time unit is 
    // given by the current tempo (in microsec per metronome click) 
    // divided by the resolution (in delta units per click)
    if (divisionType == Sequence.PPQ) {
      factor = this.tempo / resolution;

    // for SMPTE timing, the number of microsec per delta time unit is
    // given by the number of frames per second (which happens to be
    // the division type) times the number of subframes per frame 
    // (the resolution again)
    } else {
      factor = divisionType * resolution;
    }

    // return microsec per delta time unit
    return factor;
  }

  /**
   *  Returns microseconds per metronome click (normally a quarter note)
   *  given data from Set Tempo meta message.
   *  @param   data
   *           data from meta message (3 bytes, unsigned, msb first)
   *  @result  microseconds per click
   */
  private double computeTempo(byte[] data) {
    int msb = unsignedByte(data[0]);
    int mid = unsignedByte(data[1]);
    int lsb = unsignedByte(data[2]);
    int microsPerClick = ((((msb) << 8) + mid) << 8) + lsb;
    //int microsPerClick = (msb * 256 + mid) * 256 + lsb;
    return (double)microsPerClick;
  }

  /**
   *  Returns integer value of a byte if byte is considered to be unsigned.
   *  @param   b
   *           the byte
   *  @result  a value in the range 0 to 255
   */
  private int unsignedByte(byte b) {
    return (b >= 0 ? b : b + 256);
  } 

  /**
   *  Converts midi sequence data to produce an in-memory intunation
   *  file and returns it.
   *  @result  intunation file
   *  @throws   InvalidMidiDataException
   *            if sequence contains invalid midi data
   */ 
  private IntuneFile convertSequence() throws InvalidMidiDataException {

    // create an empty intune file structure
    IntuneFile intuneFile = new IntuneFile(
                              Constants.IntonationName.DIATONIC,
                              this.keyName, 
                              DEFAULT_TIMBRE);
    intuneFile.open();

    // get the (single) input track
    this.track = this.sequence.getTracks()[0];

    // for each event in the track..
    int size = this.track.size();
    for (int index = 0; index < size; index++) {
      MidiEvent event = this.track.get(index);

      // get event's timestamp
      long tick = event.getTick();
      double micros = this.microsPerDeltaUnit * tick;

      // if event's message is a Note On or Note Off, 
      // add it to the intune file
      MidiMessage message = event.getMessage();
      int status = message.getStatus() & 0xf0;      
      if (status == ShortMessage.NOTE_ON || status == ShortMessage.NOTE_OFF) {
        ShortMessage noteMessage = (ShortMessage)message;
        long nanos = (long)(micros * 1000);
        int noteNumber = noteMessage.getData1();
        int velocity = (status == ShortMessage.NOTE_ON ? noteMessage.getData2() :                                                          0);
        intuneFile.insertNoteMessage(nanos, noteNumber, velocity); 
        System.out.println("Inserted note message for note " + noteNumber +
          " velocity " + velocity);

      // if the event's message is a tempo message, set the tempo
      // and recompute the timing ratio
      }  else if (status == MetaMessage.META) {
        MetaMessage meta = (MetaMessage)message;
        int type = meta.getType();
        System.out.println("Read meta message, type = " + type);
        if (type == Constants.Midi.Meta.SET_TEMPO) {
          byte[] data = ((MetaMessage)message).getData(); 
          this.tempo = computeTempo(data);
          this.microsPerDeltaUnit = computeMicrosPerDeltaUnit();
          System.out.println(
            "usec per delta unit now = " + this.microsPerDeltaUnit);
        }
      // if the event is anything else, just ignore it
      } else {
        System.out.println("Read message with status = " + status);
      }
    }

    // return the intune file data structure
    return intuneFile;
  }

  private void displaySequence() {
    System.out.println();
    Track track = this.sequence.getTracks()[0];
    int size = track.size();
    for (int index = 0; index < size; index++ ) {
      MidiEvent event = track.get(index);
      long timestamp = event.getTick();
      MidiMessage message = event.getMessage();
      int status = message.getStatus() & 0xf0;
      long usec = (long)(this.microsPerDeltaUnit * timestamp + 0.5);
      System.out.println("tick = " + timestamp + " = " + usec + " usec" +
        ", status = " + status);
    } 
  }

// Note that an IntuneFile inserts in time order, so could handle 
// multi-track data if wanted to.  There's the trickiness with the 
// tempo changes being in the first track, but that could be handled.

  /**
   *  Runs program from the command line.
   *  @param   args  args[0] is name of the input (midi) file,
   *                 args[1] is name of the output file.
   */
  public static void main(String[] args) {
    if (args.length < 3) {
      System.err.println(
        "Usage: java Mid2Tun keyName midiInput intunationOutput");     
      System.exit(-1);
    } 
    new Mid2Tun(args[0], args[1], args[2]);
  }  

}/** end of class Mid2Tun */
