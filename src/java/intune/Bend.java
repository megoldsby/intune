
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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;

/**
 *  Tests pitch bend.
 *  <p>
 *  java intune.Bend [ -synth outdevice ] [ -t timbre ]
 */
public class Bend implements Runnable {

  private static final int NONE = -1;

  private static final int DEFAULT_MIDI_OUT = 6;
  private static final int DEFAULT_TIMBRE = 60;
  private static final int DEFAULT_NR_OF_CHANNELS = 16;
  private static final int PERCUSSION_CHANNEL = 9;

  private final int MIDI_NOTE_LIMIT = 128;
  private static final int MAX_VELOCITY = 127;
  private static final int PITCH_BEND_LIMIT = 16384;
  private static final int NO_PITCH_BEND = PITCH_BEND_LIMIT / 2;
  private static final int DEFAULT_PITCH_BEND = NO_PITCH_BEND;
  private static final int PITCH_BEND_RANGE = 1;  // 1 semitone up, 1 down

  private static final int RPN_MSB            = 0x65;
  private static final int RPN_LSB            = 0x64;
  private static final int PB_RANGE_MSB       = 0x00;
  private static final int PB_RANGE_LSB       = 0x00;
  private static final int PB_RANGE_SEMITONES = 0x06;
  private static final int PB_RANGE_CENTS     = 0x26;
  private static final int RESET_LSB          = 0x7f;
  private static final int RESET_MSB          = 0x7f;

  private static final int PROGRAMS_PER_BANK  = 128;
  private static final int BANK_SELECT        = 0x00;
  private static final int CONTROL_CHANGE_LSB = 0x20; 

  //////////////////////////////////////////////////////////////////
  // instance variables
  //////////////////////////////////////////////////////////////////

  // midi output device number
  int midiOut;

  // midi program number
  int timbre;

  // free channels, channels in use
  List freeChannels = new ArrayList();
  int[] soundingChannels = new int[MIDI_NOTE_LIMIT];

  // output device and its receiver
  MidiDevice output;
  Receiver receiver;

  /**
   *  Constructor.
   */
  public Bend(int midiOut, int timbre) {
    this.midiOut = midiOut;
    this.timbre = timbre;
    new Thread(this).start();
  }

  /**
   *  Performs the program logic.
   */
  public void run() {

    // set up the MIDI device
    initializeMidi();
    System.out.println("Midi initialized");

    // forever..
    boolean quit = false;
    while (!quit) {
      try {
        BufferedReader reader = new BufferedReader(
                                  new InputStreamReader(System.in));
        // get note number
        System.out.println();
        System.out.print("Enter MIDI note number: ");
        System.out.flush();
        String line = reader.readLine();
        int noteNumber = Integer.parseInt(line.trim());

        // get initial pitch bend value
        System.out.print("Enter initial pitch bend value (0-16383): ");
        System.out.flush();
        line = reader.readLine().trim();
	int bend = DEFAULT_PITCH_BEND;
	if (!line.equals("")) {
          bend = Integer.parseInt(line);
        }

        // sound the note
        SoundingNote soundingNote = soundNote(noteNumber, bend);

        // get pitch bend adjustments
        System.out.print("Enter positive or negative pitch bend " +
          "adjustments, ending with '.': ");
        System.out.flush();
        boolean more = true;
        while (more) {
          line = reader.readLine().trim();
          more = !line.equals(".");
          if (more) {
            int adjustment = Integer.parseInt(line);
	    soundingNote.adjust(adjustment);
          }
        }

	// see if want to quit
	System.out.print("Quit? (y/n): ");
        System.out.flush();
        line = reader.readLine().trim();
	if (line.equals("y")) {
          quit = true;
        }

      } catch(Exception e) {
        e.printStackTrace();
      } 
    }

    // close gracefully
    closeMidi();
  }

