
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
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiFileFormat;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.Track;
import javax.sound.midi.spi.MidiFileWriter;

/**
 *  Class <tt>IntuneMidiFileWriter</tt> reads a MIDI file.
 */
public class IntuneMidiFileWriter extends MidiFileWriter {
  
  int resolution;

  public IntuneMidiFileWriter(int resolution) {  
    this.resolution = resolution;
  }

  public int[] getMidiFileTypes() { 
    return new int[]{  Constants.Midi.File.SINGLE_TRACK  }; 
  }

  public int[] getMidiFileTypes(Sequence sequence) { 
    return new int[]{  Constants.Midi.File.SINGLE_TRACK  }; 
  }

  public boolean isFileTypeSupported(int fileType) { 
    return (fileType == Constants.Midi.File.SINGLE_TRACK);
  }
  public boolean isFileTypeSupported(int fileType, Sequence sequence) {
    return (fileType == Constants.Midi.File.SINGLE_TRACK);
  }

  public int write(Sequence in, int fileType, File out) throws IOException { 
    FileOutputStream stream = new FileOutputStream(out); 
    return(write(in, fileType, stream));
  }

  public int write(Sequence in, int fileType, OutputStream out) 
      throws IOException { 
    DataOutputStream midi = new DataOutputStream(out);
    for (int i = 0; i < Constants.Midi.File.HEADER_TYPE.length; i++) {
      midi.write((byte)Constants.Midi.File.HEADER_TYPE[i]);
    }
    midi.writeInt(Constants.Midi.File.HEADER_DATA_LENGTH);
    midi.writeShort(Constants.Midi.File.SINGLE_TRACK);
    midi.writeShort(1);  // number of tracks
    midi.writeByte(this.resolution / 256);
    midi.writeByte(this.resolution % 256);
    writeTrack(midi, in.getTracks()[0]); 
    return midi.size();
  }

  private void writeTrack(DataOutputStream midi, Track track) 
      throws IOException {
    // write the track type bytes
    for (int i = 0; i < Constants.Midi.File.TRACK_TYPE.length; i++) {
      byte b = (byte)Constants.Midi.File.TRACK_TYPE[i];
      midi.write(b);
    }

    // read the track's events and accumulate them in a byte array:
    // for each event..
    long ticks0 = 0;
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    int size = track.size();
    for (int i = 0; i < size; i++) {
      MidiEvent event = track.get(i);

      // compute the event's delta time and output it
      long ticks = event.getTick();
      int delta = (int)(ticks - ticks0);
      ticks0 = ticks;
      outputDeltaTime(output, delta);

      // output the event's message 
      MidiMessage message = event.getMessage();
      byte[] msg = message.getMessage();
      int length = message.getLength();
      for (int j = 0; j < length; j++) {
        output.write(msg[j]);
      }

      /****
      int status = message.getStatus() & 0xff;
      if (status == MetaMessage.META) {
        outputMetaMessage(output, (MetaMessage)message);
      } else if (status == SysexMessage.SYSTEM_EXCLUSIVE
                 || status == SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE) {
        outputSysexMessage(output, (SysexMessage)message);
      } else if (status > 0xf0) {
        outputOtherSystemMessage(output, message);
      } else {
        outputShortMessage(output, (ShortMessage)message);
      }
      ****/
    }
// should I append an end of track meta message or should I count
// it already being part of the track?  probably the latter.

    // write the track's data length
    byte[] bytes = output.toByteArray();
    midi.writeInt(bytes.length);
    System.out.println("Wrote track length: " + bytes.length);

    // write the track's data
    midi.write(bytes); 

    // close the output stream
    midi.close();
  }

  /**
   *  Writes a Midi meta message to the output stream.
   *  @param   output
   *  @param   message
   *  @throws  IOException
   */
  private void outputMetaMessage(OutputStream output, MetaMessage message)
      throws IOException {
    output.write(MetaMessage.META);  
    output.write(message.getType());
    byte[] data = message.getData();
    output.write(data.length);
    for (byte b : data) {
      output.write(b);
    }
  }

  /**
   *  Writes a Midi system exclusive message to the output stream.
   *  @param   output
   *  @param   message
   *  @throws  IOException
   */
  private void outputSysexMessage(OutputStream output, SysexMessage message)
      throws IOException {
    output.write(message.getStatus());
    byte[] data = message.getData();
    for (byte b : data) {
      output.write(b);
    }
  }

  /**
   *  Writes a Midi system message to the output stream.
   *  @param   output
   *  @param   message
   *  @throws  IOException
   */
  private void outputOtherSystemMessage(OutputStream output, 
                                        MidiMessage message)
      throws IOException {

    // write out the message (status byte followed by any data)
    byte[] msg = message.getMessage();
    int length = message.getLength();
    for (int i = 0; i < length; i++) {
      output.write(msg[i]);
    }
    Formatter fmt = new Formatter();
    fmt.format("Info: outputting message with status %x", msg[0]);
    System.err.println(fmt);
  }

  private void outputShortMessage(OutputStream output, ShortMessage message)
      throws IOException {

  }

  /**
   *  Write a Midi delta time to the output stream.
   *  @param   midi
   *  @param   delta
   *  @throws  IOException
   */
  private void outputDeltaTime(OutputStream midi, int delta) 
      throws IOException {
    System.out.println("Outputting delta time " + delta);
    /**
    while (delta > 0x7f) {
      int val = 0x80 | (delta & 0x7f);
      midi.write(val); 
      delta >>= 7;
    } 
    midi.write(delta);
    **/
    if (delta == 0) {
      midi.write(delta);
    } else {
      List<Integer> stack = new ArrayList<Integer>();
      while (delta > 0) {
        stack.add(delta & 0x7f);  
        delta >>= 7;
      }
      for (int i = stack.size()-1; i > 0; i--) {
        midi.write(stack.get(i) | 0x80);
        System.out.println("    outputting " + (stack.get(i) | 0x80));
      }
      midi.write(stack.get(0));
      System.out.println("    outputting " + stack.get(0));
    }
  }

/*****
  private static class MidiWriter {
    private DataOutputStream out;
    private int length;
    MidiWriter(DataOutputStream out) {
      this.out = out;
    }
    void write(byte[] b) throws IOException {
      this.out.write(b);
      length += b.length;
    }
    void writeInt(int v) throws IOException {
      this.out.writeInt(v);
      length += 4;
    } 
    void writeShort(int v) throws IOException {
      this.out.writeShort(v);
      length += 2;
    } 
    void writeByte(int b) throws IOException {
      this.out.writeByte(b);
      length += 1;
    } 
    int length() {
      return this.length;
    }
  }*****//** end of static inner class MidiWriter */

}/** end of class IntuneMidiFileWriter */
