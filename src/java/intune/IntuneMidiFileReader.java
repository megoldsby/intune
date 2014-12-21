
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
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
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
import javax.sound.midi.spi.MidiFileReader;

/**
 *  Class <tt>IntuneMidiFileReader</tt> reads a MIDI file.
 */
public class IntuneMidiFileReader extends MidiFileReader {

  // MIDI format values
  public static final int SINGLE_TRACK = 0;
  public static final int SIMULTANEOUS = 1;
  public static final int SEQUENTIAL   = 2;

  // end of input stream value
  private static final int EOS = -1;

  // header chunk type and track chunk type
  private static final char[] HEADER_TYPE = { 'M', 'T', 'h', 'd' };
  private static final char[] TRACK_TYPE = { 'M', 'T', 'r', 'k' };

  // length of data in header chunk, total length of header 
  private static final int HEADER_DATA_LENGTH = 6;
  private static final int HEADER_LENGTH = 8 + HEADER_DATA_LENGTH;

  // frames per second values in MIDI file for divisions of second
  private static final int SMPTE_24 = -24;
  private static final int SMPTE_25 = -25;
  private static final int SMPTE_29 = -29;
  private static final int SMPTE_30 = -30;

  // MIDI input stream
  private DataInputStream midi;

  // division type (quarter note or second, and if second,
  // number of frames per second)
  private float divisionType;

  // if division type is quarter note, number of pulses in a quarter note,
  // if division type is second, number of subfrfames per frame 
  private int resolution;

  // number of MIDI tracks
  private int numberOfTracks;

  /**
   *  Reads the MIDI file header and returns MIDI file format information.
   *  @param   file
   *           the MIDI file
   * @result   file header info
   */
  public MidiFileFormat getMidiFileFormat(File file) 
      throws InvalidMidiDataException, IOException {
    InputStream stream = new FileInputStream(file);
    boolean valid = validateMidiFileType(stream);
    if (!valid) {
      throw new InvalidMidiDataException("Invalid file type");
    }
    return readMidiFileFormat(new DataInputStream(stream));
  }

  /**
   *  Reads the MIDI file header and returns MIDI file format information.
   *  @param   url
   *           location of the MIDI file
   * @result   file header info
   */
  public MidiFileFormat getMidiFileFormat(URL url) 
      throws InvalidMidiDataException, IOException {
    InputStream stream = url.openStream();
    boolean valid = validateMidiFileType(stream);
    if (!valid) {
      throw new InvalidMidiDataException("Invalid file type");
    }
    return readMidiFileFormat(new DataInputStream(stream));
  }  

  /**
   *  Returns information in the MIDI file header.
   *  @param   stream
   *           stream connected to the MIDI file (must be a simple byte
   *           stream like a FileInputStream, cannot be for instance 
   *           an ObjectInputStream).
   *  @result  file header info
   *  @throws  InvalidMidiDataException
   *           if invalid MIDI data encountered
   *  @throws  IOException
   *           if can't read stream or stream does not support
   *           mark and reset (see MidiFileReader javadocs)
   */
  public MidiFileFormat getMidiFileFormat(InputStream stream)
      throws InvalidMidiDataException, IOException {

    // mark the stream's current position
    if (!stream.markSupported()) {
      throw new IOException("Not permitted to read unmarkable stream");
    }
    stream.mark(HEADER_TYPE.length);

    // If not valid MIDI file type, reset the stream to its 
    // former position and throw an exception
    boolean valid = validateMidiFileType(stream);
    if (!valid) {
      stream.reset();
      throw new InvalidMidiDataException();
    }

    // read and return the MIDI file info 
    return readMidiFileFormat(new DataInputStream(stream));
  }

  /**
   *  Reads file type from MIDI header and returns true
   *  if it is valid MIDI file type.
   *  @param   stream
   *           the input stream
   * @result   true if first four bytes read are 'MThd'
   * @throws   IOException
   *           if can't read the input
   */
  private boolean validateMidiFileType(InputStream stream)
      throws IOException {

    // read the first part of the header
    char[] header = new char[HEADER_TYPE.length];
    int b = 0;
    for (int i = 0; i < header.length && b != EOS; i++) {
      b = stream.read();
      header[i] = (char)b;
    }
    
    // see if it is the expected pattern and give message if it isn't
    boolean valid = Arrays.equals(header, HEADER_TYPE);
    if (!valid) {
      StringBuffer buf = new StringBuffer();
      buf.append("Doesn't look like MIDI file: expected ");
      buf.append(HEADER_TYPE);
      buf.append(", read ");
      buf.append(header);
      System.err.println(buf.toString());
    }

    // return true if it looks like valid MIDI header
    return valid;
  }

