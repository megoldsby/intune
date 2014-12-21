
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Transmitter;

/**
 *  Class <tt>Midi</tt> encapsulates the use of the 
 *  <tt>javax.sound.midi package</tt>.
 */
public class Midi {

  // the open devices (set of Midi.Device)
  private static Set devices = new HashSet();

  /**
   *  Shuts down the midi system.
   */
  public static void close() {

    // close the devices
    Iterator iterator = Midi.devices.iterator();
    while (iterator.hasNext()) {
      Midi.Device device = (Midi.Device)iterator.next();
      device.close();
    }
  }

  /**
   *  Returns true if command and velocity specify a note on.
   *  @param   message
   *           a MIDI short message
   *  @result  true if message is a Note On with velocity > 0
   */
  public static boolean isNoteOn(ShortMessage message) {
    return (message.getCommand() == ShortMessage.NOTE_ON 
            && message.getData1() > 0);
  }

  /**
   *  Returns true if command and velocity specify a note off.
   *  @param   message
   *           a MIDI short message
   *  @result  true if message is a Note Off or a 
   *           Note On with velocity zero
   */
  public static boolean isNoteOff(ShortMessage message) {
    int command = message.getCommand();
    return (command == ShortMessage.NOTE_OFF
            || (command == ShortMessage.NOTE_ON
                && message.getData1() == 0));
  }

  /**
   *  Static inner class <tt>Device</tt> is the base class
   *  for MIDI input and output device classes.
   */
  public static class Device {  

    // the MIDI device
    MidiDevice device;    

    /**
     *  Constructor.
     *  @param   index
     *           MIDI device index
     */
    public Device(int index) {

      // get the MIDI device with the given index (or if index
      // is NONE, get the software synthesizer)
      try {
        if (index != Constants.NONE) {
          MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();
          this.device = MidiSystem.getMidiDevice(info[index]); 
        } else {
          this.device = MidiSystem.getSynthesizer();
        }

      } catch(MidiUnavailableException e) {
        throw new IllegalArgumentException(
          "No such Midi device: " + index + ": " + e);
      }

      try {
        // open the device
        this.device.open();
      } catch(MidiUnavailableException e) {
        throw new IllegalArgumentException(
          "Cannot open MIDI device " + index + ": " + this.device + ": " + e);
      }

      // put device into the open device set
      Midi.devices.add(this);
    }

    /**
     *  Constructor for use when there is no underlying Midi device.
     */
    public Device() {

    }

    /**
     *  Closes the device.
     */
    public void close() {
      if (this.device != null) {
        System.out.println("CLosing device " + this);
        this.device.close();
        System.out.println("closed");
      }
    }
  }

  /**
   *  Static inner class <tt>OutputDevice</tt> represents
   *  a MIDI output device.
   */
  public static class OutputDevice extends Device {

    // device's receiver
    protected Receiver receiver;

    // number of channels for this device
    private int channelCount;

    /**
     *  Constructor.
     *  @param   index
     *           MIDI device index
     */
    public OutputDevice(int index) {
      super(index);
      try {
        this.receiver = this.device.getReceiver();
      } catch(MidiUnavailableException e) {
        throw new IllegalArgumentException(
          "Midi device " + index + ": " + this.device +
          " is not an output device: " + e);
      }
      findNumberOfChannels();
      System.out.println(
        "Output device " + index + " has " + this.channelCount + " channels");
    }

    /**
     *  Constructor for use when derived class provides the receiver.
     */
    public OutputDevice() {
      findNumberOfChannels();
    }

    /**
     *  Returns the device's receiver.
     *  @result   this output device's receiver
     */
    public Receiver getReceiver() {
      return this.receiver;
    }

    /**
     *  Returns the number of channels for the device.
     *  @result   number of output device's channels
     */
    public int getChannelCount() {
      return this.channelCount;
    }

    /**
     *  Send a MIDI message to the device.
     *  @param   message
     *           the message
     */
    public void sendMessage(ShortMessage message) {

      // send the message
      this.receiver.send((MidiMessage)message, -1L);

      System.out.println("sendMessage message: " + message.getCommand() +
        " " + message.getChannel() + " " + message.getData1() + " " +
	message.getData2());
    }  

