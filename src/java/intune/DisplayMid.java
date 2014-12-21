
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
import java.io.FileInputStream;
import java.io.IOException;
import javax.sound.midi.MidiMessage;

public class DisplayMid implements Runnable {

  // file type
  //   single track
  //   multi-track synchronous
  //   multi-track asynchronous
  private static final int SINGLE       = 0;
  private static final int MULTI_SYNCH  = 1;
  private static final int MULTI_ASYNCH = 2;

  // name of .mid file
  private String filename;

  /** 
   *  Constructor.
   */
  public DisplayMid(String filename) {
    this.filename = filename;
    new Thread(this).start();
  }

  public void run() {
    try {
      // open input file
      DataInputStream input = new DataInputStream(
                                new FileInputStream(this.filename));
      //FileInputStream input = new FileInputStream(this.filename);

      // skip 'MThd'
      input.readByte();      
      input.readByte();      
      input.readByte();      
      input.readByte();      

      // skip header size (6)
      input.readInt();

      int filetype = input.readShort();
      int nrTracks = input.readShort();
      int ticksPerQuarter = input.readShort();
      System.out.println("file type = " + 
        (filetype == SINGLE ? "single track"                   :
        (filetype == MULTI_SYNCH ? "multi-track synchronous"   :
        (filetype == MULTI_ASYNCH ? "multi-track asynchronous" : 
                                    "unrecognized"  ))));
      System.out.println("number of tracks = " + nrTracks);
      System.out.println("ticks per quarter = " + ticksPerQuarter);

      // read in the tracks
      byte[][] tracks = new byte[nrTracks][];
      for (int t = 0; t < nrTracks; t++) {
        
        // skip 'MTrk'
        input.readByte();      
        input.readByte();      
        input.readByte();      
        input.readByte();      

        // get length of track in bytes
        int trackLength = input.readInt();
        System.out.println("Length of track " + t + " is " + trackLength);
        tracks[t] = new byte[trackLength]; 
   
        // read in the track
        tracks[t] = new byte[trackLength]; 
        byte[] trk = tracks[t];
        for (int b = 0; b < trk.length; b++) {
          trk[b] = input.readByte(); 
        }
      }//for

      // display the tracks
      for (int t = 0; t < nrTracks; t++) {
        TrackReader trk = new TrackReader(tracks[t]);
        boolean more = true;
        while (more) {
          try {
            long time = trk.readTime();
            MidiMessage message = trk.readMessage(); 
            displayTimedMessage(time, message);
          } catch(EndOfTrackException e) {
            more = false;
          }
        }//while
      }//for
      
    } catch(IOException e) {
      e.printStackTrace();
    }
  }

  private void displayTimedMessage(long time, MidiMessage message) {

  }

  /**
   *  Gives sequential access to delta times and Midi messages 
   *  in a track given a track's bytes.
   */
  private static class TrackReader {

    // track bytes and current position
    private byte[] track;
    private int next = 0;

    /** 
     *  Constructor.
     *  @param   track
     *           the bytes of the track
     */
    TrackReader(byte[] track) {
      this.track = track;
    }

    /**
     *  Returns the next byte from the track.
     *  @result  next byte 
     *  @throws  EndOfTrackException
     *           if track is exhausted
     */
    private byte nextByte() throws EndOfTrackException {
      return this.track[this.next++];
    }

    /**
     *  Returns the delta time from the current position in the track.
     *  @result  time in ticks 
     *  @throws  EndOfTrackException
     *           if track is exhausted
     */
    long readTime() throws EndOfTrackException {
      long time = 0;
      boolean more = true;
      int k = 0;
      while (more) {
        byte b = nextByte();
        k++;
        byte val = (byte)(b & 0x7f);
        time = (time << 7) + val;
        more = (val != b);
      } 
      System.out.println("delta time " + time + " " + k + " bytes");
      return time;
    }

    /**
     *  Returns the Midi message from the current position in the track.
     *  @result  Midi message
     *  @throws  EndOfTrackException
     *           if track is exhausted
     */
    MidiMessage readMessage() throws EndOfTrackException {
      byte b = nextByte();
      if (b == 0xff) {

      } else if ((b & 0xf0) == 0xf0) {

      } else if ((b & 0x80) != 0) {
      
      } else {

      }
      return null;
    }
  }/** end of static inner class TrackReader */

  /**
   *  Thrown when hit end of track.
   */
  private static class EndOfTrackException extends Exception {
    EndOfTrackException(String text) {
      super(text);
    }
  }

  /**
   */
  public static void main(String[] args){
    if (args.length < 1) {
      System.err.println("Usage: java intune.DisplayMid file.mid");
      System.exit(-1);
    }
    new DisplayMid(args[0]);
  }


} /** end of class DisplayMid */