  /**
   *  Reads the MIDI file header and returns MIDI file format information.
   *  @param   midi
   *           stream positioned just past identifying pattern 
   *           ('MThd') at start of MIDI file
   *  @result  MIDI file format info
   *  @throws  InvalidMidiDataException
   *  @throws  IOException
   */
  private MidiFileFormat readMidiFileFormat(DataInputStream midi) 
      throws InvalidMidiDataException, IOException {

    // get the length of the header chunk
    int length = midi.readInt();
    if (length != HEADER_DATA_LENGTH) {
      throw new InvalidMidiDataException("Invalid header chunk length: " +
        length);
    }

    // get the format
    int formatType = midi.readShort();
    if (!(formatType == SINGLE_TRACK
          || formatType == SIMULTANEOUS
          || formatType == SEQUENTIAL)) {
      throw new InvalidMidiDataException("Invalid format type: " + formatType);
    }
      
    // number of tracks
    this.numberOfTracks = midi.readShort();
    System.out.println("number of tracks = " + this.numberOfTracks);

    // parse the division type and get the resolution
    int div0 = midi.readByte();
    int div1 = midi.readUnsignedByte();
    this.divisionType = parseDivision(div0);
    if (this.divisionType == Sequence.PPQ) {
      this.resolution = div0 * 256 + div1;
    } else {
      this.resolution = div1;
    } 

    // return the format descriptor
    return new MidiFileFormat(formatType,
                              this.divisionType,
                              this.resolution,
                              MidiFileFormat.UNKNOWN_LENGTH,
                              MidiFileFormat.UNKNOWN_LENGTH);
  }  

  /**
   *  Returns a sequence from a MIDI file.
   *  @param   file
   *           the MIDI file
   * @result   file header info
   */
  public Sequence getSequence(File file)
      throws InvalidMidiDataException, IOException {
    InputStream stream = new FileInputStream(file);
    boolean valid = validateMidiFileType(stream);
    if (!valid) {
      throw new InvalidMidiDataException("Invalid file type");
    }
    DataInputStream dataStream = new DataInputStream(stream);
    MidiFileFormat format = readMidiFileFormat(dataStream);
    return readSequence(dataStream);
  }  

  /**
   *  Returns a sequence from a MIDI file.
   *  @param   url
   *           location of the MIDI file
   * @result   file header info
   */
  public Sequence getSequence(URL url)
      throws InvalidMidiDataException, IOException {
    InputStream stream = url.openStream();
    boolean valid = validateMidiFileType(stream);
    if (!valid) {
      throw new InvalidMidiDataException("Invalid file type");
    }
    DataInputStream dataStream = new DataInputStream(stream);
    MidiFileFormat format = readMidiFileFormat(dataStream);
    return readSequence(dataStream);
  }  

  /**
   *  Returns a sequence from a MIDI file.
   *  @param   stream
   *           stream to the MIDI file
   * @result   a MIDI sequence
   */
  public Sequence getSequence(InputStream stream)
      throws InvalidMidiDataException, IOException {

    // mark the stream's current position
    if (!stream.markSupported()) {
      throw new IOException("Not permitted to read unmarkable stream");
    }
    stream.mark(HEADER_TYPE.length);

    // If not valid MIDI file type, reset the stream to its 
    // former position and throw an exception
    boolean valid = validateMidiFileType(stream);
    if (!valid) {
      stream.reset();
      throw new InvalidMidiDataException();
    }

    // read the file format info
    DataInputStream dataStream = new DataInputStream(stream);
    MidiFileFormat format = readMidiFileFormat(dataStream);

    // read and return the sequence
    return readSequence(dataStream);
  }