  /**
   */
  private void initializeMidi() {

    int nrOfChannels = -1;
    try{ 
      // get info about the MIDI devices
      MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();

      // open the MIDI output device and extract its receiver
      this.output = MidiSystem.getMidiDevice(info[this.midiOut]); 
      output.open();
      this.receiver = this.output.getReceiver();

      // learn how many channels the synthesizer supports 
      // and set the timbre and pitch bend range for each of them
      nrOfChannels = findNumberOfChannels(output);
      System.out.println("MIDI output supports " + nrOfChannels + " channels");
      setPitchBendRange(nrOfChannels);
      setTimbre(nrOfChannels, this.timbre);

    } catch(Exception e) {
      System.err.println("Cannot initialize MIDI output device: " + e);
      e.printStackTrace();
      System.exit(-1);
    }

    // initially all channels are free and no notes are sounding
    for (int i = 0; i < nrOfChannels; i++) { 

      // the percussion channel won't hold a timbre dependably
      if (i != PERCUSSION_CHANNEL) {
        this.freeChannels.add(new Integer(i));
      }
    } 
    for (int i = 0; i < MIDI_NOTE_LIMIT; i++) { 
      this.soundingChannels[i] = NONE;
    } 

  }

  /**
   */
  private void closeMidi() {
    this.output.close();
  }

  /**
   *  Returns number of channels supported by the output device.
   *  @param   output
   *           the output device
   *  @result  the number of channels
   */
  private int findNumberOfChannels(MidiDevice output) {
    int number = DEFAULT_NR_OF_CHANNELS;
    if (output instanceof Synthesizer) {
      MidiChannel[] channels = ((Synthesizer)output).getChannels();
      while (number < channels.length && channels[number] != null) {
        number++;
      }
    }
    return number;
  }

