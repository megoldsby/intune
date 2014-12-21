
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
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
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
import java.util.Arrays;

/**
 *  Class <code>PlayTuned</code> accepts input from a MIDI keyboard
 *  and an auxiliary device and produces MIDI output with a specified
 *  tuning, as modified by directives from the auxiliary device.
 *  <p>
 *  Usage: <code>java intune.PlayTuned tuning [ key ] 
 *     [ -kb kbIndex ] [ -aux auxIndex ] [ -t timbre ]</code>
 *
 *  where <code>tuning</code> is the name type of intonation,
 *  <code>key</code> is ( 'A' | 'B' | 'C' | 'D' | 'E' | 'F' | 'G' | 
 *  'a' | 'b' | 'c' | 'd' | 'e' | 'f' | 'g' ) [ ( '#' | '-' ) ],
 *  where the capital letters stand for major keys, the lower case 
 *  letters stand for minor keys and '#' and '-' stand for sharp
 *  and flat, respectively; <code>kbINdex</code> is the MIDI device index of
 *  the MIDI keyboard, <code>auxIndex</code> is the MIDI device index of the
 *  auxiliary device used to input modulation instructions, and
 *  <code>timbre</code> is a MIDI program number (0-127).
 *  <p>
 *  The recognized tunings are currently 'diatonic', 
 *  'pythagorean' and 'equaltemperament'.  The argument must be a
 *  head of one of these strings and long enough to tell them apart.
 *  (I know, a single letter will suffice, but there may be other
 *  tunings later on.)  Note that not all tunings require that the key 
 *  be given (for instance, diatonic does but equaltemperament doesn't).
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
 *  @version 0.9  7 October 2005
 *
 *  @version  19 October 2005
 *  Added synth device number to command line (default none)
 *
 *  @version   5 November 2005
 *  Added -save option.
 *
 *  @version   13 November 2005
 *  Added bank select. 
 *  Set pitch bend range at initialization.
 *
 *  @version   19 November 2005
 *  Added specification of intonation by class name (for temperaments)
 */