  /**
   *  Reads and returns the MIDI sequence.
   *  @param   dataStream
   *           stream positioned at the beginning of the sequence
   *  @result  MIDI sequence
   *  @throws  InvalidMidiDataException
   *  @throws  IOException
   */
  private Sequence readSequence(DataInputStream dataStream) 
      throws InvalidMidiDataException, IOException {

    // start a new sequence
    Sequence sequence = new Sequence(this.divisionType, this.resolution);

    // while there are tracks left..
    TrackReader reader = new TrackReader(dataStream);
    while (reader.startTrack()) {
	
      // if too many tracks, give warning and quit reading
      if (sequence.getTracks().length == this.numberOfTracks) {
        System.err.println("Warning: file contains more tracks " +
          "than # of tracks given in header (" + this.numberOfTracks + ")");
        break;
      }
 
      // create a new track in the sequence
      Track track = sequence.createTrack();

      // read the track's events and append them to the track
      MidiEvent event = null;
      while ((event = reader.getNextEvent()) != null) {
        track.add(event);
      }
    }//while more tracks

    // check whether have all the tracks
    int nrTracks = sequence.getTracks().length;
    if (nrTracks < this.numberOfTracks) {
      throw new InvalidMidiDataException("File contains " +
        nrTracks + " tracks, header specifies " + this.numberOfTracks); 
      // maybe give warning here instead?
    }
      
    // return the sequence
    return sequence;
  }  

  /**
   *  Reads and returns MIDI file format information from
   *  the given stream.
   *  @param   midi 
   *           the input stream
   *  @result  MIDI file format info (null if no valid MIDI file header)
   *  @throws  IOException
   *           if can't read from stream
   *  @throws  InvalidMidiDataException
   *           if data from stream doesn't look like a valid MIDI file
   */
  private MidiFileFormat readMidiFileFormat(InputStream stream)
      throws InvalidMidiDataException, IOException {

    // see if file header starts with valid pattern
    boolean valid = validateMidiFileType(stream);

    // if so, read the format info
    MidiFileFormat format = null;
    if (valid) {
      DataInputStream midi = new DataInputStream(stream);
      format = readMidiFileFormat(midi);
    }

    // return file format info (or null if header invalid)
    return format;
  }  

  /**
   *  Returns the division type.
   *  @param   div0   first division-type byte (signed)
   *  @result  
   */
  private float parseDivision(int div0) throws InvalidMidiDataException {
    float divisionType = 0;
    if (div0 > 0) {
      divisionType = Sequence.PPQ;
    } else if (div0 < 0) {
      int fps = -div0;
      divisionType = 
                 (fps == SMPTE_24 ? Sequence.SMPTE_24     :
                 (fps == SMPTE_25 ? Sequence.SMPTE_25     :
                 (fps == SMPTE_29 ? Sequence.SMPTE_30DROP :
                 (fps == SMPTE_30 ? Sequence.SMPTE_30     : Constants.NONE))));
      if (divisionType == Constants.NONE) {
        throw new InvalidMidiDataException(
          "Invalid division type value: " + div0);
      }
    } else if (div0 == 0) {
      throw new InvalidMidiDataException("Invalid division type: " + div0);
    }
    return divisionType;
  }

  /**
   *  Static inner class <tt>VariableLengthQuantity</tt> contains
   *  methods for reading and construcing variable length quantities.
   */
  private static class VariableLengthQuantity {

    // maximum unsigned byte valud
    private static final int MAX_7_BIT_VALUE = 127;

    /**
     *  Reads and returns a variable length quantity from the input source.
     *  <p>
     *  VLQ format is { X } Y, where X is (0x80 + a 7-bit value)
     *  and Y is a 7-bit value.  The final value never exceeds 32 bits.
     *  @param   midi
     *           the MIDI input stream 
     *  @result  the value of the VLQ
     */
    static int read(DataInputStream midi) throws IOException {
      int value = 0;
      int b = midi.readUnsignedByte();
      while ((b & 0x80) != 0) {
        value += (b & 0x7f);
        value <<= 7;
        b = midi.readUnsignedByte();
      }
      value += b;
      return value; 
    }

    /**
     *  Constructs and returns a variable length quantity given its value.
     *  @param   the value
     *  @result  the VLQ
     */
    static byte[] create(int value) throws InvalidMidiDataException {
      if (value < 0) {
        throw new InvalidMidiDataException("Can't have negative VLQ");
      }

      // divide the value into 7-bit pieces
      List<Integer> pieces = new ArrayList<Integer>();
      while (value > MAX_7_BIT_VALUE) {
        pieces.add(value & 0x7f); 
        value >>= 7;
      }
      if (value != 0) {
        pieces.add(value);
      }
      Collections.reverse(pieces);

      // turn the pieces into bytes with all but the low-order
      // byte having its high-order bit on
      byte[] bytes = new byte[pieces.size()];
      for (int i = 0; i < bytes.length-1; i++) {
        bytes[i] = (byte)(pieces.get(i) | 0x80); 
      }
      bytes[bytes.length-1] = (byte)(int)(pieces.get(bytes.length-1));

      // return the bytes
      return bytes;
    }
  }

