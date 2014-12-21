
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

/**
 *  Class <tt>Replay</tt> reads a file saved by the <tt>Replay</tt>
 *  program's save option and sends the MIDI messages in it to a MIDI 
 *  synthesizer device.  That it, it replays a performance given using
 *  <tt>Replay</tt>.
 *
 *  @version 0.9  5 November 2005
 *                4 March 2006  give it a quit command
 *
 */
public class Replay implements Runnable
  {
  /** a useful constant */
  private static final int NONE = -1;

  /** default values for command-line options (MIDI device indices) */
  private static final int DEFAULT_SYNTH_INDEX = NONE;

  /** default number of channels */
  private static final int DEFAULT_NR_OF_CHANNELS = 16;

  /** the percussion channel (not much use to us) */
  private static final int PERCUSSION_CHANNEL = 9;

  /** default timbre */
  private static final int DEFAULT_TIMBRE = 0;

  /** means end of file when read from DataInputStream */
  private static final int EOF = -1;

  /** ends program if input from keyboard (not case sensitive) */
  private static final char QUIT = 'Q';

  /**
   *  Runs the <code>Replay</code> program.
   *  @param   args   command-line arguments: 
   *                  <tt>filename [-t timbre] [ -synth synthIndex ]</tt>
   */
  static public void main(String[] args)
    {
    // arguments to Replay constructor
    int synthIndex = DEFAULT_SYNTH_INDEX;
    int timbre = DEFAULT_TIMBRE;
    File savedFile = null;

    try 
      {
      // get filename
      int index = 0;
      if (index < args.length)
        {
        savedFile = new File(args[index++]); 
        }
      else
        {
        throw new IllegalArgumentException("Missing filename");
        }

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
	else
          {
          System.err.println("Skipping unrecognized option: " + option);
          }
        }//while

      }//try
    catch (Exception e) 
      {
      System.err.println(e);
      System.err.println(
        "Usage: java intune.Replay " +
        " [ -synth synthIndex ] [ -t timbre ] filename ]\n" + 
	"  where options may occur in any order");
      System.exit(-1);
      }

    // create the Replay object
    new Replay(synthIndex, timbre, savedFile);
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
  private int synthIndex;
  private int timbre;
  private File savedFile;

  // file containing saved performance
  private DataInputStream saved;

  // the MIDI output receiver
  private Receiver synthReceiver;

  // clock giving time relative to start of run
  private Clock clock = new Clock();

  // true if quit command ('q' or 'Q') entered from keyboard
  private volatile boolean quit;

  /**
   *  Constructor.
   *  @param   synthIndex
   *           MIDI device index of the synthesizer (may be NONE)
   *  @param   timbre
   *           the timbre number (NONE if none)
   *  @param   savedFile
   *           file to replay
   */
  Replay(int synthIndex, int timbre, File savedFile)
    {
    this.synthIndex = synthIndex;
    this.timbre = timbre;
    this.savedFile = savedFile;
    new Thread(this).start();
    }

  /**
   *  Runs the program.
   */
  public void run() {

    // MIDI output device
    MidiDevice synth = null;

    // start thread that waits for quit command
    acceptQuitCommand();

    // patch everything together
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
      this.synthReceiver = synth.getReceiver();

      // open the file
      this.saved = new DataInputStream(
                     new BufferedInputStream(
                       new FileInputStream(this.savedFile)));

      // learn how many channels the synthesizer supports 
      // and set the timbre for each of them
      int nrOfChannels = findNumberOfChannels(synth);
      System.out.println("Synthesizer supports " + nrOfChannels + " channels");
      setTimbre(synthReceiver, nrOfChannels, this.timbre);

      // replay the file
      replayFile();
      }
    catch(Exception e) 
      {
      e.printStackTrace();
      System.exit(-1); 
      }

    // close the synthesizer
    synth.close();

    // close the file
    try
      {
      this.saved.close();
      }
    catch(IOException e)
      {
      System.err.println("Error: cannot close saved file: " + e);
      }

    // announce program ended
    System.out.println("Program ended");
    }

  /**
   *  Starts a thread that waits for a quit command from the keyboard.  
   */
  private void acceptQuitCommand() {
    Thread t = new Thread() 
      {
      public void run() {
        boolean done = false;
        InputStream in = System.in;
        while (!done) 
          {
          try
            {
            char c = (char)in.read();
            done = (Character.toUpperCase(c) == QUIT);
            }
          catch(IOException e) {}
          }
        Replay.this.quit = true;
        }
      };
    t.setDaemon(true);
    t.start();
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
   *  Sets timbre on all of the synthesizer's channels.
   *  @param   synthReceiver
   *           the synthesizer's receiver
   *  @param   channelCount
   *           number of channels in the synthesizer
   *  @param   timbre
   *           the timbre (MIDI program number)
   */
  private void setTimbre(Receiver synthReceiver, int channelCount, int timbre)
    {
    for (int channel = 0; channel < channelCount; channel++)
      {
      // send a program change message for the channel and
      // save it if we are saving to disk
      ShortMessage message = new ShortMessage();
      try
        {
        message.setMessage(ShortMessage.PROGRAM_CHANGE,
			   channel,
                           timbre,
                           0);
        }
      catch(InvalidMidiDataException e)
        {
        System.err.println("Invalid timbre " + timbre);
        System.exit(-1);
        }
      this.synthReceiver.send(message, 0);
      }
    }

  /**
   *  Performs the replay.
   */
  private void replayFile()
    {
    System.out.println("In replayFile");
    boolean eof = false;
    while (!eof && !this.quit)
      {
      try
        {
        ShortMessage message = readMessage();
        long timestamp = readTimestamp();
        waitUntil(timestamp);
        System.out.println("Sending message: " + message.getCommand() + " " +
          message.getChannel() + " " + message.getData1() + " " +
	  message.getData2());
        this.synthReceiver.send(message, -1);
        }
      catch(EOFException e)
        {
        eof = true;
        }
      }
    }

  /**
   *  Reads a MIDI message from the input file.
   *  @result  MIDI message
   */
  private ShortMessage readMessage() throws EOFException
    {
    ShortMessage message = null;
    int command = -1;
    int channel = -1;
    int data1 = -1;
    int data2 = -1;
    try
      {
      command = this.saved.readByte() & 0x0ff;     
      channel = this.saved.readByte() & 0x0ff;
      data1 = this.saved.readByte() & 0x0ff;
      data2 = this.saved.readByte() & 0x0ff;
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
   *  Reads timestamp from the input file and returns it.
   *  @result  timestamp (nanoseconds from start)
   */
  private long readTimestamp() throws EOFException
    { 
    long timestamp = -1;
    try
      { 
      timestamp = this.saved.readLong();
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
/** end of class Replay */