public class PlayTuned
  {
  /** names of the recognized tunings */
  private static final String DIATONIC = "diatonic";
  private static final String PYTHAGOREAN = "pythagorean";
  private static final String EQUAL_TEMPERAMENT = "equaltemperament";

  /** a useful constant */
  private static final int NONE = -1;

  /** default values for command-line options (MIDI device indices) */
  private static final int DEFAULT_KB_INDEX = 0;
  private static final int DEFAULT_AUX_INDEX = 1;
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
   *  Runs the <code>PlayTuned</code> program.
   *  @param   args   command-line arguments: 
   *                  tuning [ key ] [-kb kbIndex] [-aux auxIndex]
   *                  [-t timbre] [ -synth synthIndex ] [ -save fielname ]
   */
  static public void main(String[] args)
    {
    // arguments to PlayTuned constructor
    String intonationName = null; 
    String key= null;
    int kbIndex = DEFAULT_KB_INDEX;
    int auxIndex = DEFAULT_AUX_INDEX;
    int synthIndex = DEFAULT_SYNTH_INDEX;
    int timbre = DEFAULT_TIMBRE;
    File saveFile = null;

    try 
      {
      // get intonation name 
      int index = 0;
      intonationName = args[index++];

      // get key if any
      if (index < args.length && !isSwitch(args[index])) 
        {
        key = args[index++];
        }

      // get options 
      while (index < args.length && isSwitch(args[index])) 
        {
        String option = args[index++];

        // MIDI keyboard device index
        if (option.equals("-kb")) 
          {
          kbIndex = Integer.parseInt(args[index++]);
          } 
        // auxiliary MIDI device index
        else if (option.equals("-aux")) 
          {
          auxIndex = Integer.parseInt(args[index++]);
          }
        // synthesizer
        else if (option.equals("-synth")) 
          {
          synthIndex = Integer.parseInt(args[index++]);
          }	  
        // timbre
        else if (option.equals("-t")) 
          {
          timbre = Integer.parseInt(args[index++]);
          }	  
	// name of file to same command to
        else if (option.equals("-save")) 
          {
          saveFile = new File(args[index++]);
          }	  
        }//while
      }//try
    catch (Exception e) 
      {
      System.err.println(e);
      System.err.println(
        "Usage: java intune.PlayTuned intonation [ key ] [ -kb kbIndex ]" +
        " [ -aux auxIndex ] [ -synth synthIndex ] [ -t timbre ]\n" +
	" [ -save filename ]\n" +
        "  where options may occur in any order");
      System.exit(-1);
      }

    // create the PlayTuned object
    new PlayTuned(intonationName, 
                  key, 
                  kbIndex, 
                  auxIndex, 
                  synthIndex, 
                  timbre,
                  saveFile);
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

  // file to save sent command in
  DataOutputStream save;

  // clock giving time relative to start of run
  Clock clock = new Clock();

  /**
   *  Constructor.
   *  @param   intonationName
   *           name of the tuning to be used           
   *  @param   keyName
   *           the key (null if intonation requires no key)
   *  @param   kbIndex
   *           MIDI device index of the keyboard
   *  @param   auxIndex
   *           MIDI device index of the auxiliary device 
   *  @param   synthIndex
   *           MIDI device index of the synthesizer (may be NONE)
   *  @param   timbre
   *           the timbre number (NONE if none)
   *  @param   saveFile
   *           file to save sent command in
   */
  PlayTuned(String intonationName, 
            String keyName, 
            int kbIndex, 
            int auxIndex, 
	    int synthIndex,
            int timbre,
            File saveFile)
    {
    // create an intonation object of the specified type
    Intonation intonation = null;
    String name = intonationName.toLowerCase();
    if (DIATONIC.startsWith(name)) 
      {
      Note key = keyNameToNote(keyName);
      System.out.println("Note " + key + " for keyName " + keyName);
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
      if (keyName != null)
        {
        Note key = keyNameToNote(keyName);
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
      String className = intonationName;
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

    // patch everything together
    MidiDevice keyboard = null;
    MidiDevice auxiliary = null;
    MidiDevice synth = null;
    try 
      {
      // get info about the MIDI devices
      MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();

      // open the MIDI keyboard device
      keyboard = MidiSystem.getMidiDevice(info[kbIndex]);
      keyboard.open();

      // open the auxiliary MIDI device 
      auxiliary = MidiSystem.getMidiDevice(info[auxIndex]);
      auxiliary.open();

      // open the synthesizer and extract its receiver
      if (synthIndex == NONE) 
        { 
        synth = MidiSystem.getSynthesizer();
        }
      else
        {
//      synth = (Synthesizer)MidiSystem.getMidiDevice(info[synthIndex]);
        synth = MidiSystem.getMidiDevice(info[synthIndex]);
        }
      synth.open();
      Receiver synthReceiver = synth.getReceiver();

      // if we are saving the output, open a file to save it in
      if (saveFile != null) {
        this.save = new DataOutputStream(
                      new BufferedOutputStream(
                        new FileOutputStream(saveFile)));
      }

      // learn how many channels the synthesizer supports 
      // and set the pitch bend range and timbre for all of them
      int nrOfChannels = findNumberOfChannels(synth);
      System.out.println("Synthesizer supports " + nrOfChannels + " channels");
      setPitchBendRange(synthReceiver, nrOfChannels, PITCH_BEND_RANGE);
      setTimbre(synthReceiver, nrOfChannels, timbre);

      // make a special receiver that adjusts the pitch of  
      // notes input from the keyboard and sends the adjusted
      // notes to the synthesizer
      TuningReceiver tuningReceiver = new TuningReceiver(
                       intonation, synthReceiver, nrOfChannels);

      // connect the keyboard's transmitter to the tuning receiver
      Transmitter keyTransmitter = keyboard.getTransmitter();
      keyTransmitter.setReceiver(tuningReceiver);	

      // make another special receveiver that receives modulation
      // instructions from the auxliary MIDI device and passes
      // them to the intonation object
      ModulationReceiver modulationReceiver = new ModulationReceiver(
                                                                intonation);

      // connect the auxiliary device's transmitter to the 
      // modulation receiver
      Transmitter auxTransmitter = auxiliary.getTransmitter();
      auxTransmitter.setReceiver(modulationReceiver); 
      }
    catch(Exception e) 
      {
      e.printStackTrace();
      System.exit(-1); 
      }

    // wait until receive indication to quit 
    waitUntilDone();

    // close the keyboard, the auxiliary device and the synthesizer
    synth.close();
    auxiliary.close();
    keyboard.close();

    // if the -save option is on, close the save file
    if (this.save != null)
      {
      try
        {
        this.save.flush();
        this.save.close();
        }
      catch(IOException e)
        {
        System.err.println("Error: cannot close save file: " + e);
        }
      }

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

    // if -save option is on, save the message to disk
    if (this.save != null)
      {
      try
        {
        // write command, channel, data length, data bytes
	// and timestamp
        long nanotime = this.clock.now();
        ShortMessage msg = (ShortMessage)message;
	this.save.writeByte(msg.getCommand());
	this.save.writeByte(msg.getChannel());
        this.save.writeByte(msg.getData1());
        this.save.writeByte(msg.getData2());
        this.save.writeLong(nanotime); 
        }
      catch(IOException e) 
        {
        System.err.println("Cannot write message to file");
	e.printStackTrace();
        System.exit(-1);
        }
      }
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
   *  Inner class <code>TuningReceiver</code> adjusts the intonation
   *  of received notes according to a given <code>Intonation</code>
   *  and sends the adjusts notes to a given <code>Receiver</code>;
   *  it gets instructions about modulations through its
   *  <code>modulate</code> method.
   */
  private class TuningReceiver implements Receiver 
    {
    // MIDI channel numbers are 0..15 
    private final int MIDI_CHANNEL_LIMIT = 16;
				//
    // MIDI note numbers are 0..127
    private final int MIDI_NOTE_LIMIT = 128;

    // MIDI note numbers for note used as reference point
    private final int MIDI_MIDDLE_C = 60;

    // multiplier that raises tone and equal-temperament semitone 
    private final double MIDI_SEMITONE = Math.pow(2., 1./12.) - 1;

    // equal temperament intonation (for MIDI notes)
    private Intonation equalTemperament = new EqualTemperament();	

    // the type of tuning to use for the output
    private Intonation intonation;

    // where to send the output (synthesizer's receiver)
    private Receiver output;

    // number of channels in the synthesizer
    private int channelLimit;

    // channels in use (channel number versus MIDI note number)
    private int[] soundingChannels = new int[MIDI_NOTE_LIMIT];
	
    // list of free channels (set all free in constructor)
    private List freeChannels = new LinkedList();

    // TIMING 
    private long receiveTime; 
    private long totalProcessingTime;
    private int numberOfMessages;

    /**
     *  Constructor.
     *  @param   intonation
     *           how to tune the output
     *  @param   output
     *           where to send the output
     *  @param   channelLimit
     *           number of channels supported by synthesizer
     */
    TuningReceiver(Intonation intonation, Receiver output, int channelLimit)
      {
      this.intonation = intonation;
      this.output = output;
      this.channelLimit = channelLimit;

      // initially all channels are free and no notes are sounding
      for (int i = 0; i < channelLimit; i++) 
        { 
        // the percussion channel won't hold a timbre dependably
        if (i != PERCUSSION_CHANNEL) 
          {
          this.freeChannels.add(new Integer(i));
          }
        } 
      for (int i = 0; i < MIDI_NOTE_LIMIT; i++) 
        { 
        this.soundingChannels[i] = NONE;
        } 
      }

    /**
     *  Does nothing (required by <code>Receiver</code> interface).
     */
    public void close()
      {
      }

    /**
     *  Sends all MIDI messages to output, first adjusting pitch
     *  of NoteOn messages according to this <code>Receiver's</code>
     *  intonation type.
     *  @param   message
     *           message from keyboard
     *  @param   timestamp
     *           timestamp in microseconds
     */
    public void send(MidiMessage message, long timestamp)
      {
      // TIMING
      this.receiveTime = System.nanoTime();

      // if message is a MIDI short message..
      if (message instanceof ShortMessage)
        {
        // extract command from short message
        ShortMessage msg = (ShortMessage)message;
        int command = msg.getCommand();

        // if command in short message is "note on"..
        if (command == ShortMessage.NOTE_ON)
          {
          // extract MIDI note number and key velocity from message
          int noteNumber = msg.getData1();
          int velocity = msg.getData2();

	  // if velocity is positive, send NOTE ON
	  if (velocity > 0)
            {
            sendNoteOn(noteNumber, velocity, timestamp);
            }
	  // if velocity is zero, send NOTE OFF
          else
            {
            sendNoteOff(noteNumber, timestamp);
            }
          }

        // if command in short message is "note off"..
        else if (command == ShortMessage.NOTE_OFF)
          {
          // extract MIDI note number and send NOTE OFF
          int noteNumber = msg.getData1();
          sendNoteOff(noteNumber, timestamp);
          }

        // if command in short message is neither "note on" nor "note off"..
        else
          {
          // just forward the message 
	  sendMessage(this.output, msg, timestamp);
          }
        }
      // if it's not a MIDI short message..
      else 
        {
        // just forward the message 
	sendMessage(this.output, message, timestamp);
        }
      }	

    /**
     *  Sends MIDI NOTE ON message to output, first adjusting pitch
     *  according to this <code>Receiver's</code> intonation type.
     *  @param   noteNumber
     *           MIDI note number
     *  @param   velocity
     *           key velocity (controls dynamics)
     *  @param   timestamp
     *           timestamp in microseconds
     */
    private void sendNoteOn(int noteNumber, int velocity, long timestamp)
      {
      // allocate a channel and mark the note as sounding on that channel
      int channel = ((Integer)this.freeChannels.remove(0)).intValue();
      this.soundingChannels[noteNumber] = channel; 
      System.out.println("Using channel " + channel + " for note #" +
                         noteNumber);

      Note note = null;
      double frequency = 0;
      double midiFrequency = 0;
      try
        {
        // form a note using the MIDI note number
        note = this.intonation.midiNoteNumberToNote(noteNumber);
	System.out.println("From midiNoteNumberToNote: " + note);

        // get the true frequency of the note and its MIDI frequency
        frequency = this.intonation.frequency(note);
        midiFrequency = this.equalTemperament.frequency(note);
	System.out.println("Tuned frequency = " + frequency);
	System.out.println("Midi frequency = " + midiFrequency);
        }
      catch(UnsupportedNoteException e)
        {
        throw new IllegalStateException("Must be a programming error...");
        }

      // compute the pitch bend data 
      int[] pitchBend = computePitchBend(frequency, midiFrequency);
      System.out.println("pitch bend = " + pitchBend[0] + " " + pitchBend[1]);

      // set up a pitch bend message and a
      // "note on" message on the channel
      ShortMessage bend = new ShortMessage();
      ShortMessage noteOn = new ShortMessage();
      try
        {
        bend.setMessage(ShortMessage.PITCH_BEND,
                        channel,
                        pitchBend[0], pitchBend[1]);

        noteOn.setMessage(ShortMessage.NOTE_ON,
                          channel,
                          noteNumber,
                          velocity);
        }
      catch(InvalidMidiDataException e)
        {
        throw new IllegalStateException("Must be a programming error...");
        }

      //
 /************ 
 Really, need to know what channel the changed note is sounding on
 and in what way to change it.  Then I can just apply a pitch bend
 to the channel.  (Otherwise, I would have to send a NoteOff followed
 by a NoteOn with the new pitch bend.)  Getting the frequency of
 the note will allow me to compute the correct pitch bend.
     A simple way to do it would be to get the frequency of every
sounding note and compare it to the sounding frequency; if it's different,
send a new pitch bend for it.
      if (this.intonation instanceof AdjustingIntonation)
        {
        Note[] changed = this.intonation.addNote(note);
	for (int i = 0; i < changed.length; i++) 
          {
          adjustNote(changed[i]);
          }
        }
 *************/

      // send the pitch bend and the note on messages 
      sendMessage(this.output, bend, timestamp);
      sendMessage(this.output, noteOn, timestamp);

      // TIMING
      long processingTime = System.nanoTime() - this.receiveTime;
      this.totalProcessingTime += processingTime;
      this.numberOfMessages++;
      if ((this.numberOfMessages & 0x7f) == 0) System.out.println("avg time " +
        (((float)this.totalProcessingTime)/this.numberOfMessages));
      }

    /**
     *  Sends MIDI NOTE OFF message to output.
     *  @param   noteNumber
     *           MIDI note number
     *  @param   timestamp
     *           timestamp in microseconds
     */
    private void sendNoteOff(int noteNumber, long timestamp)
      {
      System.out.println("NOTE OFF received");
      // recover channel the note is sounding on
      // and mark the note as no longer sounding
      int channel = this.soundingChannels[noteNumber];
      this.soundingChannels[noteNumber] = NONE;

      // add the channel to the free list
      this.freeChannels.add(new Integer(channel));
      System.out.println("Added channel " + channel + " freed");

      // set up a NOTE OFF message on the channel
      ShortMessage noteOff = new ShortMessage();
      try
        {
        noteOff.setMessage(ShortMessage.NOTE_OFF,
                           channel,
                           noteNumber,
                           0);			 
        }
      catch(InvalidMidiDataException e)
        {
        throw new IllegalStateException("Must be a programming error...");
        }

      // send NOTE OFF to the synthesizer 
      sendMessage(this.output, noteOff, timestamp);
      }

    /**
     *  Returns the two data bytes for a MIDI pitch bend message,
     *  given the frequency of the MIDI note to be bent and the frequency
     *  it is to be bent to.
     *  @param   midiFrequency   note to bend
     *  @param   frequency       resulting bent note
     *  @result  high- and low-order pitch bend message bytes
     */
    private int[] computePitchBend(double midiFrequency, double frequency)
      {
      final double sensitivity = PITCH_BEND_RANGE;

      // range of pitch bend data is [0, PITCH_BEND_EXTENT-1],
      // zero bend corresponds to PITCH_BEND_EXTENT / 2
      final double PITCH_BEND_EXTENT = 0x4000;
  
      // compute fraction of a semitone to bend
      double semitones = 0.0;
      if (frequency > midiFrequency) 
        {
        semitones = (frequency - midiFrequency) / 
                      (MIDI_SEMITONE * midiFrequency);
        }
      else if (frequency < midiFrequency)
        {
        semitones = (frequency - midiFrequency) /
                      (MIDI_SEMITONE * frequency);
        }
      double fractionOfRange = semitones / sensitivity;
    
      //System.out.println("***** pitch bend semitones " + semitones);
  
  
      // derive the pitch bend data value from that
      int dataValue = (int)((PITCH_BEND_EXTENT/2) + 
                              fractionOfRange * (PITCH_BEND_EXTENT/2));
      //System.out.println("***** pitch bend dataValue " + 
      //                 Integer.toHexString(dataValue));
  
      // extract the data value's high and low order bytes
      int lo = dataValue & 0x7f;
      int hi = (dataValue >> 7) & 0x7f;
  
      // return the low and high order bytes
      return new int[] { lo, hi };
      }
    }
  /** end of inner class TuningReceiver */

  /**
   *  Inner class <code>ModulationReceiver</code>
   *  receives note on and note off messages from the auxiliary
   *  MIDI device, which it interprets as modulation instructions and
   *  sends the modulations to the tuning receiver.
   */
  class ModulationReceiver implements Receiver 
    {
    // MIDI notes per octave
    private final int OCTAVE = 12;

    // MIDI note number for the subdominant selector
    private final int SUBDOMINANT = 43;

    // MIDI note numbers for the modulation target keys 
    // (the first corresponds to C, the next to C# or Dflat, etc.)
    private final int[] MODULATION_TARGETS = {
      48, 52, 55, 59, 62, 65, 69, 72, 76, 79, 83, 86 };

    // the intonation in use
    private Modulator intonation;

    /**
     *  Constructor.
     *  @param   intonation
     *           the intonation
     */
    ModulationReceiver(Intonation intonation)
      {
      // save intonation, provided it is a Modulator
      if (intonation instanceof Modulator)
        {
        this.intonation = (Modulator)intonation;
        }
      else
        {
        this.intonation = null;
        }
      }

    /**
     *  Does nothing (required by <code>Receiver</code> interface).
     *  @see Receiver
     */
    public void close()
      {
      }

    /**
     *  Interprets message as a modulation instruction
     *  and forwards the modulation to the tuning receiver.
     *  @param   message
     *           message from keyboard
     *  @param   timestamp
     *           timestamp in microseconds
     *  @see Receiver
     */
    public void send(MidiMessage message, long timestamp)
      {
      if (this.intonation != null)
        {
        // if message is a MIDI short message..
        if (message instanceof ShortMessage)
          {
          // extract command from short message
          ShortMessage msg = (ShortMessage)message;
          int command = msg.getCommand();
  
          // if command in short message is "note on"..
          if (command == ShortMessage.NOTE_ON)
            {
            // extract MIDI note number and key velocity from message
            int noteNumber = msg.getData1();
            int velocity = msg.getData2();
  
    	    // if velocity is positive, treat it as NOTE ON
    	    if (velocity != 0)
              {
              auxNoteOn(noteNumber);
              }
  	    // if velocity is zero, treat it as NOTE OFF
            else
              {
              auxNoteOff(noteNumber);
              }
            }
  
          // if command in short message is "note off", treat it as such
          else if (command == ShortMessage.NOTE_OFF)
            {
            int noteNumber = msg.getData1();
            auxNoteOff(noteNumber);
            }
  
          // if command in short message is neither "note on" nor "note off"..
          else
            {
            System.out.println("From auxiliary device: Short Message " +
              command + " " + msg.getData1() + " " + msg.getData2());
            }
          }
        // if it's not a MIDI short message..
        else 
          {
          System.out.println("From auxiliary device: messsage of class " +
            message.getClass().getName());
          }
        }//if Modulator
      }

    /**
     *  Interpret NOTE ON from auxiliary device.
     *  @param   noteNumber
     *           the MIDI note number
     */
    private void auxNoteOn(int noteNumber)
      {
      // name of some note, any note
      final Note.Name NOTE_NAME = Note.Name.A;

      // if note is the subdominant selector, tell tuner it's on
      if (noteNumber == SUBDOMINANT)
        {
        this.intonation.setSubdominant(true);
        }

      // if note is a modulation key selector..
      else
        {
        // find the key to modulate to and tell the
        // intonation object to modulate to it
        int keyIndex = Arrays.binarySearch(MODULATION_TARGETS, noteNumber);
        if (keyIndex >= 0)
          {
          Note target = null;
          try
            {
            target = this.intonation.midiNoteNumberToNote(keyIndex);
            System.out.println("keyIndex " + keyIndex + " target " + target);
            }
          catch(UnsupportedNoteException e)
            {
            e.printStackTrace();
            System.exit(-1); 
            }
          this.intonation.modulate(target);
          }
        else 
          {
          System.err.println("Should not happen: ModulationReceiver " +
            "cannot find " + noteNumber + " in modulation targets");
          }
        }
      }

    /**
     *  Interpret NOTE OFF from auxiliary device.
     *  @param   noteNumber
     *           the MIDI note number
     */
    private void auxNoteOff(int noteNumber)
      {
      // if note is the subdominant selector, tell tuner it's off
      if (noteNumber == SUBDOMINANT)
        {
        this.intonation.setSubdominant(false);
        }

      // if note is a modulation key selector..
      else 
        {
        // do nothing
        }
      }
    }
  /** end of inner class ModulationReceiver */

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
/** end of class PlayTuned */