  /**
   *  Static inner class <tt>TrackReader</tt> reads a track
   *  from the midi input, an event at a time.
   */
  private static class TrackReader {

    // meta message type for end of track event
    private static final int END_OF_TRACK = 0x2f;

    // source of midi data
    private DataInputStream midi;
 
    // bytes left in the track
    private int bytesLeft;

    // becomes true when read end of track event
    private boolean eot;

    // current (cumulative) time in the track
    private long time;

    // current status, possibly running status
    private int status;

    /**
     *  Constructor.
     *  @param  midi
     *          the input source
     */
    TrackReader(DataInputStream midi) {
      this.midi = midi;
    }

    /**
     *  Starts a new track and returns true if can read
     *  valid track type and length, otherwise returns false.
     *  @result  true if start of track
     *  @throws  IOException
     *           if i/o error (besides EOF) reading
     */
    boolean startTrack() throws IOException {
      boolean starting = true;

      char[] hdr = null;
      try {
        // read in the type bytes and verify they're valid
        hdr = new char[TRACK_TYPE.length];
        for (int i = 0; i < hdr.length; i++) {
          hdr[i] = (char)nextByte();
        }
        starting = Arrays.equals(hdr, TRACK_TYPE);

        // give informational message if they aren't valid
        if (!starting) {
          StringBuffer buf = new StringBuffer();
          buf.append("FYI: non-track data encountered: ");
          buf.append(hdr);
          System.err.println(buf.toString());
        }

        // read track data length
        if (starting) {
          this.bytesLeft = nextInt();
        }

        // initialize track time
        this.time = 0;

        // initialize current status
        this.status = Constants.NONE;

      } catch(EOFException e) { 
        // can't start if ran out of input before reading type and length
        starting = false;
      }

      // return true if found track type and length
      return starting;
    }

    /**
     *  Reads the next MIDI event and returns it.
     *  @result  next MIDI event or null if no more events in track
     */
    MidiEvent getNextEvent() 
        throws InvalidMidiDataException, IOException {
      MidiEvent event = null;

      // provided not at end of track
      if (this.bytesLeft > 0 && !this.eot) {

        // read delta time and advance track time
        int delta = readVariable();
        this.time += delta;
        
        // read the next byte
        int b = nextByte() & 0xff;
  
        // process meta message, system exclusive message or
        // short message, as the case may be, to produce an event
        if (b == MetaMessage.META) {
          event = processMetaMessage(time, b);
          if (isEndOfTrack(event)) {
            this.eot = true;
          }
        } else if (b == SysexMessage.SYSTEM_EXCLUSIVE 
                   || b == SysexMessage.SPECIAL_SYSTEM_EXCLUSIVE) {
          event = processSysexMessage(time, b);
        } else if (b > 0xf0) {
          event = processOtherSystemMessage(time, b); 
        } else {
          event = processShortMessage(time, b);
        }

     // at end of track--check a few things just for information
     } else {
       if (this.eot && this.bytesLeft > 0) {
         System.err.println("Warning: read end of track event but " +
           "there are " + this.bytesLeft + " bytes left in the track");
       } else if (!this.eot && this.bytesLeft <= 0) {
         System.err.println("Warning: no bytes left in track but " +
           "haven't read end of track event yet");
       }
       if (this.bytesLeft < 0) {
         System.err.println("Warning: track did not end exactly " +
           "where the length said it should: there were " +
           (-this.bytesLeft) + " extra bytes");
       }
     }

      // return the input event (null if events exhausted)
      return event;
    }

    /**
     *  Reads and returns a MIDI delta time from the input source.
     *  <p>
     *  Delta time format is { X } Y, where X is (0x80 + a 7-bit value)
     *  and Y is a 7-bit value.  The final value never exceeds 32 bits.
     *  @result  the delta time, in whatever units
     */
    private int readVariable() throws IOException {
      //System.out.println("readVariable:");
      int delta = 0;
      int b = nextByte();
      while ((b & 0x80) != 0) {
        delta += (b & 0x7f);
        //Formatter fmt = new Formatter();
        //fmt.format("readVariable: b = %x, delta = %x..", b, delta);
        //System.out.println(fmt);
        delta <<= 7;
        b = nextByte();
      }
      delta += b;
      //Formatter fmt = new Formatter();
      //fmt.format("readVariable: b = %x, delta = %x", b, delta);
      //System.out.println(fmt);
      return delta; 
    }

