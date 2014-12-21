
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
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.List;
import java.util.LinkedList;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Transmitter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 *  Class <code>Playback</code> reads a file produced by the
 *  Editor (q.v.) containing Note On, Note Off and Tuning Request
 *  MIDI commands and sends Note On, Note Off and Pitch Bend
 *  commands to the MIDI output device (presumably one that
 *  can produce sound, also conceivably a recording device).  
 *  <p>
 *  The Tune Request commands include (are followed by)
 *  information specifying a modulation or the start or end of
 *  subdominant tuning.  (The use of MIDI Tune Request commands
 *  is a sort of pun, since they were originally intended to
 *  command analog devices to tune their ocsillators.)  
 *  <p>
 *  The file produced by the Editor also contains, at its beginning,
 *  the class name of an <tt>Intonation</tt> class, possibly
 *  followed by a key indicator.  This intonation will be applied 
 *  to the output unless it is overridden in the command line.
 *  <p>
 *  Usage: <code>java intune.Playback infile [ tuning [ key ] ]
 *     [ -kb kbIndex ] [ -t timbre ]</code>
 *
 *  where <tt>infile</tt> is the name of the input file,
 *  <code>tuning</code> is the name type of intonation,
 *  <code>key</code> is ( 'A' | 'B' | 'C' | 'D' | 'E' | 'F' | 'G' | 
 *  'a' | 'b' | 'c' | 'd' | 'e' | 'f' | 'g' ) [ ( '#' | '-' ) ],
 *  where the capital letters stand for major keys, the lower case 
 *  letters stand for minor keys and '#' and '-' stand for sharp
 *  and flat, respectively; <code>kbINdex</code> is the MIDI device index of
 *  the MIDI keyboard, and <code>timbre</code> is a MIDI bank/program 
 *  number (bank = timbre div 128, program number = timbre mod 128). 
 *  <p>
 *  The recognized tunings are currently 'diatonic', 
 *  'pythagorean' and 'equaltemperament'.  The argument must be a
 *  head of one of these strings and long enough to tell them apart.
 *  (I know, a single letter will suffice, but there may be other
 *  tunings later on.)  Note that not all tunings require that the key 
 *  be given (for instance, diatonic does but equaltemperament doesn't).
 *  If the tuning name is none of the above, it is taken to be
 *  the Java class name of the tuning (an <tt>Intonation</tt> class).
 *  <p>
 *  The auxiliary device indicates modulations and selection of the
 *  lowered (subdominant) tuning of the 2nd and minor 7th notes of
 *  the scale.  Currently, the auxiliary device is a second MIDI
 *  keyboard rigged to look act like a set of foot pedals.  One pedal
 *  is reserved to indicate lowered 2nds and minor 7ths when pressed
 *  and normal (dominant) 2nds and 7ths when released.  Twelve other
 *  pedals indicate the key to which to modulate.
 *  <p>
 *  Inputting a 'q' or 'Q' from the keyboard stops the program.
 *  It's best to stop it this way, because memory is not released
 *  if you just control-C it off.
 *
 *  @version 0.9  November 2005
 */
public class Playback implements Runnable
  {
  /** names of the recognized tunings */
  private static final String DIATONIC = "diatonic";
  private static final String PYTHAGOREAN = "pythagorean";
  private static final String EQUAL_TEMPERAMENT = "equaltemperament";

  /** a useful constant */
  private static final int NONE = -1;

  /** default values for command-line options (MIDI device indices) */
  private static final int DEFAULT_SYNTH_INDEX = NONE;

  /** default number of channels */
  private static final int DEFAULT_NR_OF_CHANNELS = 16;

  /** the percussion channel (not much use to us) */
  private static final int PERCUSSION_CHANNEL = 9;

  /** the total pitch bend range (semitones) */
  private static final int PITCH_BEND_RANGE = 2;

  /** registered parameter number (RPN) selector MSB and LSB */
  private static final int RPN_MSB = 0x65;
  private static final int RPN_LSB = 0x64;

  /** pitch bend range MSB, LSB (for RPN) */
  private static final int PB_RANGE_MSB = 0x00;
  private static final int PB_RANGE_LSB = 0x00;

  /** RPN data entry MSB, LSB */
  private static final int DATA_ENTRY_MSB = 0x06;
  private static final int DATA_ENTRY_LSB = 0x26;

  /** RPN reset MSB, LSB */
  private static final int RPN_RESET_MSB          = 0x7f;
  private static final int RPN_RESET_LSB          = 0x7f;

  /** default timbre */
  private static final int DEFAULT_TIMBRE = 0;

  /** programs (timbres) per bank */
  private static final int PROGRAMS_PER_BANK  = 128;

  /** controller number for bank select MSB */
  private static final int BANK_SELECT        = 0x00;

  /** controller number for all control change LSBs */
  private static final int CONTROL_CHANGE_LSB = 0x20; 

  /** ends program if input from keyboard (not case sensitive) */
  private static final char QUIT = 'Q';

  /**
   *  Runs the <code>Playback</code> program.
   *  @param   args   command-line arguments: 
   *                  filename tuning [ key ] [-t timbre] [ -synth synthIndex ]
   */
  static public void main(String[] args)
    {
    // arguments to Playback constructor
    String fileName = null;
    String intonationName = null; 
    String key= null;
    int synthIndex = DEFAULT_SYNTH_INDEX;
    int timbre = Constants.NONE;

    try 
      {
      if (args.length == 0) 
        {
        throw new IllegalArgumentException("Missing input file name");
        }

      // get filename
      int index = 0;
      fileName = args[index++];
      
      // get intonation name and key, if any 
      if (index < args.length && !isSwitch(args[index]))
        {
        intonationName = args[index++];
        if (index < args.length && !isSwitch(args[index])) 
          {
          key = args[index++];
          }
        }

      // get options 
      while (index < args.length && isSwitch(args[index])) 
        {
        String option = args[index++];

        // synthesizer
        if (option.equals("-synth")) 
          {
          synthIndex = Integer.parseInt(args[index++]);
          }	  
        // timbre
        else if (option.equals("-t")) 
          {
          timbre = Integer.parseInt(args[index++]);
          }	  
        }//while
      }//try
    catch (Exception e) 
      {
      System.err.println(e);
      System.err.println(
        "Usage: java intune.Playback infile [ intonation [ key ] ]" + 
        " [ -synth synthIndex ] [ -t timbre ]\n" +
        "  where options may occur in any order");
      System.exit(-1);
      }

    // create the Playback object
    new Playback(fileName,
                 intonationName, 
                 key, 
                 synthIndex, 
                 timbre);
    }

  /**
   *   Returns true if argument starts with hyphen.
   *   @param   arg
   *   @result  true if arg starts with "-"
   */
  private static boolean isSwitch(String arg)
    {
    return (arg.startsWith("-"));
    }

  //////////////////////////////////////////////////////////////////
  //  instance variables
  //////////////////////////////////////////////////////////////////

  // constructor args
  String fileName;
  String intonationName;
  String keyName;
  int synthIndex;
  int timbre;

  // clock giving time relative to start of run
  Clock clock = new Clock();

  /**
   *  Constructor.
   *  @param   fileName
   *           name of input file
   *  @param   intonationName
   *           name of the tuning to be used           
   *  @param   keyName
   *           the key (null if intonation requires no key)
   *  @param   synthIndex
   *           MIDI device index of the synthesizer (may be NONE)
   *  @param   timbre
   *           the timbre number (NONE if none)
   */
  Playback(String fileName,
           String intonationName, 
           String keyName, 
           int synthIndex,
           int timbre)
    {
    this.fileName = fileName;
    this.intonationName = intonationName;
    this.keyName = keyName;
    this.synthIndex = synthIndex;
    this.timbre = timbre;
    new Thread(this).start();
    }

  /** 
   *  Main program logic.
   */
  public void run() {

    String intonationLine = null;
    DataInputStream input = null;
    try {
      // open the input file
      input = new DataInputStream(
                new BufferedInputStream(
                  new FileInputStream(this.fileName)));  

      // read the intonation line 
      // (if there isn't one, substitute blank line--the file may have
      // been produced by the 'save' feature of PlayTuned)
      intonationLine = " ";
      try {
        intonationLine = input.readUTF();
      } catch(Exception e) {  
        input.close();
        input = new DataInputStream(
                  new BufferedInputStream(
                    new FileInputStream(fileName)));
      }
    } catch(IOException e) {
      System.err.println("Cannot read input file");
      e.printStackTrace();
      System.exit(-1);
    }

    // extract the intonation class name (and possibly key)
    // and the timbre from the intonation line
    String recordedIntonationName = null;
    String recordedKeyName = null;
    int recordedTimbre = Constants.NONE;
    StringTokenizer tokenizer = new StringTokenizer(intonationLine);
    List tokenList = new ArrayList();
    while (tokenizer.hasMoreTokens()) {
      tokenList.add(tokenizer.nextToken()); 
    }
    String[] tokens = (String[])tokenList.toArray(new String[0]);
    int index = 0;
    if (index < tokens.length && !isSwitch(tokens[index])) {
      recordedIntonationName = tokens[index++];
      if (index < tokens.length && !isSwitch(tokens[index])) {
        recordedKeyName = tokens[index++];
      }
    }
    if (index < tokens.length && isSwitch(tokens[index])) {
      String option = tokens[index++];
      if (option.equals("-t")) {
        recordedTimbre = Integer.parseInt(tokens[index++]); 
      } else {
        System.err.println(
          "Warning: unrecognized option in intonation line");
      }
    }
    System.out.println(
      "Recorded with intonation " + recordedIntonationName +
      (recordedKeyName != null ? ", key " + recordedKeyName : "") +
      " and timbre " + 
      (recordedTimbre != Constants.NONE ? recordedTimbre : Constants.NONE));
 
    // give preference to the intonation and key names and timbre passed
    // in the constructor, otherwise use the ones in the input file
    if (this.intonationName == null) {
      this.intonationName = recordedIntonationName;
      this.keyName = recordedKeyName;
    }
    if (this.timbre == Constants.NONE) {
      this.timbre = recordedTimbre;
    }

    // create an intonation object of the specified type
    Intonation intonation = null;
    String name = this.intonationName.toLowerCase();
    if (DIATONIC.startsWith(name)) 
      {
      Note key = keyNameToNote(this.keyName);
      System.out.println("Note " + key + " for keyName " + this.keyName);
      Diatonic.ScaleType type = (Character.isUpperCase(keyName.charAt(0)) ?
                                   Diatonic.ScaleType.MAJOR :		      
                                   Diatonic.ScaleType.MINOR);      
      System.out.println("Scale type " + type);
      try
        {
        intonation = new Diatonic(new Diatonic.Key(key, type), 
                       new Pythagorean().frequency(key)); 
        }
      catch(UnsupportedNoteException e)
        {
        throw new IllegalArgumentException(
          "Cannot build Diatonic Intonation with given key name");
        }
      }
    else if (PYTHAGOREAN.startsWith(name))
      {
      if (this.keyName != null)
        {
        Note key = keyNameToNote(this.keyName);
        intonation = new Pythagorean(key);
        }
      else
        {
        intonation = new Pythagorean();
        }
      }
    else if (EQUAL_TEMPERAMENT.startsWith(name))
      {
      intonation = new EqualTemperament();
      }
    else
      {
      // must be the class name of a temperament
      String className = this.intonationName;
      Class intonationClass = null;
      try 
        {
        intonationClass = Class.forName(className);
        }
      catch(ClassNotFoundException e)
        {
        System.err.println("Cannot find intonation class " + className);
        System.exit(-1);
        }
      try
        {
        intonation = (Intonation)intonationClass.newInstance();
        }
      catch(ClassCastException e)
        {
        System.err.println(className + " is not an Intonation class");
        System.exit(-1);
        }
      catch(Exception e)
        {
        System.err.println("Instantiating intonation class: " + e);
        System.exit(-1);
        }
      }

    // initialize the MIDI output device 
    MidiDevice synth = null;
    Receiver synthReceiver = null;
    int nrOfChannels = 0;
    try 
      {
      // get info about the MIDI devices
      MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();

      // open the synthesizer and extract its receiver
      if (this.synthIndex == NONE) 
        { 
        synth = MidiSystem.getSynthesizer();
        }
      else
        {
        synth = MidiSystem.getMidiDevice(info[this.synthIndex]);
        }
      synth.open();
      synthReceiver = synth.getReceiver();
      System.out.println("synth index = " + this.synthIndex +
		         " output device = " + synth);

      // learn how many channels the synthesizer supports 
      // and set the pitch bend range and timbre for all of them
      nrOfChannels = findNumberOfChannels(synth);
      System.out.println("Synthesizer supports " + nrOfChannels + " channels");
      setPitchBendRange(synthReceiver, nrOfChannels, PITCH_BEND_RANGE);
      int theTimbre =
        (this.timbre != Constants.NONE ? this.timbre : DEFAULT_TIMBRE);
      setTimbre(synthReceiver, nrOfChannels, theTimbre);
      }
    catch(Exception e) 
      {
      e.printStackTrace();
      System.exit(-1); 
      }

    System.out.println("Intonation = " + intonation.getClass().getName());

    // create the tuning receiver and connect it to file input on
    // one side and the synthesizer on the other side
    try
      {
      // make a special receiver that adjusts the pitch of input 
      // notes and sends the adjusted notes to the synthesizer
      TuningReceiver tuningReceiver = new TuningReceiver(
                       intonation, nrOfChannels, synthReceiver);

      // open the input file and start a thread that reads it
      // and sends commands to the tuning receiver and
      // calls the tuning-adjustment methods of the intonation
      // (provided it is an intonation capable of modulation)
      startFileReader(input, intonation, tuningReceiver); 
      }
    catch(Exception e) 
      {
      e.printStackTrace();
      System.exit(-1); 
      }

    // wait until receive indication to quit 
    waitUntilDone();

    // close the synthesizer and the input file
    synth.close();
    try
      {
      input.close();
      }
    catch(IOException e) {  }

    // announce program ended
    System.out.println("Program ended");
    }

  /**
   *  Returns number of channels supported by synthesizer.
   *  @param   synth
   *           the synthesizer device
   *  @result  the number of channels
   */
  private int findNumberOfChannels(MidiDevice synth)
    {
    int number = DEFAULT_NR_OF_CHANNELS;
    if (synth instanceof Synthesizer)
      {
      MidiChannel[] channels = ((Synthesizer)synth).getChannels();
      while (number < channels.length && channels[number] != null)
        {
        number++;
        }
      }
    return number;
    }

  /**
   *  Sets pitch bend range on all of the output device's channels.
   *  @param   synthReceiver
   *           the synthesizer's receiver
   *  @param   channelCount
   *           number of channels in the synthesizer
   *  @param   range
   *           total pitch bend range in semitones
   */
  private void setPitchBendRange(Receiver synthReceiver,
		                 int channelCount,
				 int range) {

    // for each channel..
    for (int channel = 0; channel < channelCount; channel++) {

      // set up the sequence of messages
      // (note use half of total range as semitone value)
      ShortMessage pbRangeMSB = new ShortMessage();
      ShortMessage pbRangeLSB = new ShortMessage();
      ShortMessage setPBRangeSemitones = new ShortMessage();
      ShortMessage setPBRangeCents = new ShortMessage();
      ShortMessage rpnResetMSB = new ShortMessage();
      ShortMessage rpnResetLSB = new ShortMessage();
      try {
        pbRangeMSB.setMessage(ShortMessage.CONTROL_CHANGE,
                              channel,
                              RPN_MSB,
                              PB_RANGE_MSB);
        pbRangeLSB.setMessage(ShortMessage.CONTROL_CHANGE,
                              channel,
                              RPN_LSB,
                              PB_RANGE_LSB);
        setPBRangeSemitones.setMessage(ShortMessage.CONTROL_CHANGE,
                                       channel,
                                       DATA_ENTRY_MSB,
                                       range / 2);
        setPBRangeCents.setMessage(ShortMessage.CONTROL_CHANGE,
                                   channel,
                                   DATA_ENTRY_LSB,
                                   0);
        rpnResetMSB.setMessage(ShortMessage.CONTROL_CHANGE,
                               channel,
                               RPN_MSB,
                               RPN_RESET_MSB);
        rpnResetLSB.setMessage(ShortMessage.CONTROL_CHANGE,
                               channel,
                               RPN_LSB,
                               RPN_RESET_LSB);
  
      } catch(InvalidMidiDataException e) {
        System.err.println("Error setting pitch bend range");
        e.printStackTrace();
        System.exit(-1);
      }
  
      // send the messages
      sendMessage(synthReceiver, pbRangeMSB, -1);
      sendMessage(synthReceiver, pbRangeLSB, -1);
      sendMessage(synthReceiver, setPBRangeSemitones, -1);
      sendMessage(synthReceiver, setPBRangeCents, -1);
      sendMessage(synthReceiver, rpnResetMSB, -1);
      sendMessage(synthReceiver, rpnResetLSB, -1);
    }//for
  }

  /**
   *  Sets timbre on all of the output devices's channels.
   *  @param   synthReceiver
   *           synthesizer's receiver
   *  @param   channelCount
   *           number of channels in the synthesizer
   *  @param   timbre
   *           the timbre (MIDI program number)
   */
  private void setTimbre(Receiver synthReceiver, 
		                  int channelCount, 
				  int timbre) {

    // get bank and program numbers
    int bank = timbre / PROGRAMS_PER_BANK;
    int program = timbre % PROGRAMS_PER_BANK;

    // send a program change message to each channel
    for (int channel = 0; channel < channelCount; channel++) {

      // if bank is not zero, send a bank select
      if (bank > 0) {
        int bankMSB = bank / 128;          	      
        int bankLSB = bank % 128;          	      
        ShortMessage bankMSBmessage = new ShortMessage();
        ShortMessage bankLSBmessage = new ShortMessage();
        try {
          bankMSBmessage.setMessage(ShortMessage.CONTROL_CHANGE,
                                    channel,
                                    BANK_SELECT,
                                    bankMSB);
          bankLSBmessage.setMessage(ShortMessage.CONTROL_CHANGE,
                                    channel,
                                    CONTROL_CHANGE_LSB,
                                    bankLSB);
        } catch(InvalidMidiDataException e) {
          System.err.println("Invalid  " + timbre);
          System.exit(-1);
        } 	
        sendMessage(synthReceiver, bankMSBmessage, -1);
        sendMessage(synthReceiver, bankLSBmessage, -1);
      }

      // send a program change
      ShortMessage timbreMessage = new ShortMessage();
      try {
        timbreMessage.setMessage(ShortMessage.PROGRAM_CHANGE,
                                 channel,
                                 program,
                                 0);
      } catch(InvalidMidiDataException e) {
        System.err.println("Invalid timbre " + timbre);
        System.exit(-1);
      }
      sendMessage(synthReceiver, timbreMessage, -1);

    }//for
  }

  /**
   *  Starts a thread that reads the input data and sends
   *  commands into the MIDI output pipeline.
   *  @param   input
   *           input command stream
   *  @param   intonation
   *           the tuning in use
   *  @param   output
   *           receiver of output commands
   */
  private void startFileReader(final DataInputStream input, 
                               final Intonation intonation,
                               final TuningReceiver output) 
    {
    Thread t = new Thread() 
      {
      public void run()
        {
        readInWriteOut(input, intonation, output);     
        }
      };
    t.setDaemon(true);
    t.start();
    }

  /**
   *  Reads the input data and sends commands into the MIDI output pipeline.
   *  @param   input
   *           input command stream
   *  @param   intonation
   *           the tuning in use
   *  @param   output
   *           receiver of output commands
   */
  private void readInWriteOut(DataInputStream input, 
                              Intonation intonation,
                              TuningReceiver output)
    {
    // discover whether tuning can modulate
    Modulator modulator = null;
    if (intonation instanceof Modulator)
      {
      modulator = (Modulator)intonation;
      }

    // until end of file..
    boolean eof = false;
    while (!eof)
      {
      try
        {
        // if next item is MIDI command..
        int command = input.readByte() & 0x0ff;
        if (command != ShortMessage.TUNE_REQUEST)
          {
          // read the message and the timestamp following it
          ShortMessage message = readMessage(input, command); 
          long timestamp = readTimestamp(input);

          // wait until that time and send the message
          waitUntil(timestamp);
          sendMessage(output, message, -1);
          }

        // if next item if tuning request..
        else 
          {
          // if it is a modulation..
          int type = input.readByte() & 0x0ff;
          if (type == Constants.TuneRequest.MODULATION)
            {
            // read note number of key to modulate to and modulate to that key
            int noteNumber = input.readByte() & 0x0ff;
            if (modulator != null)
              {
              Note note = null; 
              try
                {
                note = intonation.midiNoteNumberToNote(noteNumber);
                modulator.modulate(note);
                }
              catch(UnsupportedNoteException e)
                {
                System.err.println("Cannot carry out modulation to " + note);
                }
              } 

            // read and ignore timestamp
            long timestamp = readTimestamp(input);
            }


          // if it is a subdominant directive..
          else if (type == Constants.TuneRequest.SUBDOMINANT)
            {
            // read state (on or off) and set subdominant to that state
            boolean state = input.readBoolean();
            if (modulator != null)
              {
              modulator.setSubdominant(state); 
              } 

            // read and ignore timestamp
            long timestamp = readTimestamp(input);
            }
          else
            {
            System.err.println("Unrecognized tuning request: " + type);
            System.exit(-1);
            }
          }
        }
      catch(EOFException e)
        {
        eof = true;
        }
      catch(IOException e)
        {
        System.err.println("Cannot read input file");
        e.printStackTrace();
        System.exit(-1);
        }
      }//while not eof
    }

  /**
   *  Reads a MIDI message from the input.
   *  @param   input
   *           the input
   *  @param   command
   *           command for the message
   *  @result  MIDI message
   */
  private ShortMessage readMessage(DataInputStream input,
                                   int command) throws EOFException
    {
    ShortMessage message = null;
    int channel = -1;
    int data1 = -1;
    int data2 = -1;
    try
      {
      channel = input.readByte() & 0x0ff;
      data1 = input.readByte() & 0x0ff;
      data2 = input.readByte() & 0x0ff;
      message = new ShortMessage();
      message.setMessage(command, channel, data1, data2);
      }
    catch(InvalidMidiDataException e)
      {
      System.err.println("Can't set ShortMessage: " +
        command + " " + channel + " " + data1 + " " + data2 + ": " + e);
      throw new EOFException("read invalid data");
      }
    catch(EOFException e)
      {
      throw new EOFException();
      }
    catch(IOException e)
      {
      System.err.println("Can't read input file: " + e);
      e.printStackTrace();
      throw new EOFException("i/o error");
      }
    return message; 
    }

  /**
   *  Reads timestamp from the input and returns it.
   *  @param   input
   *           the input 
   *  @result  timestamp (nanoseconds from start)
   */
  private long readTimestamp(DataInputStream input) throws EOFException
    { 
    long timestamp = -1;
    try
      { 
      timestamp = input.readLong();
      } 
    catch(IOException e)
      {
      System.err.println("Can't read input file: " + e);
      e.printStackTrace();
      throw new EOFException("i/o error");
      }
    return timestamp;
    } 

  /**
   *  Waits until the time since the start of this replay run
   *  is at least as great as the given time interval.
   *  @param   then
   *           time interval in nanoseconds
   */
  private void waitUntil(long then) 
    {  
    final int NANOS_PER_MSEC = 1000000;
    long now = Long.MAX_VALUE;
    while ((now = this.clock.now()) < then)
      {
      try
        {
        long interval = then - now;
        long millis = interval / NANOS_PER_MSEC;
	int nanos = (int)(interval % NANOS_PER_MSEC);
        System.out.println("Sleeping until " + millis + " msec + " +
          nanos + " nsec");
        Thread.sleep(millis, nanos);
        }
      catch(InterruptedException e)
        {
        }
      }
    }

  /**
   *  Returns note corresponding to given key name.
   *  @param   name
   *           ('A'..'G''a'..'g')['#'|'-']
   *  @result  note for given 
   *  @throws  IllegalArgumentException
   *           if it's not a valid note name
   */
  private Note keyNameToNote(String name)
    {
    Note note = null;

      // get accidental if any
      Note.Accidental accidental = null;
      if (name.length() == 1)
      {
      accidental = Note.Accidental.NATURAL;
      }
    else if (name.length() == 2)
      {
      char sharpFlat = name.charAt(1);
      accidental = (sharpFlat == '#' ? Note.Accidental.SHARP :
                   (sharpFlat == '-' ? Note.Accidental.FLAT  : accidental));
						}
    if (accidental == null)
      {
      throw new IllegalArgumentException("Invalid key name");
      }

    // build note from key letter and accidental
    char c = Character.toLowerCase(name.charAt(0));
    switch(c)
      {
      case 'a':
        note = new Note(Note.Name.A, accidental, 0);
        break;
      case 'b':
        note = new Note(Note.Name.B, accidental, 0);
        break;
      case 'c':
        note = new Note(Note.Name.C, accidental, 0);
        break;
      case 'd':
        note = new Note(Note.Name.D, accidental, 0);
        break;
      case 'e':
        note = new Note(Note.Name.E, accidental, 0);
        break;
      case 'f':
        note = new Note(Note.Name.F, accidental, 0);
        break;
      case 'g':
        note = new Note(Note.Name.G, accidental, 0);
        break;
      default:
        throw new IllegalArgumentException("Invalid note name");
      }

    // return note
    return note;
    }

  /**
   *  Sends a MIDI message to a Receiver.
   *  @param   receiver
   *           the receiver
   *  @param   message
   *           the message
   *  @param   timestamp
   *           timestampt associated with the message
   */
  private void sendMessage(Receiver receiver, 
		           MidiMessage message, 
			   long timestamp) 
    {
    // send the message to the receiver
    receiver.send(message, timestamp);
    }

  /**
   *  Sleeps for a given interval.
   *  @param   seconds   the given interval
   */
  private void sleep(double seconds) 
    {
    long interval = (long)(seconds * 1000 + 0.5);
    long t0 = System.currentTimeMillis();
    long t = t0;
    while (t - t0 < interval) 
      {
      try 
        {
        Thread.sleep(interval - (t - t0));
        } 
      catch(InterruptedException e) {  }
      t = System.currentTimeMillis();
      }
    }

  /**
   *  Waits until quit indicator is received from computer keyboard.
   */
  private void waitUntilDone()
    {
    boolean done = false;
    InputStream in = System.in;
    while (!done) 
      {
      try
        {
        char c = (char)in.read();
        done = (Character.toUpperCase(c) == QUIT);
        }
      catch(IOException e) {  }
      }
    }

  /**
   *   Static inner class <tt>Clock</tt> returns the time
   *   relative to the start of the run.
   */
  static private class Clock
    {
    // starting time
    private long nano0 = System.nanoTime();

    /**
     *   Returns current relative time.
     *   @result  nanoseconds since start of run
     *            (within resolution of system clock)
     */
    long now()
      {
      return (System.nanoTime() - nano0);
      }
    }
  /** end of static inner class Clock */

  }
/** end of class Playback */
