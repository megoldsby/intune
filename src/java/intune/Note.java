
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

/**
 *  Class <code>Note</code> represents a note.
 */
class Note
  {
  /**
   *  names of the notes
   */
  static class Name extends Enumerate
    {
    static final Name A = new Name("a");
    static final Name B = new Name("b");
    static final Name C = new Name("c");
    static final Name D = new Name("d");
    static final Name E = new Name("e");
    static final Name F = new Name("f");
    static final Name G = new Name("g");
    private Name(String name) { super(name); }
    }

  /**
   *  accidentals
   */
  static class Accidental extends Enumerate
    {
    static final Accidental NATURAL      = new Accidental("natural");
    static final Accidental SHARP        = new Accidental("sharp");
    static final Accidental DOUBLE_SHARP = new Accidental("double sharp");
    static final Accidental FLAT         = new Accidental("flat");
    static final Accidental DOUBLE_FLAT  = new Accidental("double flat");
    private Accidental(String name) { super(name); }
    }

  /**
   *  note types (duration types)
   */
  static class Type extends Enumerate
    {
    static final Type BREVE                     = new Type("breve");
    static final Type WHOLE                     = new Type("whole");
    static final Type HALF                      = new Type("half");
    static final Type HALF_TRIPLET              = new Type("half triplet");
    static final Type QUARTER                   = new Type("quarter");
    static final Type QUARTER_TRIPLET           = new Type("quarter triplet");
    static final Type EIGHTH                    = new Type("8th");
    static final Type EIGHTH_TRIPLET            = new Type("8th triplet");
    static final Type SIXTEENTH                 = new Type("16th");
    static final Type SIXTEENTH_TRIPLET         = new Type("16th triplet");
    static final Type THIRTY_SECOND             = new Type("32nd");
    static final Type THIRTY_SECOND_TRIPLET     = new Type("32nd triplet");
    static final Type SIXTY_FOURTH              = new Type("64th");
    static final Type SIXTY_FOURTH_TRIPLET      = new Type("64th triplet");
    static final Type HUNDRED_AND_TWENTY_EIGHTH = new Type("128th");
    static final Type HUNDRED_AND_TWENTY_EIGHTH_TRIPLET 
                                                = new Type("128th triplet");
    private Type(String name) { super(name); }
    }
  
  /**
   *  pitch modifiers
   */
  static class PitchModifier extends Enumerate
    {
    static final PitchModifier NONE = new PitchModifier("none");
    static final PitchModifier LOWERED_2ND = new PitchModifier("lowered 2nd");
    static final PitchModifier LOWERED_7TH = new PitchModifier("lowered 7th");
    static final PitchModifier SEPTIMAL_7TH = new PitchModifier("dominant 7th");

    private PitchModifier(String name) { super(name); }
    }

  /**
   *  intervals 
   */
  static class Interval extends Enumerate
    {
    static final Interval UNISON        = new Interval("unison");
    static final Interval MINOR_SECOND  = new Interval("minor second");
    static final Interval MAJOR_SECOND  = new Interval("major second");
    static final Interval MINOR_THIRD   = new Interval("minor third");
    static final Interval MAJOR_THIRD   = new Interval("major third");
    static final Interval FOURTH        = new Interval("fourth");
    static final Interval FIFTH         = new Interval("fifth");
    static final Interval MINOR_SIXTH   = new Interval("minor sixth");
    static final Interval MAJOR_SIXTH   = new Interval("major sixth");
    static final Interval MINOR_SEVENTH = new Interval("minor seventh"); 
    static final Interval MAJOR_SEVENTH = new Interval("major seventh");
    static final Interval OCTAVE        = new Interval("octave");
    private Interval(String name) { super(name); }
    }

  /**  note name */
  Name name;

  /** accidental */
  Accidental accidental;

  /**
   * octave:
   *  ...
   * -2 for CC-BB
   * -1 for C-B
   *  0 for c-b
   *  1 for cc-bb
   *  2 for ccc-bbb
   *  ...
   */
  int octave;

  /**
   * type of note: a **kern note type number:
   *   0:    breve
   *   1:    whole
   *   2:    half
   *   3:    half triplet
   *   4:    quarter
   *   6:    quarter triplet
   *   8:    eighth
   *   12:   eighth triplet
   *   16:   sixteenth 
   *   24:   sixteenth triplet
   *   32:   thirty-second 
   *   48:   thirty-second triplet
   *   64:   sixty-fourth
   *   92:   sixty-fourth triplet
   *   128:  hundred-and-twenty-eighth 
   *   192:  hundred-and-twenty-eighth triplet
   */
  int type;

  /**
   *  dots modifying the duration
   */
  int dots;

  /**
   *  diatonic pitch modifier
   */
  PitchModifier modifier;

  /**
   *  Returns <code>Accidental</code> corresponding to 
   *  given accidental number.
   *  @param   accidentalNumber  accidental number: > 0 for sharps, 
   *                             < 0 for flats, 0 for natural
   *  @result  the corresponding <code>Accidental</code> object
   */
  public static Accidental accidental(int accidentalNumber)
    {
    Accidental accidental = null;
    switch(accidentalNumber)
      {
      case 2:
        accidental = Accidental.DOUBLE_SHARP;
        break;
      case 1:
        accidental = Accidental.SHARP;
        break;
      case 0:
        accidental = Accidental.NATURAL;
        break;
      case -1:
        accidental = Accidental.FLAT;
        break;
      case -2:
        accidental = Accidental.DOUBLE_FLAT;
        break;
      default:
        throw new IllegalArgumentException("Unsupported accidental");
      } 
    return accidental;
    }

  /**
   *  Returns the duration of a note given a note type and a dot count.
   *  @param   type   note type, a kern duration number
   *  @param   dots   number of dots (if any)
   *  @result  duration in fractions of a whole note
   */
  public static double duration(int type, int dots)
    {
    // the duration of the type is the inverse of its kern number
    double duration = (1.0 / (double)type);

    // now add in the effects of the dots, if any
    double denom = 1;
    for (int i = 0; i < dots; i++)
      {
      denom += denom;
      } 
    duration = duration * (2.0 - (1.0 / denom));

    // return the total duration (number of wholes)
    return duration;
    
    }

  /**
   *  Constructor used when duration of note is not important
   *  and note lies between middle C (inclusive) and the C above it.
   *  @param   name          note name (Note.Name.A, Note.Name.B, etc.)
   *  @param   accidental    accidental (Note.Accidental.NATURAL, e.g.)
   */
  Note(Name name, Accidental accidental)
    {
    this(name, accidental, 0, 0, 0, PitchModifier.NONE);
    }

  /**
   *  Constructor used when duration of note is not important.
   *  @param   name          note name (Note.Name.A, Note.Name.B, etc.)
   *  @param   accidental    accidental (Note.Accidental.NATURAL, e.g.)
   *  @param   octave        0 for c-b, 1 for cc-bb, etc., -1 for C-B,
   *                         -2 for CC-BB etc.
   */
  Note(Name name, Accidental accidental, int octave)
    {
    this(name, accidental, octave, 0, 0, PitchModifier.NONE);
    }

  /**
   *  Constructor used when there is no accidental.
   *  @param   name          note name (Note.a, Note.b, etc.)
   *  @param   octave        0 for c-b, 1 for cc-bb, etc., -1 for C-B,
   *                         -2 for CC-BB etc.
   *  @param   type          note type, a kern duration number
   *  @param   dots          number of dots modifying note duration
   */
  Note(Name name, int octave, int type, int dots)
    {
    this(name, Accidental.NATURAL, octave, type, dots, PitchModifier.NONE);
    }

  /**
   *  Constructor used when there are no accidental and no dots.
   *  @param   name          note name (Note.a, Note.b, etc.)
   *  @param   octave        0 for c-b, 1 for cc-bb, etc., -1 for C-B,
   *                         -2 for CC-BB etc.
   *  @param   type          note type, a kern duration number
   */
  Note(Name name, int octave, int type)
    {
    this(name, Accidental.NATURAL, octave, type, 0, PitchModifier.NONE);
    }

  /**
   *  Constructor with all possible arguments except pitch modifier.
   *  @param   name          note name
   *  @param   accidental    note accidental
   *  @param   octave        0 for c-b, 1 for cc-bb, etc., -1 for C-B,
   *                         -2 for CC-BB etc.
   *  @param   type          note type, a kern duration number
   *  @param   dots          number of dots modifying note duration
   *  @throws  IllegalArgumentException
   *           if any argument is invalid
   */ 
  Note(Name name, Accidental accidental, int octave, int type, int dots) 
    { 
    this(name, accidental, octave, type, dots, PitchModifier.NONE);
    } 

  /**
   *  Constructor with all possible arguments.
   *  @param   name          note name
   *  @param   accidental    note accidental
   *  @param   octave        0 for c-b, 1 for cc-bb, etc., -1 for C-B,
   *                         -2 for CC-BB etc.
   *  @param   type          note type, a kern duration number
   *  @param   dots          number of dots modifying note duration
   *  @param   modifier      pitch modifier
   *  @throws  IllegalArgumentException
   *           if any argument is invalid
   */ 
  Note(Name name, 
       Accidental accidental, 
       int octave, 
       int type, 
       int dots,
       PitchModifier modifier)
    { 
    this.name = name; 
    this.accidental = accidental; 
    this.octave = octave; 
    this.type = type; 
    this.dots = dots; 
    this.modifier = modifier;
    checkArguments(); 
    } 

  /**
   *  Returns the duration of this note, in fractions of a whole note.
   *  @result  the number of whole notes equal to the duration of this note
   */
  public double duration()
    {
    return Note.duration(this.type, this.dots);
    }

  /** 
   *  Verifies that the constructor arguments are valid.
   *  @throws   IllegalArgumentException  if they are not
   */ 
  private void checkArguments()
    {
    if (this.name == null || this.accidental == null)
      {
      throw new IllegalArgumentException("Null Note constructor argument");
      }
    if (!validDurationNumber(this.type))
      {
      throw new IllegalArgumentException("Note type (duration) invalid");
      }
    if (this.dots < 0)
      {
      throw new IllegalArgumentException("Note dot count negative");
      }
    }

  /**
   *  Returns true if given note duration number is valid.
   *  @param   n  the duration number
   *  @result  true if given number is not valid kern duration
   */
  private boolean validDurationNumber(int n)
    {
    return (n == 0
            || n == 1            
            || n == 2
            || n == 3
            || n == 4
            || n == 6
            || n == 8
            || n == 12
            || n == 16
            || n == 24
            || n == 32
            || n == 48
            || n == 64
            || n == 96
            || n == 128
            || n == 192);

/**
    final int DURATION_LIMIT = 128;
    boolean valid = false;
    if (n == 0)
      {
      valid = true;
      }
    else
      {
      int value = 1;
      while (value <= DURATION_LIMIT
             && n != value
             && n != value + (value / 2))
        {
        value += value;
        }
      valid = (value <= DURATION_LIMIT);
      }
    return valid;
**/
    }

  /**
   *  Returns a string representing the type of this note.
   *  @result  string note type
   */
  private String typeToString()
    {
/**
    String s = 
      (this.type == 0   ? "breve"           :
      (this.type == 1   ? "whole"           :
      (this.type == 2   ? "half"            :
      (this.type == 3   ? "half triplet"    :
      (this.type == 4   ? "quarter"         :
      (this.type == 6   ? "quarter triplet" :
      (this.type == 8   ? "eighth"          :
      (this.type == 12  ? "eighth triplet"  :
      (this.type == 16  ? "16th"            :
      (this.type == 24  ? "16th triplet"    :
      (this.type == 32  ? "32nd"            :
      (this.type == 48  ? "32nd triplet"    :
      (this.type == 64  ? "64th"            :
      (this.type == 92  ? "64th triplet"    : 
      (this.type == 128 ? "128th"           :
                          "128th triplet"     )))))))))))))));
    return s;
**/
    int ordinal = 0;
    if (this.type <= 1)
      {
      ordinal = this.type;
      }
    else
      {
      int n = this.type;
      while (n != 1)
        {
        ordinal += (2 + (n & 1));
        n /= 2;
        }
      }
    return Type.WHOLE.get(ordinal).getName();
    }

  public boolean equals(Object that)
    {
    return (this == that
            || (that instanceof Note
                && this.name == ((Note)that).name
                && this.accidental == ((Note)that).accidental
                && this.octave == ((Note)that).octave
                && this.type == ((Note)that).type
                && this.dots == ((Note)that).dots));
    }

  public int hashCode()
    {
    return this.name.hashCode() 
           ^ this.accidental.hashCode() 
           ^ this.octave
           ^ this.type
           ^ this.dots;
    }

  /**
   *  Returns string representation of a note.
   *  @result  note in string form
   */
  public String toString()
    {
    StringBuffer buf = new StringBuffer();
    buf.append(this.name.getName());
    buf.append(" " +this.accidental.getName());
    buf.append(" " + this.octave);
    buf.append(" " + typeToString());
    for (int i = 0; i < this.dots; i++)
      {
      buf.append(" dot");
      }
    return buf.toString();
    }
  }
