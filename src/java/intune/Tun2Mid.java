
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
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Track;
import javax.sound.midi.spi.MidiFileWriter;;

/**
 *  Class <tt>Tun2Mid</tt> converts a Intunation Editor file to a Midi file.
 */
public class Tun2Mid implements Runnable {

  // tempo (microsec per metronome click, usually a quarter note), 
  // equivalent to 120 clicks per minute, which is the Midi default.
  // (Note that tempo value doesn't matter, since all timing is 
  // determined by explicit time values, but some tempo value must be used
  // to give Midi time values (ticks) a meaning to the Midi file reader.)
  private static final double TEMPO = 500000;  

  // Midi ticks per metronome click (nominally per quarter note)
  private static final int RESOLUTION = 384;  

  // constructor args
  private String infile;
  private String outfile;
  private String copyright;

  /**
   *  Constructor
   *  @param   infile
   *  @param   outfile
   *  @param   copyright
   */
  public Tun2Mid(String infile, String outfile, String copyright) {
    this.infile = infile;
    this.outfile = outfile;
    this.copyright = copyright;
    new Thread(this).start();
  }

  /**
   *  Reads intunation file and writes it out as midi file.
   */
  public void run() {
    try {
      // read in the intunation file
      IntuneFile input = new IntuneFile(null, null, Constants.NONE);
      input.read(this.infile);

      // create a "Midi output device" that will redirect its input
      // to a Midi sequence
      SequenceBuildingDevice device = 
        new SequenceBuildingDevice(Sequence.PPQ, RESOLUTION);

      // go through the motions of playing the input file, which will have
      // the effect of building a Midi sequence
      convertToSequence(input, device);

      // extract the sequence from the builder
      Sequence sequence = device.getSequence();

      // write out the Midi file
      MidiFileWriter writer = new IntuneMidiFileWriter(RESOLUTION);
      writer.write(sequence, 
                   Constants.Midi.File.SINGLE_TRACK, 
                   new File(this.outfile));

    } catch(IOException e) {
      e.printStackTrace();
    }
  }

  /**
   *  Plays an intunation file, interpreting all its tuning requests
   *  (modulations and subdominant directives) to produce and output
   *  the appropriate pitch bend messages.
   *  @param   input
   *           the intunation file   
   *  @param   device
   *           the output device to which the file is played
   */
  private void convertToSequence(IntuneFile input,  
                                 SequenceBuildingDevice device) {

    // send copyright if there is one
    if (this.copyright != null) {
      device.sendCopyright(this.copyright);
    }
    
    // make sure the pitch bend range is what we expect
    device.setPitchBendRange(Constants.Midi.PITCH_BEND_RANGE); 
 
    // set the timbre on all the output device's channels
    int timbre = input.getTimbre();
    if (timbre == Constants.NONE) {
      System.err.println("Missing timbre!");
      throw new RuntimeException("Missing timbre!");
    }
    device.setTimbre(timbre);

    // establish the tempo (it's just the default tempo)
    device.setTempo(TEMPO);

    // use a tuning receiver so output can be in true intonation
    Receiver output = new TuningReceiver(input.getIntonation(),
                                         device.getChannelCount(),
                                         device.getReceiver()); 

    // develop conversion factor from microseconds to Midi ticks
    double microsPerDeltaUnit = computeMicrosPerDeltaUnit();

    // send the messages in the input file to the output,
    // first converting their timestamps to Midi ticks
    IntuneFile.Message message = null;
    while ((message = input.nextMessage()) != null) {
      long nanos = message.timestamp;
      long micros = (long)(((double)nanos / 1000) + 0.5);
      long ticks = (long)((micros / microsPerDeltaUnit) + 0.5);
      output.send(message.message, ticks);
    }//while

    // include end of track message
    device.sendEndOfTrack();
  }

  /**
   *  Returns number of microseconds per midi delta time unit.
   *  @result  microseconds / delta unit
   */
  private double computeMicrosPerDeltaUnit() {
    // we use PPQ timing, so the number of microsec per delta time unit
    // is given by the tempo (in microsec epr mitronome click) divided
    // by the resolution (in delta units per click)
    return TEMPO / RESOLUTION;
  }

  /**
   *  Static inner class <tt>SequenceBuildingDevice</tt> implements
   *  a "Midi output device" that redirects Midi messages sent to it
   *  to put them into a Midi sequence.
   */
  private static class SequenceBuildingDevice extends Midi.OutputDevice {

    // the sequence being constructed
    Sequence sequence;

    // latest timestamp so far  
    long latestTimestamp;

    /**
     *  Constructor.
     *  @param   diviisonType
     *  @param   resolution
     */
    SequenceBuildingDevice(float divisionType, int resolution) {
      try {
        this.sequence = new Sequence(divisionType, resolution);
      } catch(InvalidMidiDataException e) {
        throw new IllegalArgumentException("Invalid Midi division type (" +
          + divisionType + ") or resolution (" + resolution + ")");
      }
      this.receiver = new SequenceBuildingReceiver(this.sequence);
    }

