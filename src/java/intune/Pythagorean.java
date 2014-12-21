
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
 *  Class <code>Pythagorean</code> implements Pythagorean intonation.
 */
class Pythagorean implements Intonation
  {
  /** frequency of middle C */
  static final double FIFTH = 3.0/2.0;
  static final double MIDDLE_C = 
    2 * Intonation.CONCERT_A / (FIFTH * FIFTH * FIFTH);

  /** midi note number for middle C */
  static final int MIDI_MIDDLE_C = 60;

  /** number of midi notes */
  static final int MIDI_NOTE_LIMIT = 128;

  /** number of MIDI notes per octave */
  static final int MIDI_NOTES_PER_OCTAVE = 12;

  /**
   *  the tonic ("final") note of the mode
   */
  Note tonic;

  /**
   *  frequency vs. Note for notes in octave 0.
   *  @see Note
   */
  Map pitch0 = new HashMap(); 

  /**
   *  Constructor.
   */
  Pythagorean(Note tonic)
    {
    this.tonic = tonic;

    /** set up all pitches in octave 0 */

    // c
    double pitch = MIDDLE_C;
    this.pitch0.put(new Note(Note.Name.C, Note.Accidental.NATURAL, 0), 
                    new Double(pitch));
    // g
    pitch = octave0(pitch * FIFTH);
    this.pitch0.put(new Note(Note.Name.G, Note.Accidental.NATURAL, 0), 
                    new Double(pitch));
    // d
    pitch = octave0(pitch * FIFTH);
    this.pitch0.put(new Note(Note.Name.D, Note.Accidental.NATURAL, 0), 
                    new Double(pitch));
    // a
    pitch = octave0(pitch * FIFTH);
    this.pitch0.put(new Note(Note.Name.A, Note.Accidental.NATURAL, 0), 
                    new Double(pitch));
    // e
    pitch = octave0(pitch * FIFTH);
    this.pitch0.put(new Note(Note.Name.E, Note.Accidental.NATURAL, 0), 
                    new Double(pitch));
    // b
    pitch = octave0(pitch * FIFTH);
    this.pitch0.put(new Note(Note.Name.B, Note.Accidental.NATURAL, 0), 
                    new Double(pitch));
    // f sharp
    pitch = octave0(pitch * FIFTH);
    this.pitch0.put(new Note(Note.Name.F, Note.Accidental.SHARP, 0), 
                    new Double(pitch));
    // c sharp
    pitch = octave0(pitch * FIFTH);
    this.pitch0.put(new Note(Note.Name.C, Note.Accidental.SHARP, 0), 
                    new Double(pitch));
    // g sharp
    pitch = octave0(pitch * FIFTH);
    this.pitch0.put(new Note(Note.Name.G, Note.Accidental.SHARP, 0), 
                    new Double(pitch));
    // d sharp
    pitch = octave0(pitch * FIFTH);
    this.pitch0.put(new Note(Note.Name.D, Note.Accidental.SHARP, 0), 
                    new Double(pitch));
    // a sharp
    pitch = octave0(pitch * FIFTH);
    this.pitch0.put(new Note(Note.Name.A, Note.Accidental.SHARP, 0), 
                    new Double(pitch));
    // e sharp
    pitch = octave0(pitch * FIFTH);
    this.pitch0.put(new Note(Note.Name.E, Note.Accidental.SHARP, 0), 
                    new Double(pitch));
    // b sharp
    pitch = octave0(pitch * FIFTH);
    this.pitch0.put(new Note(Note.Name.B, Note.Accidental.SHARP, 0), 
                    new Double(pitch));
    // f double sharp
    pitch = octave0(pitch * FIFTH);
    this.pitch0.put(new Note(Note.Name.F, Note.Accidental.DOUBLE_SHARP, 0), 
                    new Double(pitch));
    // c double sharp
    pitch = octave0(pitch * FIFTH);
    this.pitch0.put(new Note(Note.Name.C, Note.Accidental.DOUBLE_SHARP, 0), 
                    new Double(pitch));
    // g double sharp
    pitch = octave0(pitch * FIFTH);
    this.pitch0.put(new Note(Note.Name.G, Note.Accidental.DOUBLE_SHARP, 0), 
                    new Double(pitch));
    // d double sharp
    pitch = octave0(pitch * FIFTH);
    this.pitch0.put(new Note(Note.Name.D, Note.Accidental.DOUBLE_SHARP, 0), 
                    new Double(pitch));
    // a double sharp
    pitch = octave0(pitch * FIFTH);
    this.pitch0.put(new Note(Note.Name.A, Note.Accidental.DOUBLE_SHARP, 0), 
                    new Double(pitch));
    // e double sharp
    pitch = octave0(pitch * FIFTH);
    this.pitch0.put(new Note(Note.Name.E, Note.Accidental.DOUBLE_SHARP, 0), 
                    new Double(pitch));
    // b double sharp
    pitch = octave0(pitch * FIFTH);
    this.pitch0.put(new Note(Note.Name.B, Note.Accidental.DOUBLE_SHARP, 0), 
                    new Double(pitch));
    // f
    pitch = octave0(MIDDLE_C / FIFTH);
    this.pitch0.put(new Note(Note.Name.F, Note.Accidental.NATURAL, 0), 
                    new Double(pitch));
    // b flat 
    pitch = octave0(pitch/FIFTH);
    this.pitch0.put(new Note(Note.Name.B, Note.Accidental.FLAT, 0), 
                    new Double(pitch));

    // e flat 
    pitch = octave0(pitch/FIFTH);
    this.pitch0.put(new Note(Note.Name.E, Note.Accidental.FLAT, 0), 
                    new Double(pitch));

    // a flat 
    pitch = octave0(pitch/FIFTH);
    this.pitch0.put(new Note(Note.Name.A, Note.Accidental.FLAT, 0), 
                    new Double(pitch));

    // d flat 
    pitch = octave0(pitch/FIFTH);
    this.pitch0.put(new Note(Note.Name.D, Note.Accidental.FLAT, 0), 
                    new Double(pitch));

    // g flat 
    pitch = octave0(pitch/FIFTH);
    this.pitch0.put(new Note(Note.Name.G, Note.Accidental.FLAT, 0), 
                    new Double(pitch));

    // c flat 
    pitch = octave0(pitch/FIFTH);
    this.pitch0.put(new Note(Note.Name.C, Note.Accidental.FLAT, 0), 
                    new Double(pitch));

    // f flat 
    pitch = octave0(pitch/FIFTH);
    this.pitch0.put(new Note(Note.Name.F, Note.Accidental.FLAT, 0), 
                    new Double(pitch));

    // b double flat 
    pitch = octave0(pitch/FIFTH);
    this.pitch0.put(new Note(Note.Name.B, Note.Accidental.DOUBLE_FLAT, 0), 
                    new Double(pitch));

    // e double flat 
    pitch = octave0(pitch/FIFTH);
    this.pitch0.put(new Note(Note.Name.E, Note.Accidental.DOUBLE_FLAT, 0), 
                    new Double(pitch));

    // a double flat 
    pitch = octave0(pitch/FIFTH);
    this.pitch0.put(new Note(Note.Name.A, Note.Accidental.DOUBLE_FLAT, 0), 
                    new Double(pitch));

    // d double flat 
    pitch = octave0(pitch/FIFTH);
    this.pitch0.put(new Note(Note.Name.D, Note.Accidental.DOUBLE_FLAT, 0), 
                    new Double(pitch));

    // g double flat 
    pitch = octave0(pitch/FIFTH);
    this.pitch0.put(new Note(Note.Name.G, Note.Accidental.DOUBLE_FLAT, 0), 
                    new Double(pitch));

    // c double flat 
    pitch = octave0(pitch/FIFTH);
    this.pitch0.put(new Note(Note.Name.C, Note.Accidental.DOUBLE_FLAT, 0), 
                    new Double(pitch));

    // f double flat 
    pitch = octave0(pitch/FIFTH);
    this.pitch0.put(new Note(Note.Name.F, Note.Accidental.DOUBLE_FLAT, 0), 
                    new Double(pitch));
    }

  /**
   *  Constructor for use when don't care about the tonic.
   */
  Pythagorean()
    {
    this(new Note(Note.Name.C, Note.Accidental.NATURAL));
    }

  /**
   *  Given the pitch of a note, returns the same of the note
   *  of the same name in octave 0 (the octave starting at
   *  middle C and going up).
   *  @param   pitch   the given pitch
   *  @result  pitch of "same" note in octave 0
   */
  private double octave0(double pitch)
    {
    while (pitch < MIDDLE_C)
      {
      pitch += pitch;
      }
    while (pitch >= 2 * MIDDLE_C)
      {
      pitch /= 2;
      }
    return pitch;
    }

  /**
   *  Returns frequency of a given note.
   *  @param   note   the given note
   *  @result  frequency of the given note
   *  @throws  UnsupportedNoteException
   *           if the note has greater than double accidentals
   *  @see Intonation
   */
  public double frequency(Note note) throws UnsupportedNoteException
    {
    Double notePitch = 
      (Double)this.pitch0.get(new Note(note.name, note.accidental, 0));
    System.out.println("***** Pythagorean base pitch " + notePitch);
    if (notePitch == null)
      {
      throw new UnsupportedNoteException(note.toString());
      }
    double pitch = notePitch.doubleValue();
    int octave = note.octave;
    System.out.println("*****      octave " + octave);
    while (octave > 0)
      {
      pitch += pitch;
      octave--;
      } 
    while (octave < 0)
      {
      pitch /= 2;
      octave++;
      } 
    return pitch;
    }

  /**
   *  Returns <code>Note</code> corresponding to a given MIDI note number.
   *  @param  noteNumber
   *          the MIDI note number
   *  @result the corresponding <code>Note</code>
   *  @throws UnsupportedNoteException
   *          if note is not in the MIDI range
   */
  public synchronized Note midiNoteNumberToNote(int noteNumber)
      throws UnsupportedNoteException
    {
    final int MIDI_C_ABOVE_MIDDLE_C = MIDI_MIDDLE_C + 12;
    final int OCTAVE = 12;
    final Note.Name NOTE_NAME = Note.Name.A;

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

    // discover number of MIDI notes from tonic to given note
    int numberOfTonic = noteToMidiNoteNumber(this.tonic);
    int difference = noteNumber - numberOfTonic;
    while (difference < 0)
      {
      difference += MIDI_NOTES_PER_OCTAVE;
      }
    System.out.println("Note number, tonic number, diff = " +
      noteNumber + " " + numberOfTonic + " " + difference);

    // develop Enumerate Note.Name index of note's name 
    // and use it to recover the name
    int indexOfTonic = this.tonic.name.getOrdinal();
    int indexOfNote = (indexOfTonic + difference) % NOTE_NAME.getSize();
    Note.Name name = (Note.Name)NOTE_NAME.get(indexOfNote);

    // form the note
    Note note = new Note(name, this.tonic.accidental, octave);
    switch (difference)
      {
      case 1:
      case 3:
      case 6:
      case 8:
      case 10:
        note = lower(note);
        break;
 
      default:
        break;
      }
    System.out.println("Developed note = " + note);

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

  }
/** end of class Pythagorean */