    /**
     *  Determines number of channels supported by this Midi output device.
     *  @result  the number of channels
     */
    private void findNumberOfChannels() {
  
      // if device is Synthesizer, ask it how many channels it has
      int number = 0;
      if (this.device instanceof Synthesizer) {
        MidiChannel[] channels = ((Synthesizer)this.device).getChannels();
        while (number < channels.length && channels[number] != null) {
          number++;
        }
  
      // otherwise assume it has the default number of channels
      } else {
        number = Constants.Midi.DEFAULT_NR_OF_CHANNELS;
      }

      // set number of channels
      this.channelCount = number;
    }

    /**
     *  Sets the  pitch bend range for each of the device's channels.
     *  @param   range
     *           total range in semitones
     */
    public void setPitchBendRange(int range) {

      // for each channel..
      for (int channel = 0; channel < this.channelCount; channel++) {
  
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
                                Constants.Midi.RPN_MSB,
                                Constants.Midi.PB_RANGE_MSB);
          pbRangeLSB.setMessage(ShortMessage.CONTROL_CHANGE,
                                channel,
                                Constants.Midi.RPN_LSB,
                                Constants.Midi.PB_RANGE_LSB);
          setPBRangeSemitones.setMessage(ShortMessage.CONTROL_CHANGE,
                                         channel,
                                         Constants.Midi.DATA_ENTRY_MSB,
                                         range / 2);
          setPBRangeCents.setMessage(ShortMessage.CONTROL_CHANGE,
                                     channel,
                                     Constants.Midi.DATA_ENTRY_LSB,
                                     0);
          rpnResetMSB.setMessage(ShortMessage.CONTROL_CHANGE,
                                 channel,
                                 Constants.Midi.RPN_MSB,
                                 Constants.Midi.RPN_RESET_MSB);
          rpnResetLSB.setMessage(ShortMessage.CONTROL_CHANGE,
                                 channel,
                                 Constants.Midi.RPN_LSB,
                                 Constants.Midi.RPN_RESET_LSB);
    
        } catch(InvalidMidiDataException e) {
          throw new IllegalArgumentException("Invalid pitch bend range " + range);
        }
    
        // send the messages
        sendMessage(pbRangeMSB);
        sendMessage(pbRangeLSB);
        sendMessage(setPBRangeSemitones);
        sendMessage(setPBRangeCents);
        sendMessage(rpnResetMSB);
        sendMessage(rpnResetLSB);
      }//for
    }

    /**
     *  Sets the timbre on each of the device's channels.
     *  @param   timbre
     *           the timbre (MIDI bank number * 128 + MIDI program number) 
     */
    public void setTimbre(int timbre) {

      // get bank and program numbers
      int bank = timbre / Constants.Midi.PROGRAMS_PER_BANK;
      int program = timbre % Constants.Midi.PROGRAMS_PER_BANK;
  
      // send a program change message to each channel
      for (int channel = 0; channel < this.channelCount; channel++) {
  
        // if bank is not zero, send a bank select
        if (bank > 0) {
          int bankMSB = bank / 128;          	      
          int bankLSB = bank % 128;          	      
          ShortMessage bankMSBmessage = new ShortMessage();
          ShortMessage bankLSBmessage = new ShortMessage();
          try {
            bankMSBmessage.setMessage(ShortMessage.CONTROL_CHANGE,
                                      channel,
                                      Constants.Midi.BANK_SELECT,
                                      bankMSB);
            bankLSBmessage.setMessage(ShortMessage.CONTROL_CHANGE,
                                      channel,
                                      Constants.Midi.CONTROL_CHANGE_LSB,
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
          throw new IllegalArgumentException("Invalid timbre " + timbre);
        }
        sendMessage(timbreMessage);
  
      }//for
    }
  }/** end of static inner class OutputDevice */

  /**
   *  Static inner class <tt>InputDevice</tt> represents
   *  a MIDI input device.
   */
  public static class InputDevice extends Device {
    private Transmitter transmitter;
    public InputDevice(int index) {
      super(index);
      try {
        this.transmitter = this.device.getTransmitter();
      } catch(MidiUnavailableException e) {
        throw new IllegalArgumentException(
          "Midi device " + index + ": " + this.device +
          " is not an input device: " + e);
      }
    }

    /**
     *  Returns the transmitter.
     *  @result   this input device's transmitter
     */
    public Transmitter getTransmitter() {
      return this.transmitter;
    }
    
  }/** end of static inner class InputDevice */

}/** end of class Midi */