    /**
     *  Overrides method in Midi.OutputDevice to send message to
     *  this device's receiver with latest timestamp. 
     *  @param   message
     */
    public void sendMessage(ShortMessage message) {
      this.receiver.send((MidiMessage)message, this.latestTimestamp); 
    }

    /**
     *  Sends a message to this device's receiver with a given timestamp,
     *  which becomes the latest timestamp.
     *  @param   messsage
     *  @param   timestamp
     */
    public void sendMessage(MidiMessage message, long timestamp) {
      this.receiver.send(message, timestamp);
      this.latestTimestamp = timestamp;
    }

    /**
     *  Returns the Midi sequence built by this receiver.
     *  @result  the constructed sequence
     */
    Sequence getSequence() {
      return this.sequence;
    }

    /**
     *  Sets the tempo.
     *  @param   tempo
     *           microseconds per metronome click (usually a quarter note)
     */
    public void setTempo(double tempo) {
      MetaMessage message = new MetaMessage();
      int value = (int)tempo;
      byte[] data = new byte[3]; 
      data[2] = (byte)(value & 0xff);
      value >>= 8;
      data[1] = (byte)(value & 0xff);
      value >>= 8;
      data[0] = (byte)(value & 0xff);
      try {
        message.setMessage(Constants.Midi.Meta.SET_TEMPO, data, 3); 
      } catch(InvalidMidiDataException e) {
        throw new IllegalArgumentException("Invalid tempo " + tempo);
      }
      sendMessage((MidiMessage)message, this.latestTimestamp); 
    }

    /**
     *  Sends a copyright meta message.
     *  @param   copyright
     *           the text of the copyright message
     */
    public void sendCopyright(String copyright) {
      MetaMessage message = new MetaMessage();
      byte[] bytes = copyright.getBytes();
      byte[] data = new byte[bytes.length + 1];
      System.arraycopy(bytes, 0, data, 0, bytes.length);
      data[data.length-1] = '\0';
      try {
        message.setMessage(Constants.Midi.Meta.COPYRIGHT, data, data.length);
      } catch(InvalidMidiDataException e) {
        throw new IllegalArgumentException("Invalid copyright " + copyright);
      }
      sendMessage((MidiMessage)message, this.latestTimestamp); 
    }

    /**
     *  Sends an end of track meta message.
     */
    //does this need an explicit timestamp? 
    public void sendEndOfTrack() {
      MetaMessage message = new MetaMessage();
      try {
        message.setMessage(Constants.Midi.Meta.END_OF_TRACK, new byte[]{  }, 0);
      } catch(InvalidMidiDataException e) {
        throw new IllegalArgumentException("Should not happen");
      }
      sendMessage((MidiMessage)message, this.latestTimestamp); 
    }
  }/** end of static inner class RedirectingOutputDevice */

  /**
   *  Static inner class <tt>SequenceBuildingReceiver</tt> acts as a
   *  Midi receiver and puts all messages sent through it into
   *  a Midi sequence.
   */
  private static class SequenceBuildingReceiver implements Receiver {
    Sequence sequence;
    Track track;
    SequenceBuildingReceiver(Sequence sequence) {
      this.sequence = sequence;
      this.track = sequence.createTrack();
    }

    /**
     *  Adds a Midi message and its timestamp to the sequence this
     *  receiver is building.
     *  @see Receiver
     */
    public void send(MidiMessage message, long timestamp) {
      this.track.add(new MidiEvent(message, timestamp)); 
    }

    /**
     *  Does nothing: required by <tt>Receiver</tt> interface.
     *  @see Receiver
     */
    public void close() {  }
  }/** end of static inner class SequenceBuildingReceiver */

  private void displaySequence(Sequence sequence, double microsPerDeltaUnit) {
    System.out.println();
    Track track = sequence.getTracks()[0];
    int size = track.size();
    for (int index = 0; index < size; index++ ) {
      MidiEvent event = track.get(index);
      long timestamp = event.getTick();
      MidiMessage message = event.getMessage();
      int status = message.getStatus() & 0xf0;
      long usec = (long)(microsPerDeltaUnit * timestamp + 0.5);
      System.out.println("tick = " + timestamp + " = " + usec + " usec" +
        ", status = " + status);
    } 
  }

  /**
   *  Runs program from the command line.
   *  @param   args  args[0] is name of the input (intunation) file,
   *                 args[1] is name of the output (midi) file.
   */
  public static void main(String[] args) {
    if (args.length < 2) {
      System.err.println(
        "Usage: java Tun2Mid intunationInput midiOutput");     
      System.exit(-1);
    } 
    String copyright = null;
    if (args.length > 2) {
      copyright = args[2];
    }
    new Tun2Mid(args[0], args[1], copyright);
  }  

}/** end of class Tun2Mid */
