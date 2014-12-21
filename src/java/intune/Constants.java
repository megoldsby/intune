
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

public class Constants
  {
  /** a useful constant */
  static final int NONE = -1;

  /** names of various intonation types */
  public static class IntonationName {
    static final String DIATONIC = "diatonic";
    static final String PYTHAGOREAN = "pythagorean";
    static final String EQUAL_TEMPERAMENT = "equaltemperament";
  }

  /** type of tuning requests */
  public static class TuneRequest
    {
    static final int MODULATION  = 0;
    static final int SUBDOMINANT = 1;
    }

  /** various MIDI constants */
  public static class Midi 
    {
    // number of distinct MIDI notes
    static final int NOTE_LIMIT = 128;

    // default number of channels for MIDI output device
    static final int DEFAULT_NR_OF_CHANNELS = 16;

    // MIDI channel reserved for percussion effects
    static final int PERCUSSION_CHANNEL = 9;

    // the total pitch bend range in semitones
    static final int PITCH_BEND_RANGE = 2;

    // registered parameter number (RPN) selector MSB and LSB 
    static final int RPN_MSB = 0x65;
    static final int RPN_LSB = 0x64;
  
    // pitch bend range MSB, LSB (for RPN) 
    static final int PB_RANGE_MSB = 0x00;
    static final int PB_RANGE_LSB = 0x00;
  
    // RPN data entry MSB, LSB 
    static final int DATA_ENTRY_MSB = 0x06;
    static final int DATA_ENTRY_LSB = 0x26;
  
    // RPN reset MSB, LSB 
    static final int RPN_RESET_MSB          = 0x7f;
    static final int RPN_RESET_LSB          = 0x7f;
  
    // programs (timbres) per bank 
    static final int PROGRAMS_PER_BANK  = 128;
  
    // controller number for bank select MSB 
    static final int BANK_SELECT        = 0x00;
  
    // controller number for all control change LSBs 
    static final int CONTROL_CHANGE_LSB = 0x20; 

    /** Midi META type codes */
    public static class Meta 
      {
      // copyright
      static final int COPYRIGHT = 0x02;

      // set tempo
      static final int SET_TEMPO = 0x51;

      // end of track mark
      static final int END_OF_TRACK = 0x2f;

      }/** end of static inner class Meta */

    /** Midi file constants */
    public static class File 
      {
      // MIDI format values
      static final int SINGLE_TRACK = 0;
      static final int SIMULTANEOUS = 1;
      static final int SEQUENTIAL   = 2;
    
      // header chunk type and track chunk type
      static final char[] HEADER_TYPE = { 'M', 'T', 'h', 'd' };
      static final char[] TRACK_TYPE = { 'M', 'T', 'r', 'k' };
    
      // length of data in header chunk, total length of header 
      static final int HEADER_DATA_LENGTH = 6;
      static final int HEADER_LENGTH = 8 + HEADER_DATA_LENGTH;
    
      // frames per second values in MIDI file for divisions of second
      static final int SMPTE_24 = -24;
      static final int SMPTE_25 = -25;
      static final int SMPTE_29 = -29;
      static final int SMPTE_30 = -30;
    
      }/** end of static inner class File */

    }/** end of static inner class Midi */

  }/** end of class Constants */ 