    /**
     *  Reads a system exclusive  message from the MIDI source and 
     *  constructs and returns a MIDI event containing the message and 
     *  the given time.
     *  @param  time
     *          timestamp for the event
     *  @param  sysex
     *          status (SYSTEM_EXCLUSIVE or SPECIAL_SYSTEM_EXCLUSIVE)
     *  @result event containing MetaMessage and time
     */
    private MidiEvent processSysexMessage(long time, int sysex)
        throws InvalidMidiDataException, IOException {
      System.out.println("Reading sysex message " + sysex);

      // read length and data and put them into a Sysex message
      int length = readVariable();
      byte[] data = new byte[length+1];
      data[0] = (byte)sysex; 
      for (int i = 1; i < data.length; i++) {
        data[i] = (byte)nextByte();
      } 
      SysexMessage message = new SysexMessage();
      message.setMessage(data, data.length);
      
      // enclose the message in a MIDI event and return it
      MidiEvent event = new MidiEvent(message, time);
      return event;
    }

    /**
     *  Reads a meta message from the MIDI source and constructs and
     *  returns a MIDI event containing the message and the given time.
     *  @param  time
     *          timestamp for the event
     *  @param  meta
     *          status (META)
     *  @result event containing MetaMessage and time
     */
    private MidiEvent processMetaMessage(long time, int meta)
        throws InvalidMidiDataException, IOException {
      System.out.println("Reading meta message " + meta);

      // read type, data length and data and put them into a MetaMessage
      int type = nextByte();
      int length = readVariable();
      byte[] data = new byte[length];
      for (int i = 0; i < data.length; i++) {
        data[i] = (byte)nextByte();
      } 
      MetaMessage message = new MetaMessage();
      message.setMessage(type, data, length);
      System.out.print(
        "Meta message of type " + message.getType() + " with data");
      data = message.getData();
      for (int i = 0; i < data.length; i++) {
        System.out.print(" " + data[i]);
      }
      System.out.println();

      // construct and return a MIDI event containing the 
      // meta message and the given timestamp
      MidiEvent event = new MidiEvent(message, time);
      return event;
    }

    /**
     *  Reads a system message from the MIDI source and 
     *  constructs and returns a MIDI event containing the message and 
     *  the given time.
     *  @param  time
     *          timestamp for the event
     *  @param  status
     *          status code
     *  @result event containing MetaMessage and time
     */
    private MidiEvent processOtherSystemMessage(long time, int status)
        throws InvalidMidiDataException, IOException {
      System.out.println("Reading other system message " + status);

      System.err.println("Warning: encountered a system message highly " +
        "unlikely to occur in a MIDI file: " + status);
      
      // process message type according to status byte
      ShortMessage message = null;
      switch(status) {

        case ShortMessage.MIDI_TIME_CODE:
          // 0xf1, data
          message = processMidiTimeCode();
          break;

        case ShortMessage.SONG_POSITION_POINTER:
          // 0xf2, lsb, msb
          message = processSongPositionPointer();
          break;

        case ShortMessage.SONG_SELECT:
          // 0xf3, data
          message = processSongSelect();
          break;

        case ShortMessage.TUNE_REQUEST:
          // 0xf6
          message = processTuneRequest();
          break;

        case ShortMessage.TIMING_CLOCK:
          // 0xf8
          System.err.println("And that goes double for a MIDI timing clock");
          message = processTimingClock();
          break;

        case ShortMessage.START:
          // 0xfa
          message = processStart();
          break;

        case ShortMessage.CONTINUE:
          // 0xfb
          message = processContinue();
          break;

        case ShortMessage.STOP:
          // 0xfc
          message = processStop();
          break;

        case ShortMessage.ACTIVE_SENSING:
          // 0xfe
          message = processActiveSensing();
          break;

        case ShortMessage.SYSTEM_RESET:
          // 0xff
          message = processSystemReset();
          break;
      }

      // construct and return a MIDI event containing the 
      // system message and the given timestamp
      MidiEvent event = new MidiEvent(message, time);
      return event;
    }