  /**
   *  Sets timbre on all of the output devices's channels.
   *  @param   channelCount
   *           number of channels in the synthesizer
   *  @param   timbre
   *           the timbre (MIDI program number)
   */
  private void setTimbre(int channelCount, int timbre) {

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
        sendMessage(bankMSBmessage);
        sendMessage(bankLSBmessage);
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
      sendMessage(timbreMessage);

    }//for
  }

  /**
   *  Sets pitch bend range on all of the output device's channels.
   *  @param   channelCount
   *           number of channels in the synthesizer
   *  @param   timbre
   *           the timbre (MIDI program number)
   */
  private void setPitchBendRange(int channelCount) {

    // for each channel..
    for (int channel = 0; channel < channelCount; channel++) {

      // set up the sequence of messages
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
                                       PB_RANGE_SEMITONES,
                                       1);
        setPBRangeCents.setMessage(ShortMessage.CONTROL_CHANGE,
                                   channel,
                                   PB_RANGE_CENTS,
                                   0);
        rpnResetMSB.setMessage(ShortMessage.CONTROL_CHANGE,
                               channel,
                               RPN_MSB,
                               RESET_MSB);
        rpnResetLSB.setMessage(ShortMessage.CONTROL_CHANGE,
                               channel,
                               RPN_LSB,
                               RESET_LSB);
  
      } catch(InvalidMidiDataException e) {
        System.err.println("Error setting pitch bend range");
        e.printStackTrace();
        System.exit(-1);
      }
  
      // send the messages
      sendMessage(pbRangeMSB);
      //System.out.println("Send pbRangeMSB");
      //sleep(2);
      sendMessage(pbRangeLSB);
      //System.out.println("Send pbRangeLSB");
      //sleep(2);
      sendMessage(setPBRangeSemitones);
      //System.out.println("Send setPBRangeSemitones");
      //sleep(2);
      sendMessage(setPBRangeCents);
      //System.out.println("Send setPBRangeCents");
      //sleep(2);
      sendMessage(rpnResetMSB);
      //System.out.println("Send rpnResetMSB");
      //sleep(2);
      sendMessage(rpnResetLSB);
      //System.out.println("Send rpnResetLSB");
      //sleep(2);
    }//for
  }

  /**
   *  Starts a note sounding.
   */
  private SoundingNote soundNote(int noteNumber, int bendValue) {

    // allocate a channel and mark the note as sounding on that channel
    int channel = ((Integer)this.freeChannels.remove(0)).intValue();
    this.soundingChannels[noteNumber] = channel; 
    System.out.println("Using channel " + channel + " for note #" +
                       noteNumber);

    // send the pitch bend value
    sendPitchBend(channel, bendValue);

    // send a NoteOn message
    sendNoteOn(channel, noteNumber, MAX_VELOCITY);

    // return info about the sounding note
    return new SoundingNote(noteNumber, channel, bendValue);
  }

  /**
   *   Composes and sends a pitch bend message.
   *   @param   channel
   *            the channel to send it on
   *   @param   bendValue
   *            the amount of the bend (0-16383)
   */
  private void sendPitchBend(int channel, int bendValue) {

    // compute the pitch bend data 
    int lo = bendValue & 0x7f;
    int hi = (bendValue >> 7) & 0x7f;
    int[] pitchBend = new int[] { lo, hi };
    System.out.println("pitch bend = " + pitchBend[0] + " " + pitchBend[1]);

    // form the pitch bend message
    ShortMessage bend = new ShortMessage();
    try {
      bend.setMessage(ShortMessage.PITCH_BEND,
                      channel,
                      pitchBend[0], pitchBend[1]);
    } catch(InvalidMidiDataException e) {
      throw new IllegalStateException("Must be a programming error...");
    }

    // send the pitch bend message
    sendMessage(bend);
  }
    
  /**
   *   Composes and sends a NoteOn message.
   *   @param   channel
   *            the channel to send it on
   *   @param   noteNumber
   *            the MIDI note number of the note to turn on
   *   @param   velocity
   *            the velocity (loudness)
   */
  private void sendNoteOn(int channel, int noteNumber, int velocity) {

    // form the NoteOn message
    ShortMessage noteOn = new ShortMessage();
    try {
      noteOn.setMessage(ShortMessage.NOTE_ON,
                        channel,
                        noteNumber,
                        velocity);
    } catch(InvalidMidiDataException e) {
      throw new IllegalStateException("Must be a programming error...");
    }

    // send the pitch bend message
    sendMessage(noteOn);
  }

  /**
   *  Sends Midi message to the output device.
   *  @param   message
   *           the message
   */
  private void sendMessage(MidiMessage message) {
    this.receiver.send(message, -1);
  }

  /**
   *  Sleep for a while.
   *  @param   seconds
   *           the while
   */
  private void sleep(double seconds) {
    long msec = (long)(seconds * 1000 + 0.5);
    try {
      Thread.sleep(msec);
    } catch(InterruptedException e) {  }
  }

  /**
   *  Inner class <tt>SoundingNote</tt> represents a
   *  sounding note.
   */
  private class SoundingNote {
    private int noteNumber;
    private int channel;
    private int bend;
    SoundingNote(int noteNumber, int channel, int bend) {
      this.noteNumber = noteNumber;
      this.channel = channel;
      this.bend = bend;
    }
    int getNoteNumber() {
      return this.noteNumber;
    }
    int getChannel() {
      return this.channel;
    }
    int getBend() {
      return this.bend;
    }
    void adjust(int adjustment) {

      // develop new pitch bend value and save it
      int newBend = this.bend + adjustment;
      if (newBend < 0 || newBend >= PITCH_BEND_LIMIT) {
        System.err.println("Adjustment would produce invalid " +
			   "pitch bend value: " + newBend);
	return;
      } 
      this.bend = newBend;

      // send the new bend value to the channel
      sendPitchBend(this.channel, this.bend);
    }
  }/** end of inner class SoundingNote */

  public static void main(String[] args) {
    int midiOut = DEFAULT_MIDI_OUT;
    int timbre = DEFAULT_TIMBRE;
    try {
      int index = 0;
      while (index < args.length) {
        if (args[index].equals("-synth")) {
          midiOut = Integer.parseInt(args[++index]); 
        } else if (args[index].equals("-t")) {
          timbre = Integer.parseInt(args[++index]);
        } else {
          System.err.println("Unrecognized argument: " + args[index]);
        }
        index++;
      }
    } catch(Exception e) {
      System.err.println("Usage: java Bend [ -synth midiOut ] [ -t timbre ]");
      System.exit(-1);
    }
    new Bend(midiOut, timbre);
  } 
}/** end of class Bend */
