
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
import java.util.LinkedList;
import java.util.List;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

/**
 *  Class <code>TuningReceiver</code> adjusts the intonation
 *  of received notes according to a given <code>Intonation</code>
 *  and sends the adjusts notes to a given <code>Receiver</code>;
 *  it gets instructions about modulations through its
 *  <code>modulate</code> method.
 */
public class TuningReceiver implements Receiver 
  {
  // MIDI channel numbers are 0..15 
  private final int MIDI_CHANNEL_LIMIT = 16;

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

  // number of channels in the synthesizer
  private int channelLimit;

  // the MIDI output device
  private Receiver output;

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
   *  @param   channelLimit
   *           number of channels supported by output device
   *  @param   receiver
   *           the MIDI output device
   */
  TuningReceiver(Intonation intonation, int channelLimit, Receiver output)
    {
    this.intonation = intonation;
    this.channelLimit = channelLimit;
    this.output = output;

    // initially all channels are free and no notes are sounding
    for (int i = 0; i < channelLimit; i++) 
      { 
      // the percussion channel won't hold a timbre dependably
      if (i != Constants.Midi.PERCUSSION_CHANNEL) 
        {
        this.freeChannels.add(new Integer(i));
        }
      } 
    for (int i = 0; i < MIDI_NOTE_LIMIT; i++) 
      { 
      this.soundingChannels[i] = Constants.NONE;
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
        this.output.send(msg, timestamp);
        }
      }

    // if it's not a MIDI short message..
    else 
      {
      // just forward the message 
      this.output.send(message, timestamp);
      }
    }	

  /**
   *  Turns off any sounding notes.
   */
  public void quiet() 
    {
    // for each MIDI note, if note is sounding, send a Note Off
    // for that note
    for (int note = 0; note < this.soundingChannels.length; note++) 
      {
      // if note is sounding..
      if (this.soundingChannels[note] != Constants.NONE) 
        {
        sendNoteOff(note, -1L);
        }
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
    //System.out.println("Using channel " + channel + " for note #" +
    //                   noteNumber);

    Note note = null;
    double frequency = 0;
    double midiFrequency = 0;
    try
      {
      // form a note using the MIDI note number
      note = this.intonation.midiNoteNumberToNote(noteNumber);
      //System.out.println("From midiNoteNumberToNote: " + note);

      // get the true frequency of the note and its MIDI frequency
      frequency = this.intonation.frequency(note);
      midiFrequency = this.equalTemperament.frequency(note);
      //System.out.println("Tuned frequency = " + frequency);
      //System.out.println("Midi frequency = " + midiFrequency);
      }
    catch(UnsupportedNoteException e)
      {
      throw new IllegalStateException("Must be a programming error...");
      }

    // compute the pitch bend data 
    int[] pitchBend = computePitchBend(frequency, midiFrequency);
    //System.out.println("pitch bend = " + pitchBend[0] + " " + pitchBend[1]);

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

    // send the pitch bend and the note on messages 
    this.output.send(bend, timestamp);
    this.output.send(noteOn, timestamp);

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
    //System.out.println("NOTE OFF received");
    // recover channel the note is sounding on
    // and mark the note as no longer sounding
    int channel = this.soundingChannels[noteNumber];
    this.soundingChannels[noteNumber] = Constants.NONE;

    // since can reposition in the file and start playing from an arbitrary
    // position, it is possible to encounter a NoteOff for which there
    // no corresonding NoteOn (also, now that there is an 'insert' command,
    // could insert NoteOff for which there is not corres. NoteOn)
    if (channel == Constants.NONE) 
      {
      //System.err.println("Warning: attempt to free channel " +
      // Constants.NONE + " for note #" + noteNumber + " at time " + timestamp);
      return;
      }

    // add the channel to the free list
    this.freeChannels.add(new Integer(channel));
    //System.out.println("Channel " + channel + " freed");

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
    this.output.send(noteOff, timestamp);
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
    final double sensitivity = Constants.Midi.PITCH_BEND_RANGE;

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