    /**
     *  Reads a short message from the MIDI source and constructs and
     *  returns a MIDI event containing the message and the given time.
     *  @param  time
     *          timestamp for the event
     *  @param  b
     *          current input byte
     *  @result event containing ShortMessage and time
     *          or null if it was not a short message 
     *          
     */
    private MidiEvent processShortMessage(long time, int b) 
        throws InvalidMidiDataException, IOException {
      System.out.println("Reading short message " + b + 
        " (running status = " + this.status + ")");

      // if input is a status byte, install it and read next byte
      if (isStatus(b)) {
        this.status = b;
        b = nextByte();
        System.out.println("Installed status " + this.status +
          ", read " + b);

      // if no status in sight, throw exception
      } else if (this.status == Constants.NONE) {
        throw new InvalidMidiDataException(
          "Read " + b + " when expected status byte");
      }

      // process message type corresponding to status byte
      ShortMessage message = null;
      switch(this.status & 0xf0) {

        case ShortMessage.NOTE_OFF:
          // 0x80 + channel, note, velocity
          message = processNoteOff(b);
          break;

        case ShortMessage.NOTE_ON:
          // 0x90 + channel, note, velocity
          message = processNoteOn(b);
          break;

        case ShortMessage.POLY_PRESSURE:
          // 0xa0 + channel, note, aftertouch
          message = processPolyPressure(b);
          break;

        case ShortMessage.CONTROL_CHANGE:
          // 0xb0 + channel, controller, value
          message = processControlChange(b);
          break;

        case ShortMessage.PROGRAM_CHANGE:
          // 0xc0 + channel, program
          message = processProgramChange(b);
          break;

        case ShortMessage.CHANNEL_PRESSURE:
          // 0xd0 + channel, aftertouch
          message = processChannelPressure(b);
          break;

        case ShortMessage.PITCH_BEND:
          // 0xe0 + channel, lsb, msb
          message = processPitchBend(b);
          break;

        default:
          System.err.println("Expecting status byte, read " + b);
          System.exit(-1);
          break;
      }//switch

      // construct and return a MIDI event containing the 
      // short message and the given timestamp
      MidiEvent event = null;
      if (message != null) {
        event = new MidiEvent(message, time);
      }
      return event;
    }

    /**
     *  Reads and returns a NoteOff message.
     *  @param   current input byte
     *  @result  the NoteOff message
     */
    private ShortMessage processNoteOff(int b) 
        throws InvalidMidiDataException, IOException {
      int channel = this.status & 0x0f;
      int note = b;
      int velocity = nextByte(); 
      ShortMessage message = new ShortMessage();
      message.setMessage(ShortMessage.NOTE_OFF, channel, note, velocity);
      return message;
    }

    /**
     *  Reads and returns a NoteOn message.
     *  @param   current input byte
     *  @result  the NoteOn message
     */
    private ShortMessage processNoteOn(int b)
        throws InvalidMidiDataException, IOException {
      int channel = this.status & 0x0f;
      int note = b;
      int velocity = nextByte(); 
      ShortMessage message = new ShortMessage();
      message.setMessage(ShortMessage.NOTE_ON, channel, note, velocity);
      return message;
    }

    /**
     *  Reads and returns a PolyPressure message.
     *  @param   current input byte
     *  @result  the PolyPressure message
     */
    private ShortMessage processPolyPressure(int b) 
        throws InvalidMidiDataException, IOException {
      int channel = this.status & 0x0f;
      int note = b;
      int afterTouch = nextByte(); 
      ShortMessage message = new ShortMessage();
      message.setMessage(ShortMessage.POLY_PRESSURE, channel, note, afterTouch);
      return message;
    }

    /**
     *  Reads and returns a ControlChange message.
     *  @param   current input byte
     *  @result  the ControlChange message
     */
    private ShortMessage processControlChange(int b) 
        throws InvalidMidiDataException, IOException {
      int channel = this.status & 0x0f;
      int controller = b;
      int value = nextByte(); 
      ShortMessage message = new ShortMessage();
      message.setMessage(ShortMessage.CONTROL_CHANGE, channel, controller, value);
      return message;
    }

    /**
     *  Reads and returns a ProgramChange message.
     *  @param   current input byte
     *  @result  the ProgramChange message
     */
    private ShortMessage processProgramChange(int b)
        throws InvalidMidiDataException, IOException {
      int channel = this.status & 0x0f;
      int program = b;
      ShortMessage message = new ShortMessage();
      message.setMessage(ShortMessage.PROGRAM_CHANGE, channel, program);
      return message;
    }

    /**
     *  Reads and returns a ChannelPressure message.
     *  @param   current input byte
     *  @result  the ChannelPressure message
     */
    private ShortMessage processChannelPressure(int b)
        throws InvalidMidiDataException, IOException {
      int channel = this.status & 0x0f;
      int aftertouch = b;
      ShortMessage message = new ShortMessage();
      message.setMessage(ShortMessage.CHANNEL_PRESSURE, channel, aftertouch);
      return message;
    }

