
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
import java.util.Map;
import java.util.HashMap;

/**
 *  Class <tt>Diatonic</tt> provides distonic (just) intonation.
 *
 *  @author Michael E. Goldsby
 *  @version  2005
 *
 *  @version  Nov 11, 2005
 *  Made access to instance variables currentKey and currentTonic
 *  synchronized.
 */
class Diatonic implements ModulatingIntonation, Modulator
  {
  /** middle C (for purposes of judging what octave a note is in) */
  static final double MIDDLE_C = CONCERT_A * 3./5.;

  /** midi note number for middle C */
  static final int MIDI_MIDDLE_C = 60;

  /** number of midi notes */
  static final int MIDI_NOTE_LIMIT = 128;

  /** number of MIDI notes per octave */
  static final int MIDI_NOTES_PER_OCTAVE = 12;

  /**
   *  major and minor scale types
   */
  static class ScaleType extends Enumerate
    {
    static final ScaleType MAJOR = new ScaleType("major");
    static final ScaleType MINOR = new ScaleType("minor");
    private ScaleType(String name) { super(name); }
    }

  /**
   *  Static inner class <code>Key</code> expresses the notion
   *  of a musical key.
   */
  static class Key
    {
    /** tonic note of the key */
    Note tonic;

    /** type of key (major or minor) */
    ScaleType type; 

    /**
     *  Constructor.
     *  @param   tonic  tonic note of the key
     *  @param   type   type of the key (major or minor)
     */
    Key(Note tonic, ScaleType type)
      {
      // save tonic and type, making sure note is in octave 0
      this.tonic = new Note(tonic.name, tonic.accidental);
      this.type = type;
      }
    /**
     *  Returns true if given object denotes the same key as this one
     *  (needed if use <code>Key</code> objects as map or set keys).
     *  @param   that  the given object
     *  @result  true  if given object is a <code>Key</code>
     *                 that has the same note name, note accidental
     *                 and scale type.
     */
    public boolean equals(Object that)
      {
      return (this == that
              || (that instanceof Key
                  && this.tonic.name == ((Key)that).tonic.name
                  && this.tonic.accidental == ((Key)that).tonic.accidental
                  && this.type == ((Key)that).type));
      }
    /**
     *  Returns a hash code for this object (needed if use
     *  <code>Key</code> objects as map or set keys).
     *  @result  hash code
     */
    public int hashCode()
      {
      return tonic.name.hashCode() 
             ^ tonic.accidental.hashCode() 
             ^ type.hashCode();
      }
    public String toString()
      {
      StringBuffer buf = new StringBuffer();
      buf.append(this.tonic);
      if (type == ScaleType.MAJOR)
        {
        buf.append(" major");
        }
      else 
        {
        buf.append(" minor");
        }
      return buf.toString();
      }
    }/** end of static inner class Key */

  /** the degrees are 1, 2, 3, 4, 5, 6, 7 */
  private static int DEGREES = 7;

  /** 
   *  map containing the accidentals of each degree of the
   *  scale for all the supported keys:
   *    Note.Accidental[DEGREES] versus Key
   */
  private static Map accidentals = new HashMap();
  static
    {
    // include all keys except those whose tonic 
    // would have a double accidental
    accidentals.put(new Key(new Note(Note.Name.C, 
                                     Note.Accidental.NATURAL),
                                     ScaleType.MAJOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.G, 
                                     Note.Accidental.NATURAL),
                                     ScaleType.MAJOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.SHARP
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.D, 
                                     Note.Accidental.NATURAL),
                                     ScaleType.MAJOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.SHARP,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.SHARP
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.A, 
                                    Note.Accidental.NATURAL),
                                    ScaleType.MAJOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.SHARP,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.E, 
                                     Note.Accidental.NATURAL),
                                     ScaleType.MAJOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.NATURAL,
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.B, 
                                     Note.Accidental.NATURAL),
                                     ScaleType.MAJOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.NATURAL,
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.NATURAL,
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.F, 
                                     Note.Accidental.SHARP),
                                     ScaleType.MAJOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.NATURAL,
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.C, 
                                     Note.Accidental.SHARP),
                                     ScaleType.MAJOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.G, 
                                     Note.Accidental.SHARP),
                                     ScaleType.MAJOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.DOUBLE_SHARP
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.D, 
                                     Note.Accidental.SHARP),
                                     ScaleType.MAJOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.DOUBLE_SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.DOUBLE_SHARP
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.A, 
                                     Note.Accidental.SHARP),
                                     ScaleType.MAJOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.DOUBLE_SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.DOUBLE_SHARP,
                      Note.Accidental.DOUBLE_SHARP
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.E, 
                                     Note.Accidental.SHARP),
                                     ScaleType.MAJOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.SHARP,
                      Note.Accidental.DOUBLE_SHARP,
                      Note.Accidental.DOUBLE_SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.DOUBLE_SHARP,
                      Note.Accidental.DOUBLE_SHARP
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.B, 
                                     Note.Accidental.SHARP),
                                     ScaleType.MAJOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.SHARP,
                      Note.Accidental.DOUBLE_SHARP,
                      Note.Accidental.DOUBLE_SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.DOUBLE_SHARP,
                      Note.Accidental.DOUBLE_SHARP,
                      Note.Accidental.DOUBLE_SHARP
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.A, 
                                     Note.Accidental.NATURAL),
                                     ScaleType.MINOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.E, 
                                     Note.Accidental.NATURAL),
                                     ScaleType.MINOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.NATURAL,
                      Note.Accidental.SHARP,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.B, 
                                     Note.Accidental.NATURAL),
                                     ScaleType.MINOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.NATURAL,
                      Note.Accidental.SHARP,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.SHARP,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.F, 
                                     Note.Accidental.SHARP),
                                     ScaleType.MINOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.SHARP,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.C, 
                                     Note.Accidental.SHARP),
                                     ScaleType.MINOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.NATURAL,
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.G, 
                                     Note.Accidental.SHARP),
                                     ScaleType.MINOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.NATURAL,
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.NATURAL,
                      Note.Accidental.SHARP
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.D, 
                                     Note.Accidental.SHARP),
                                     ScaleType.MINOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.NATURAL,
                      Note.Accidental.SHARP
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.A, 
                                     Note.Accidental.SHARP),
                                     ScaleType.MINOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.E, 
                                     Note.Accidental.SHARP),
                                     ScaleType.MINOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.SHARP,
                      Note.Accidental.DOUBLE_SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.B, 
                                     Note.Accidental.SHARP),
                                     ScaleType.MINOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.SHARP,
                      Note.Accidental.DOUBLE_SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.DOUBLE_SHARP,
                      Note.Accidental.SHARP,
                      Note.Accidental.SHARP
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.F, 
                                     Note.Accidental.NATURAL),
                                     ScaleType.MAJOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.FLAT,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.B, 
                                     Note.Accidental.FLAT),
                                     ScaleType.MAJOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.FLAT,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.FLAT,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.E, 
                                     Note.Accidental.FLAT),
                                     ScaleType.MAJOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.FLAT,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.A, 
                                     Note.Accidental.FLAT),
                                     ScaleType.MAJOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.NATURAL,
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.D, 
                                     Note.Accidental.FLAT),
                                     ScaleType.MAJOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.NATURAL,
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.NATURAL
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.G, 
                                     Note.Accidental.FLAT),
                                     ScaleType.MAJOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.NATURAL
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.C, 
                                     Note.Accidental.FLAT),
                                     ScaleType.MAJOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.F, 
                                     Note.Accidental.FLAT),
                                     ScaleType.MAJOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.DOUBLE_FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.D, 
                                     Note.Accidental.NATURAL),
                                     ScaleType.MINOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.FLAT,
                      Note.Accidental.NATURAL
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.G, 
                                     Note.Accidental.NATURAL),
                                     ScaleType.MINOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.FLAT,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.FLAT,
                      Note.Accidental.NATURAL
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.C, 
                                     Note.Accidental.NATURAL),
                                     ScaleType.MINOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.FLAT,
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.F, 
                                     Note.Accidental.NATURAL),
                                     ScaleType.MINOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.NATURAL,
                      Note.Accidental.NATURAL,
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.NATURAL,
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.B, 
                                     Note.Accidental.FLAT),
                                     ScaleType.MINOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.FLAT,
                      Note.Accidental.NATURAL,
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.NATURAL,
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.E, 
                                     Note.Accidental.FLAT),
                                     ScaleType.MINOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.FLAT,
                      Note.Accidental.NATURAL,
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.A, 
                                     Note.Accidental.FLAT),
                                     ScaleType.MINOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.D, 
                                     Note.Accidental.FLAT),
                                     ScaleType.MINOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.DOUBLE_FLAT,
                      Note.Accidental.FLAT
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.G, 
                                     Note.Accidental.FLAT),
                                     ScaleType.MINOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.DOUBLE_FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.DOUBLE_FLAT,
                      Note.Accidental.FLAT
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.C, 
                                     Note.Accidental.FLAT),
                                     ScaleType.MINOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.DOUBLE_FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.DOUBLE_FLAT,
                      Note.Accidental.DOUBLE_FLAT
                      }
                   );
    accidentals.put(new Key(new Note(Note.Name.F, 
                                     Note.Accidental.FLAT),
                                     ScaleType.MINOR),
                    new Note.Accidental[]
                      {
                      Note.Accidental.FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.DOUBLE_FLAT,
                      Note.Accidental.DOUBLE_FLAT,
                      Note.Accidental.FLAT,
                      Note.Accidental.DOUBLE_FLAT,
                      Note.Accidental.DOUBLE_FLAT
                      }
                   );
                    
    

    }

  //////////////////////////////////////////////////////////////////////
  //  instance variables
  //////////////////////////////////////////////////////////////////////

  /**  
   *  key of the composition and frequency of its tonic 
   */
  private Key key;
  private double keyTonic;

  /** 
   *  current key and tonic frequency 
   */
  private Key currentKey;
  private double currentTonic;

  private Key originalKey;        // for test printout
  private double originalTonic; 

  /** 
   *  true means use tunings consistent with subdominant,
   *  false means use tunings consistent with dominant 
   */
  private boolean subdominant = false;

  //////////////////////////////////////////////////////////////////////
  //  end of instance variables
  //////////////////////////////////////////////////////////////////////

  /**
   *  Constructor.
   *  @param   key           nominal key of the composition
   *  @param   
   *  @param   keyTonic      frequency of tonic of nominal key
   */
  Diatonic(Key key, double keyTonic)
    {
    // remember the key
    this.key = key;
    this.keyTonic = octave0(keyTonic);

    this.originalKey = key;              // for test printout
    this.originalTonic = this.keyTonic;  

    // make it the current key
    setCurrentKey(this.key, this.keyTonic);
    if (Diatonic.accidentals.get(key) == null)
      {
      throw new IllegalArgumentException(
        "Attempt to establish unsupported key: " + key);
      }

    }

  /**
   *  Returns the frequency of a given note in the current key.
   *  @param   note  
   *           the note
   *  @param   modulation
   *           true if the frequency is to be used as 
   *           the new tonic of a key we are modulating to
   *  @result  the frequency of the note
   */
  private double frequency(Note note, boolean modulation) 
      throws UnsupportedNoteException
    {
    // get subdominant indicator, current key, current tonic
    boolean subdom = getSubdominant();
    Key currKey = getCurrentKey();
    double currTonic = getTonicFrequency();

    // learn what degree of the scale the note is
    // and get the accidental for that degree in current key
    int degree = noteToDegree(note);
    //System.out.println("frequency: degree = " + degree);
    Note.Accidental degreeAccidental = 
      ((Note.Accidental[])Diatonic.accidentals.get(currentKey))[degree-1];

    // get note shorn of duration etc.
    Note degreeNote = new Note(note.name, degreeAccidental);
    Note givenNote = new Note(note.name, note.accidental);

    if (note.accidental != degreeAccidental)
      {
      System.err.println("Info: accidental doesn't match that of degree " +
        "of scale: " + note + " " + degree);
      }

    double frequency = 0.0;    
    switch (degree)
      {
      case 1:
        if (givenNote.equals(degreeNote))
          {
          frequency = currTonic; 
          }
        else if (givenNote.equals(raise(degreeNote))) 
          {
          // raised tonic (really weird)
          frequency = octave0(currTonic * (27./25.));
          }
        break;

      case 2:
	//System.out.println("case 2");
        if (givenNote.equals(degreeNote)) 
          {
          //System.out.println("given = degree");
          // normal 2nd
          if (subdom)
            {
            frequency = octave0(currTonic * (10./9.));
            }
          else
            {
            frequency = octave0(currTonic * (9./8.));
            }
	  }
        else if (givenNote.equals(lower(degreeNote))) 
          {
          // Phrygian second
          if (subdom)
            {
            frequency = octave0(currTonic * (16./15.));
	    //System.out.println("16/15, tonic, freq = " + 
            //  currTonic + " " + frequency);
            }
          else
            {
            frequency = octave0(currTonic * (27./25.));
	    //System.out.println("27/25, tonic, freq = " + 
            //  currTonic + " " + frequency);
            }
          }
        else
          {
          throw new UnsupportedNoteException(
                "Invalid 6th degree of scale: " + note +
                " in key " + currentKey); 
          }
        break;

      case 3:
        if (currKey.type == ScaleType.MINOR) 
          {
          if (givenNote.equals(degreeNote))
            {
            // normal 3rd of minor key
            frequency = octave0(currTonic * (6./5.));
            }
          else if (givenNote.equals(raise(degreeNote))) 
            {
            // major 3rd of minor key
            frequency = octave0(currTonic * (5./4.));
            }
          }
        else if (currKey.type == ScaleType.MAJOR) 
          {
          if (givenNote.equals(degreeNote))
            {
            // normal 3rd of major key
            frequency = octave0(currTonic * (5./4.));
            }
          else if (givenNote.equals(lower(degreeNote)))
            {
            // minor 3rd of major key          
            frequency = octave0(currTonic * (6./5.));
            }
          }
        break;

      case 4:
        frequency = octave0(currTonic * (4./3.));
        // (forget about septimal 7ths for now)
        break;

      case 5:
        if (givenNote.equals(degreeNote))
          {
          // normal 5th
          frequency = octave0(currTonic * (3./2.));
          }
        else if (givenNote.equals(lower(degreeNote)))
          {
// maybe make midiNoteNumberToNote return an augmented 4th
// instead of a diminshed 5th
// or let this method change or perhaps the modulate(Note)
// method change an augmented 4th to a diminished 5th
          // diminished 5th
          if (!modulation)
            {
            //frequency = octave0(currTonic * 36./25.);
            //frequency = octave0(currTonic * 45./32.);
            //frequency = octave0(currTonic * 64./45.);
            // for playing, use a major 6th above the major 6th
            // (really an augmented 4th)
	    frequency = octave0(currTonic * (25./18.));
            }
          else
            {
            // for modulations, use
            frequency = octave0(currTonic * (64./45.));
            }
          }
        else if (givenNote.equals(raise(degreeNote)))
          {
          // augmented 5th
          frequency = octave0(currTonic * (25./16.));          
          }
        break;

      case 6:
        if (currKey.type == ScaleType.MINOR)
          {
          if (givenNote.equals(degreeNote))
            {
            // normal 6th of minor scale
            frequency = octave0(currTonic * (8./5.)); 
            }
          else if (givenNote.equals(raise(degreeNote)))
            {
            // major 6th of minor scale
            frequency = octave0(currTonic * (5./3.)); 
            }
          else 
            {
            throw new UnsupportedNoteException(
                "Invalid 6th degree of scale: " + note +
                " in key " + currentKey); 
            }
	  }
        else if (currKey.type == ScaleType.MAJOR)
          {
          if (givenNote.equals(degreeNote))
            {
            // major 6th in major scale
            frequency = octave0(currTonic * (5./3.)); 
            }
          else if (givenNote.equals(lower(degreeNote)))
            {
            // minor 6th in major scale
            frequency = octave0(currTonic * (8./5.)); 
            }
          else 
            {
            throw new UnsupportedNoteException(
                "Invalid 6th degree of scale: " + note +
                " in key " + currentKey); 
            }
          }
        break;

      case 7:
        if (currKey.type == ScaleType.MINOR)
          {
          if (givenNote.equals(degreeNote))
            {
            // minor 7th in minor scale
            if (subdom)
              {
              frequency = octave0(currTonic * (16./9.));
              }
            else 
              {
              frequency = octave0(currTonic * (9./5.));
              }
	    }
          else if (givenNote.equals(raise(degreeNote)))
            {
            // major 7th in minor scale
            frequency = octave0(currTonic * (15./8.)); 
            }
          else 
            {
            throw new UnsupportedNoteException(
                "Invalid 7th degree of scale: " + note +
                " in key " + currentKey); 
            }
          }
        else if (currKey.type == ScaleType.MAJOR)
          {
          if (givenNote.equals(lower(degreeNote)))
            {
            // minor 7th in major scale 
            if (subdom)
              {
              frequency = octave0(currTonic * (16./9.));
              }
            else
              {
              frequency = octave0(currTonic * (9./5.));
              }
	    }
          else if (givenNote.equals(degreeNote))
            {
            // major 7th in major scale
            frequency = octave0(currTonic * (15./8.)); 
            }
          else 
            {
            throw new UnsupportedNoteException(
                "Invalid 7th degree of scale: " + note +
                " in key " + currentKey); 
            }
          }
        break;

      default:
        System.err.println("Invalid scale degree " + degree +
                           ": should not happen");
        break;
      }//switch
    //System.out.println("***** freq. before apply octave " + frequency);
    int octave = note.octave;
    while (octave > 0)
      {
      frequency += frequency;
      octave--;
      } 
    while (octave < 0)
      {
      frequency /= 2;
      octave++;
      } 
    
    return frequency;
    }
  /*****
  public double frequency(Note note) throws UnsupportedNoteException
    {
    int degree = noteToDegree(note);
    Note.Accidental degreeAccidental = 
      ((Note.Accidental[])Diatonic.accidentals.get(currentKey))[degree-1];
    Note degreeNote = null; 
    Note givenNote = null;   // given note shorn of duration, etc.

    if (note.accidental != degreeAccidental)
      {
      System.out.println("Accidental doesn't match that of degree of scale: " +
                         note + " " + degree);
      }

    double frequency = 0.0;    
    switch (degree)
      {
      case 1:
        frequency = currTonic; 
        break;

      case 2:
        if (note.modifier == Note.PitchModifier.LOWERED_2ND)
          {
          System.out.println("***** 10/9 " + note);
          frequency = octave0((10./9.)*currTonic); 
          }
        else
          {
          System.out.println("***** 9/8 " + note);
          frequency = octave0((9./8.)*currTonic); 
          }
        break;

      case 3:
        frequency = (currKey.type == ScaleType.MAJOR ?
                     octave0((5./4.)*currTonic) :
                     octave0((6./5.)*currTonic));
        break;

      case 4:
        if (note.modifier != Note.PitchModifier.SEPTIMAL_7TH)
          {
          frequency = octave0((4./3.)*currTonic);
          }
        else
          {
          frequency = octave0((21./16.)*currTonic);
          }
        break;

      case 5:
        frequency = octave0((3./2.)*currTonic);
        break;

      case 6:
        degreeNote = new Note(note.name, degreeAccidental); 
        givenNote = new Note(note.name, note.accidental); 
        if (currKey.type == ScaleType.MAJOR)
          {
          if (givenNote.equals(degreeNote))
            {
            frequency = octave0((5./3.)*currTonic); 
            }
          else if (givenNote.equals(lower(degreeNote)))
            {
            frequency = octave0((8./5.)*currTonic); 
            }
          else 
            {
            throw new UnsupportedNoteException(
                "Invalid 6th degree of scale: " + note +
                " in key " + currentKey); 
            }
          }
        else
          {
          if (givenNote.equals(degreeNote))
            {
            frequency = octave0((8./5.)*currTonic); 
            }
          else if (givenNote.equals(raise(degreeNote)))
            {
            frequency = octave0((5./3.)*currTonic); 
            }
          else 
            {
            throw new UnsupportedNoteException(
                "Invalid 6th degree of scale: " + note +
                " in key " + currentKey); 
            }
          }
        break;

      case 7:
        degreeNote = new Note(note.name, degreeAccidental); 
        givenNote = new Note(note.name, note.accidental); 
        if (currKey.type == ScaleType.MAJOR)
          {
          if (givenNote.equals(degreeNote))
            {
            frequency = octave0((15./8.)*currTonic); 
            }
          else if (givenNote.equals(lower(degreeNote)))
            {
            frequency = octave0((9./5.)*currTonic); 
            }
          else 
            {
            throw new UnsupportedNoteException(
                "Invalid 7th degree of scale: " + note +
                " in key " + currentKey); 
            }
          }
        else
          {
          if (givenNote.equals(degreeNote))
            {
            frequency = octave0((9./5.)*currTonic); 
            }
          else if (givenNote.equals(raise(degreeNote)))
            {
            frequency = octave0((15./8.)*currTonic); 
            }
          else 
            {
            throw new UnsupportedNoteException(
                "Invalid 7th degree of scale: " + note +
                " in key " + currentKey); 
            }
          }
        break;

      default:
        System.err.println("Invalid scale degree " + degree +
                           ": should not happen");
        break;
      }
    //System.out.println("***** freq. before apply octave " + frequency);
    int octave = note.octave;
    while (octave > 0)
      {
      frequency += frequency;
      octave--;
      } 
    while (octave < 0)
      {
      frequency /= 2;
      octave++;
      } 
    
    return frequency;
    }
  *****/

  /**
   *  Returns the frequency of a given note in the current key.
   *  @param   note  
   *           the note
   *  @result  the frequency of the note
   */
  public double frequency(Note note)
      throws UnsupportedNoteException
    {
    // return the frequency computed for playing, not modulation
    return frequency(note, false);
    }

  /**
   *  Returns <code>Note</code> corresponding to a given MIDI note number.
   *  @param  noteNumber
   *          the MIDI note number
   *  @result the corresponding <code>Note</code>
   *  @throws UnsupportedNoteException
   *          if note is not in the MIDI range
   */
  public Note midiNoteNumberToNote(int noteNumber)
      throws UnsupportedNoteException
    {
    final int MIDI_C_ABOVE_MIDDLE_C = MIDI_MIDDLE_C + 12;
    final int OCTAVE = 12;

    // first make sure it's really a MIDI note
    if (noteNumber < 0 || noteNumber >= MIDI_NOTE_LIMIT)
      {
      throw new UnsupportedNoteException(
        "MIDI note number out of range: " + noteNumber);
      }

    // discover what octave the note is in
    int octave = 0;
    while (noteNumber >= MIDI_C_ABOVE_MIDDLE_C)
      {
      octave++;
      noteNumber -= OCTAVE;
      }
    while (noteNumber < MIDI_MIDDLE_C)
      {
      octave--;
      noteNumber += OCTAVE;
      }

    // discover number of MIDI notes between tonic and given note
    Key currKey = getCurrentKey();
    int numberOfTonic = noteToMidiNoteNumber(currKey.tonic);
    int difference = noteNumber - numberOfTonic;
    while (difference < 0)
      {
      difference += MIDI_NOTES_PER_OCTAVE;
      }
    //System.out.println("Note number, tonic number, diff = " +
    //  noteNumber + " " + numberOfTonic + " " + difference);

    // note depends on that number and the current key
    Note note = null;
    switch (difference)
      {
      case 0:
        note = new Note(currKey.tonic.name,
	                currKey.tonic.accidental, octave);
        break;

      case 1:
        note = lower(degreeToNote(2));
        break;

      case 2:
        note = degreeToNote(2);
        break;

      case 3:
        if (currKey.type == ScaleType.MINOR)
	  {
	  note = degreeToNote(3); 
	  }
	else if (currKey.type == ScaleType.MAJOR)
	  {
	  note = lower(degreeToNote(3)); 
	  }
        break;

      case 4:
        if (currKey.type == ScaleType.MINOR)
	  {
	  note = raise(degreeToNote(3)); 
	  }
	else if (currKey.type == ScaleType.MAJOR)
	  {
	  note = degreeToNote(3); 
	  }
        break;

      case 5:
        note = degreeToNote(4);
        break;

      case 6:
        note = lower(degreeToNote(5));
        break;

      case 7:
        note = degreeToNote(5);
        break;

      case 8:
        if (currKey.type == ScaleType.MINOR)
	  {
	  note = degreeToNote(6); 
	  }
	else if (currKey.type == ScaleType.MAJOR)
	  {
	  note = lower(degreeToNote(6)); 
	  }
        break;

      case 9:
        if (currKey.type == ScaleType.MINOR)
	  {
	  note = raise(degreeToNote(6)); 
	  }
	else if (currKey.type == ScaleType.MAJOR)
	  {
	  note = degreeToNote(6); 
	  }
        break;

      case 10:
        if (currKey.type == ScaleType.MINOR)
	  {
	  note = degreeToNote(7); 
	  }
	else if (currKey.type == ScaleType.MAJOR)
	  {
	  note = lower(degreeToNote(7)); 
	  }
        break;

      case 11:
        if (currKey.type == ScaleType.MINOR)
	  {
	  note = raise(degreeToNote(7));
	  }
	else if (currKey.type == ScaleType.MAJOR)
	  {
	  note = degreeToNote(7); 
	  }
        break;

      default:
        System.err.println("Should not happen");
        new Exception().printStackTrace();
        System.exit(-1);
      }
    //System.out.println("Developed note = " + note);

    // return the note
    return note;
    }

  /**
   *  Returns the MIDI note number corresponding to a given note.
   *  @param   note   the given note
   *  @result  corresponding MIDI note number
   */
  private int noteToMidiNoteNumber(Note note) 
    {
    final int INCONCEIVABLE = 1234;

    // set note number in lowest octave for note name
    int noteNumber = (note.name == Note.Name.C ?  0 : 
                     (note.name == Note.Name.D ?  2 :
                     (note.name == Note.Name.E ?  4 :
                     (note.name == Note.Name.F ?  5 :
                     (note.name == Note.Name.G ?  7 :
                     (note.name == Note.Name.A ?  9 :
                     (note.name == Note.Name.B ? 11 : 
                               INCONCEIVABLE )))))));

    // adjust note number for accidentals
    noteNumber += (note.accidental == Note.Accidental.DOUBLE_FLAT  ? -2 :
                  (note.accidental == Note.Accidental.FLAT         ? -1 :
                  (note.accidental == Note.Accidental.NATURAL      ?  0 :
                  (note.accidental == Note.Accidental.SHARP        ?  1 :
                  (note.accidental == Note.Accidental.DOUBLE_SHARP ?  2 :
                                                     INCONCEIVABLE )))));

    // put note number in correct octave
    int lowerBound = MIDI_MIDDLE_C + note.octave * MIDI_NOTES_PER_OCTAVE; 
    while (noteNumber < lowerBound)
      {
      noteNumber += MIDI_NOTES_PER_OCTAVE;
      }

    // return MIDI note number
    return noteNumber;
    }

  /**
   *  Effects a modulation to a new key.
   *  @param   degree
   *           degree of the scale to modulate to
   *  @param   modifier
   *           modifier with format ["m" | "M"], where
   *           "m" means minor and "M" means major
   *  @throws  IllegalArgumentException
   *           if the degree of the scale is not in 1, 2, .., 7
   *  @throws  InvalidSyntaxException
   *           if the modifier's format is invalid
   *  @throws  UnsupportedNoteException
   *           if an unsupported modulation is specified
   */
  public synchronized void modulate(int degree, String modifier)
        throws InvalidSyntaxException,
               UnsupportedNoteException
    {
    final int NO_MODIFIER = 0;
    final int MAJOR_MODIFIER = 1;
    final int MINOR_MODIFIER = -1;
    int modifierType = NO_MODIFIER;
    if (modifier.equals(""))
      { 
      modifierType = NO_MODIFIER;
      }
    else if (modifier.equals("m"))
      {
      modifierType = MINOR_MODIFIER;
      }
    else if (modifier.equals("M"))
      {
      modifierType = MAJOR_MODIFIER;
      }
    else
      {
      throw new InvalidSyntaxException(
        "Invalid modulation modifier " + modifier);
      }

    //System.out.println("***** modulating to degree " + degree);

    // Need to change the frequency and the note together,
    // making sure the note's octave matches the frequency.
    // In 'frequency()', use the note as the reference point.
    // (Therefore it doesn't much matter what octave the note is in).

    // find the new tonic frequency
    Key currKey = getCurrentKey();
    double currTonic = getTonicFrequency();
    Key newKey = null;
    double newTonic = 0;
    switch (degree)
      {
      case 1:
        // this is no modulation at all
        newKey = currKey;
        newTonic = currTonic;
        break;

      case 2:
        // use the subdominant's second
	// (which is an unwarranted assumption)
        if (currKey.type == ScaleType.MAJOR) 
          {
          ScaleType scaleType = (modifierType == MAJOR_MODIFIER ?
                                 ScaleType.MAJOR : ScaleType.MINOR);
          newKey = new Key(degreeToNote(degree), scaleType);
          newTonic = octave0((10./9.)*currTonic);
          } 

        // is this every used?
	// (I hope not, it's wrong..)
        else 
          {
          newKey = new Key(lower(degreeToNote(degree)), ScaleType.MAJOR);
          newTonic = octave0((16./15.)*currTonic);
          }
        break;

      case 3:
        if (currKey.type == ScaleType.MAJOR) 
          {
          ScaleType scaleType = (modifierType == MAJOR_MODIFIER ?
                                 ScaleType.MAJOR : ScaleType.MINOR);
          newKey = new Key(degreeToNote(degree), scaleType);
          newTonic = octave0((5./4.)*currTonic); 
          }
        else 
          {
          ScaleType scaleType = (modifierType == MINOR_MODIFIER ?
                                 ScaleType.MINOR : ScaleType.MAJOR);
          newKey = new Key(degreeToNote(degree), scaleType);
          newTonic = octave0((6./5.)*currTonic); 
          }
        break;

      case 4:
        if (currKey.type == ScaleType.MAJOR) 
          {
          ScaleType scaleType = (modifierType == MINOR_MODIFIER ?
                                 ScaleType.MINOR : ScaleType.MAJOR);
          newKey = new Key(degreeToNote(degree), scaleType);
          }
        else 
          {
          ScaleType scaleType = (modifierType == MAJOR_MODIFIER ?
                                 ScaleType.MAJOR : ScaleType.MINOR);
          newKey = new Key(degreeToNote(degree), scaleType);
          }
        newTonic = octave0((4./3.)*currTonic); 
        break;

      case 5:
        {
        ScaleType scaleType = (modifierType == MINOR_MODIFIER ?
                                 ScaleType.MINOR : ScaleType.MAJOR);
        newKey = new Key(degreeToNote(degree), scaleType);
        newTonic = octave0((3./2.)*currTonic); 
        }
        break;

      case 6:
        // agrees with subdominant
        if (currKey.type == ScaleType.MAJOR) 
          {
          ScaleType scaleType = (modifierType == MAJOR_MODIFIER ?
                                 ScaleType.MAJOR : ScaleType.MINOR);
          newKey = new Key(degreeToNote(degree), scaleType);
          newTonic = octave0((5./3.)*currTonic); 
          }
        else 
          {
          ScaleType scaleType = (modifierType == MINOR_MODIFIER ?
                                 ScaleType.MINOR : ScaleType.MAJOR);
          newKey = new Key(degreeToNote(degree), scaleType);
          newTonic = octave0((8./5.)*currTonic); 
          }
        break;

      case 7:
        if (currKey.type == ScaleType.MAJOR) 
          {
          ScaleType scaleType = (modifierType == MAJOR_MODIFIER ?
                                 ScaleType.MAJOR : ScaleType.MINOR);
          newKey = new Key(degreeToNote(degree), scaleType);
          newTonic = octave0((15./8.)*currTonic); 
          // note: modulation to the lowered seventh is also
          // possible but is not currently supported
          }
        else 
          {
          ScaleType scaleType = (modifierType == MINOR_MODIFIER ?
                                 ScaleType.MINOR : ScaleType.MAJOR);
          newKey = new Key(degreeToNote(degree), scaleType);
          newTonic = octave0((9./5.)*currTonic); 
          }
        break;

      default:
        throw new IllegalArgumentException(
          "Invalid modulation degree: " + degree);
      }

    // install the new key
    setCurrentKey(newKey, newTonic);
    }

  /**
   *  Effects a modulation to a new key.
   *  @param   tonic
   *           tonic note of the new key
   */
  public void modulate(Note tonic) 
    {
    // find the new tonic frequency
    double newTonicFreq = 0;
    try
      {
      newTonicFreq = octave0(frequency(tonic, true));  
      }
    catch(UnsupportedNoteException e) 
      {
      e.printStackTrace();
      System.exit(-1);
      }
  
    // install the new key (major/minor doesn't matter)
    // and tonic frequency
    Key newKey = new Key(tonic, ScaleType.MAJOR);
    setCurrentKey(newKey, newTonicFreq);
    }

  /**
   *  Effects a modulation to a new key, forcing the frequency
   *  of the tonic note of the new key to a given value.
   *  @param   tonic
   *           tonic note of the new key
   *  @param   frequency
   *           frequency of tonic note of the new key
   */
  public void modulate(Note tonic, double frequency) 
    {
    // install the new key (major/minor doesn't matter)
    // and tonic frequency
    Key newKey = new Key(tonic, ScaleType.MAJOR);
    setCurrentKey(newKey, frequency);
    }

  /**
   *  Turns tunings consistent with subdominant on or off.
   *  @param   subdominant
   *           true means tune consistent with subdominant,
   *           false means tune consistent with dominant
   */
  public synchronized void setSubdominant(boolean subdominant) 
    {
    this.subdominant = subdominant;
    //System.out.println("**** subdominant now " + subdominant);
    }

  /**
   *  Returns subdominant indicator.
   *  @result  true if tuning consistent with subdominant is in effect
   */
  public synchronized boolean getSubdominant() 
    {
    return this.subdominant;
    }

  /**
   *  Sets current key and tonic frequency.
   *  @param   key
   *           current key
   *  @param   tonic
   *           frequency of tonic of current key
   */
  public synchronized void setCurrentKey(Key key, double tonic)
    {
    this.currentKey = key;
    this.currentTonic = tonic;
    //System.out.println("**** current key now " + key);
    //System.out.println("**** current tonic frequency now " + tonic);
    System.out.println("Current tonic note is " + 
      key.tonic.name + key.tonic.accidental);
    System.out.println("Ratio of new tonic to original tonic = " +
      (tonic / this.originalTonic));
    }

  /**
   *  Returns current key.
   *  @result  current key
   */
  public synchronized Key getCurrentKey() {
    return this.currentKey;
  }

  /**
   *  Returns tonic note of current key.
   *  @result  tonic note
   */
  public synchronized Note getTonicNote() {
    return this.currentKey.tonic;
  }

  /**
   *  Returns current tonic frequency.
   *  @result  frequency of tonic of current key
   */
  public synchronized double getTonicFrequency() {
    return this.currentTonic;
  }

  /**
   *  Returns to the original key and tonic frequency.
   */
  public synchronized void revert() {
    this.currentKey = this.key;
    this.currentTonic = this.keyTonic;
  }

  /**
   *  Returns the note at the given degree of the current key.
   *  @param   degree  
   *           the degree
   *  @result  note at the given degree
   *  @throws  UnsupportedNoteException
   *           never
   */
  private Note degreeToNote(int degree)
      throws UnsupportedNoteException
    {
    // get Enumerate index of the note (Enumerate indices run
    // from 0 to 6, degrees from 1 to 7)
    Key currKey = getCurrentKey();
    int indexOfTonic = currKey.tonic.name.getOrdinal();
    int indexOfNote = (indexOfTonic + degree - 1) % DEGREES;

    // get the note's name
    Note.Name name = (Note.Name)currKey.tonic.name.get(indexOfNote);

    // determine the note's accidental
    Note.Accidental accidental = accidentals(currKey)[degree-1];

    // return note corresponding to the degree
    return new Note(name, accidental);
    }
    // Certain keys are not supported: e.g., keys whose tonic
    // has a double accidental.  Therefore certain modulations are 
    // not supported either, for instance those that would make the 
    // current key an unsupported key.
 
  /**
   *  Lowers a given note a half step.
   *  @param   note   
   *           the given note
   *  @result  the lowered note
   *  @throws  UnsupportedNoteException
   *           if the note already has a double flat
   */
  private Note lower(Note note)
      throws UnsupportedNoteException
    {
    Note.Accidental accidental = null;
    if (note.accidental == Note.Accidental.DOUBLE_SHARP)
      {
      accidental = Note.Accidental.SHARP;
      }
    else if (note.accidental == Note.Accidental.SHARP)
      {
      accidental = Note.Accidental.NATURAL;
      }
    else if (note.accidental == Note.Accidental.NATURAL)
      {
      accidental = Note.Accidental.FLAT;
      }
    else if (note.accidental == Note.Accidental.FLAT)
      {
      accidental = Note.Accidental.DOUBLE_FLAT;
      }
    else
      {
      throw new UnsupportedNoteException("Cannot lower " + note);
      }
    return new Note(note.name, accidental);
    }

  /**
   *  Raises a given note a half step.
   *  @param   note   
   *           the given note
   *  @result  the raised note
   *  @throws  UnsupportedNoteException
   *           if the note already has a double sharp
   */
  private Note raise(Note note)
      throws UnsupportedNoteException
    {
    Note.Accidental accidental = null;
    if (note.accidental == Note.Accidental.DOUBLE_SHARP)
      {
      throw new UnsupportedNoteException("Cannot raise " + note);
      }
    else if (note.accidental == Note.Accidental.SHARP)
      {
      accidental = Note.Accidental.DOUBLE_SHARP;
      }
    else if (note.accidental == Note.Accidental.NATURAL)
      {
      accidental = Note.Accidental.SHARP;
      }
    else if (note.accidental == Note.Accidental.FLAT)
      {
      accidental = Note.Accidental.NATURAL;
      }
    else if (note.accidental == Note.Accidental.DOUBLE_FLAT)
      {
      accidental = Note.Accidental.FLAT;
      }
    return new Note(note.name, accidental);
    }

  /**
   *  Returns the accidentals for a given key and scale type.
   *  @param   key    the given key
   *  @param   type   the given type (major or minor)
   *  @result  accidentals for given key and scale type
   *  @throws  UnsupportedNoteException
   *           if key is unsupported
   */
  private Note.Accidental[] accidentals(Key key)
      throws UnsupportedNoteException
    {
    Note.Accidental[] a = 
        (Note.Accidental[])Diatonic.accidentals.get(key);
    if (a == null)
      {
      throw new UnsupportedNoteException("Unsupported key: " + key); 
      }
    return a;
    }

  /**
   *  Returns the degree of a note in the current key.
   *  @param   note  the note 
   *  @result  degree of the given note
   */
  private int noteToDegree(Note note)
    {
    // indices run from 0 to 6, degrees from 1 to 7
    // (can deduce the degree without looking at the accidental)
    Key currKey = getCurrentKey();
    int indexOfNote = note.name.getOrdinal();
    int indexOfTonic = currKey.tonic.name.getOrdinal(); 
    int degree = 1 + ((indexOfNote + DEGREES - indexOfTonic) % DEGREES);
    return degree;
    }

  /**
   *  Installs the new tonic.
   *  @param   tonic           the new tonic note
   *  @param   tonicFrequency  frequency of the new tonic
   */
  private void setTonic(Note tonic, double tonicFrequency)
    {
    
    }

  /**
   *  Given the pitch of a note, returns the pitch of the note
   *  of the same name in octave 0 (the octave starting at
   *  concert A and going down).
   *  @param   pitch   the given pitch
   *  @result  pitch of "same" note in octave 0
   */
  private double octave0(double pitch)
    {
    final double margin = 8.0;
    while (pitch < MIDDLE_C + margin)
      {
      pitch += pitch;
      }
    while (pitch >= 2 * (MIDDLE_C - margin))
      {
      pitch /= 2;
      }
    return pitch;
    }

/***
  private double octave0(double pitch)
    {
    // use fixed point to avoid round-off problems
    final double SCALE = 1000;
    final int A = (int)(CONCERT_A * SCALE);
    int p = (int)(pitch * SCALE);
    while (p <= A / 2)
      {
      p+= p;
      }
    while (p > A)
      {
      p /= 2;
      }
    return ((double)p) / SCALE;
    }
***/
  }