    /**
     *  Reads and returns a PitchBend message.
     *  @param   current input byte
     *  @result  the PitchBend message
     */
    private ShortMessage processPitchBend(int b) 
        throws InvalidMidiDataException, IOException {
      int channel = this.status & 0x0f;
      int lsb = b;
      int msb = nextByte(); 
      ShortMessage message = new ShortMessage();
      message.setMessage(ShortMessage.PITCH_BEND, channel, lsb, msb);
      return message;
    }

    /**
     *  Reads and returns a MidiTimeCode message.
     *  @result  the MidiTimeCode message
     */
    private ShortMessage processMidiTimeCode() 
        throws InvalidMidiDataException, IOException {
      int data = nextByte(); 
      ShortMessage message = new ShortMessage();
      message.setMessage(ShortMessage.MIDI_TIME_CODE, data, 0);
      return message;
    }

    /**
     *  Reads and returns a SongPositionPointer message.
     *  @result  the SongPositionPointer message
     */
    private ShortMessage processSongPositionPointer()
        throws InvalidMidiDataException, IOException {
      int lsb = nextByte(); 
      int msb = nextByte(); 
      ShortMessage message = new ShortMessage();
      message.setMessage(ShortMessage.SONG_POSITION_POINTER, lsb, msb);
      return message;
    }

    /**
     *  Reads and returns a SongSelect message.
     *  @result  the SongSelect message
     */
    private ShortMessage processSongSelect()
        throws InvalidMidiDataException, IOException {
      int data = nextByte(); 
      ShortMessage message = new ShortMessage();
      message.setMessage(ShortMessage.SONG_SELECT, data, 0);
      return message;
    }

    /**
     *  Reads and returns a TuneRequest message.
     *  @result  the TuneRequest message
     */
    private ShortMessage processTuneRequest()
        throws InvalidMidiDataException, IOException {
      ShortMessage message = new ShortMessage();
      message.setMessage(ShortMessage.TUNE_REQUEST, 0, 0);
      return message;
    }

    /**
     *  Reads and returns a TimingCLock message.
     *  @result  the TimingClock message
     */
    private ShortMessage processTimingClock() 
        throws InvalidMidiDataException, IOException {
      ShortMessage message = new ShortMessage();
      message.setMessage(ShortMessage.TIMING_CLOCK, 0, 0);
      return message;
    }

    /**
     *  Reads and returns a Start message.
     *  @result  the Start message
     */
    private ShortMessage processStart() 
      throws InvalidMidiDataException, IOException {
      ShortMessage message = new ShortMessage();
      message.setMessage(ShortMessage.START, 0, 0);
      return message;
    }

    /**
     *  Reads and returns a Continue message.
     *  @result  the Continue message
     */
    private ShortMessage processContinue() 
        throws InvalidMidiDataException, IOException {
      ShortMessage message = new ShortMessage();
      message.setMessage(ShortMessage.CONTINUE, 0, 0);
      return message;
    }

    /**
     *  Reads and returns a Stop message.
     *  @result  the Stop message
     */
    private ShortMessage processStop()
        throws InvalidMidiDataException, IOException {
      ShortMessage message = new ShortMessage();
      message.setMessage(ShortMessage.STOP, 0, 0);
      return message;
    }

    /**
     *  Reads and returns an ActiveSensing message.
     *  @result  the ActiveSensing message
     */
    private ShortMessage processActiveSensing()
        throws InvalidMidiDataException, IOException {
      ShortMessage message = new ShortMessage();
      message.setMessage(ShortMessage.ACTIVE_SENSING, 0, 0);
      return message;
    }

    /**
     *  Reads and returns a SystemReset message.
     *  @result  the SystemReset message
     */
    private ShortMessage processSystemReset()
        throws InvalidMidiDataException, IOException {
      ShortMessage message = new ShortMessage();
      message.setMessage(ShortMessage.SYSTEM_RESET, 0, 0);
      return message;
    }

    /**
     *  Returns true if a byte has high-order bit on.
     *  @result  true if is status byte
     */
    private boolean isStatus(int b) {
      return ((b & 0x80) != 0);
    }

    /**
     *  Returns true if event is an End of Track meta event.
     *  @result  true if end of track
     */
    private boolean isEndOfTrack(MidiEvent event) {
      MetaMessage message = (MetaMessage)event.getMessage();
      return (message.getType() == END_OF_TRACK);
    }

    /**
     *  Reads and returns next integer from the MIDI data stream.
     *  @result  next integer
     *  @throws  IOException
     */
    private int nextInt() throws IOException {
      int i = this.midi.readInt();
      System.out.println("Read int " + i);
      return i;
    }

    /**
     *  Reads and returns next byte from the MIDI data stream.
     *  @result  next byte
     *  @throws  IOException
     */
    private int nextByte() throws IOException {
      int b = this.midi.readUnsignedByte();
      this.bytesLeft--;
      Formatter fmt = new Formatter();
      fmt.format("Read byte %x = %c, %d bytes left", b, b, this.bytesLeft);
      System.out.println(fmt);
      return b;
    }
  }/** end of static inner class TrackReader */

  /**
   *  Displays the events in a Midi sequence (for testing).
   */
  private static void displayEvents(Sequence sequence) {
    System.out.println();
    Track track = sequence.getTracks()[0];
    int size = track.size();
    for (int index = 0; index < size; index++ ) {
      MidiEvent event = track.get(index);
      long timestamp = event.getTick();
      MidiMessage message = event.getMessage();
      int status = message.getStatus();
      System.out.print("tick = " + timestamp); 
      if (status == ShortMessage.NOTE_ON || status == ShortMessage.NOTE_OFF) {
        ShortMessage noteMessage = (ShortMessage)message;
        int note = noteMessage.getData1();
        int velocity = noteMessage.getData2();
        if (status == ShortMessage.NOTE_ON && velocity > 0) {
          System.out.println(", Note On, note = " + note);
        } else {
          System.out.println(", Note off, note = " + note);
        }
      } else {
        System.out.println(", status = " + status);
      }
    } 
  }

  /**
   *  Tests this module.
   */
  public static void main(String[] args) {

    // if arg contains // it must be a URL, otherwise it's a file name
    File file = null;
    URL url = null;
    if (args[0].indexOf("//") >= 0) {
      try {
        url = new URL(args[0]);
      } catch(MalformedURLException e) {
        e.printStackTrace();
        System.exit(-1);
      }
    } else {
      file = new File(args[0]);
    }

    MidiFileReader reader = new IntuneMidiFileReader();
    try {
      // extract and display Midi file format info from file or URL,
      // also extract stream for future use
      MidiFileFormat format = null;
      InputStream stream;
      if (file != null) {
        format = reader.getMidiFileFormat(file);
        stream = new FileInputStream(file);
      } else {
        format = reader.getMidiFileFormat(url);
        stream = url.openStream();
      }
      System.out.println("byte length = " + format.getByteLength() +
        " division type = " + format.getDivisionType() +
        " resolution = " + format.getResolution() +
        " type = " + format.getType());
      System.out.println();

      // now extract Midi file format info from stream (should
      // succeed for URL args, fail for file arg)
      format = reader.getMidiFileFormat(stream);
      System.out.println("byte length = " + format.getByteLength() +
        " division type = " + format.getDivisionType() +
        " resolution = " + format.getResolution() +
        " type = " + format.getType());
    } catch(InvalidMidiDataException e) {
      e.printStackTrace();
    } catch(IOException e) {
      e.printStackTrace();
    }
  
    try {
      // read Midi sequence from file or URL and display its parameters,
      // also extract stream for future use
      Sequence sequence = null;
      InputStream stream;
      if (file != null) {
        sequence = reader.getSequence(file);
        stream = new FileInputStream(file);
      } else {
        sequence = reader.getSequence(url);
        stream = url.openStream();
      }
      System.out.println("division type = " + sequence.getDivisionType() +
        " usec length = " + sequence.getMicrosecondLength() +
        " resolution = " + sequence.getResolution() +
        " tick length = " + sequence.getTickLength() +
        " # of tracks = " + sequence.getTracks().length);
      displayEvents(sequence);
      System.out.println();

      // read Midi sequence from stream and display its parameters
      // (should succeed for URL arg, fail for file arg)
      sequence = reader.getSequence(stream);
      System.out.println("division type = " + sequence.getDivisionType() +
        " usec length = " + sequence.getMicrosecondLength() +
        " resolution = " + sequence.getResolution() +
        " tick length = " + sequence.getTickLength() +
        " # of tracks = " + sequence.getTracks().length);
    } catch(InvalidMidiDataException e) {
      e.printStackTrace();
    } catch(IOException e) {
      e.printStackTrace();
    }
  }
}/** end of class IntuneMidiFileReader */
