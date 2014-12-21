
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
 *  Class <code>ThomasYoung1799</code> implements a well temperament
 *  devised by Thomas Young and publish in 1799.  See
 *  http://www.math.uwaterloo.ca/~mrubinst/tuning/tuning.html.
 */
class ThomasYoung1799 implements Intonation
  {
  /**
   *  MIDI note number for middle C, MIDI note number limit
   */
  static final int MIDI_MIDDLE_C = 60;
  static final int MIDI_NOTE_LIMIT = 128;

  /**
   *  ratio of keyboard notes to C in Thomas Young's tuning
   */
  private static final double C       = 1.0;
  private static final double C_SHARP = 1.055730636;
  private static final double D       = 1.119771437;
  private static final double E_FLAT  = 1.187696971;
  private static final double E       = 1.253888072;
  private static final double F       = 1.334745462;
  private static final double F_SHARP = 1.407640848;
  private static final double G       = 1.496510232;
  private static final double A_FLAT  = 1.583595961;
  private static final double A       = 1.675749414;
  private static final double B_FLAT  = 1.781545449;
  private static final double B       = 1.878842233;

  /** pitch of middle C in this temperament */
  private static final double MIDDLE_C = Intonation.CONCERT_A * C / A;

  /**
   *  frequency vs. Note for notes in octave 0.
   *  @see Note
   */
  private Map pitch0 = new HashMap();

  /**
   *  Constructor.
   */
  ThomasYoung1799()
    {
    /** set up all notes in octave 0 */

    // a = g double sharp = b double flat
    double pitch = Intonation.CONCERT_A;
    Double notePitch = new Double(pitch);
    this.pitch0.put(
      new Note(Note.Name.A, Note.Accidental.NATURAL, 0), notePitch);
    this.pitch0.put(new Note(
      Note.Name.G, Note.Accidental.DOUBLE_SHARP, 0), notePitch);
    this.pitch0.put(new Note(
      Note.Name.B, Note.Accidental.DOUBLE_FLAT, 0), notePitch);

    // b flat = a sharp = c double flat
    pitch = octave0(Intonation.CONCERT_A * B_FLAT / A);
    notePitch = new Double(pitch);
    this.pitch0.put(
        new Note(Note.Name.B, Note.Accidental.FLAT, 0), notePitch);
    this.pitch0.put(
        new Note(Note.Name.A, Note.Accidental.SHARP, 0), notePitch);
    this.pitch0.put(
        new Note(Note.Name.C, Note.Accidental.DOUBLE_FLAT, 0), notePitch);
    
    // b = c flat = a double sharp
    pitch = octave0(Intonation.CONCERT_A * B / A);
    notePitch = new Double(pitch);
    this.pitch0.put(
        new Note(Note.Name.B, Note.Accidental.NATURAL, 0), notePitch);
    this.pitch0.put(
        new Note(Note.Name.C, Note.Accidental.FLAT, 0), notePitch);
    this.pitch0.put(
        new Note(Note.Name.A, Note.Accidental.DOUBLE_SHARP, 0), notePitch);
    
    // c = b sharp = d double flat
    pitch = octave0(Intonation.CONCERT_A * C / A);
    notePitch = new Double(pitch);
    this.pitch0.put(
        new Note(Note.Name.C, Note.Accidental.NATURAL, 0), notePitch);
    this.pitch0.put(
        new Note(Note.Name.B, Note.Accidental.SHARP, 0), notePitch);
    this.pitch0.put(
        new Note(Note.Name.D, Note.Accidental.DOUBLE_FLAT, 0), notePitch);
    
    // c sharp = d flat = b double sharp
    pitch = octave0(Intonation.CONCERT_A * C_SHARP / A);
    notePitch = new Double(pitch);
    this.pitch0.put(
        new Note(Note.Name.C, Note.Accidental.SHARP, 0), notePitch);
    this.pitch0.put(
        new Note(Note.Name.D, Note.Accidental.FLAT, 0), notePitch);
    this.pitch0.put(
        new Note(Note.Name.B, Note.Accidental.DOUBLE_SHARP,0), notePitch);
    
    // d = c double sharp = e double flat
    pitch = octave0(Intonation.CONCERT_A * D / A);
    notePitch = new Double(pitch);
    this.pitch0.put(
        new Note(Note.Name.D, Note.Accidental.NATURAL, 0), notePitch);
    this.pitch0.put(
        new Note(Note.Name.C, Note.Accidental.DOUBLE_SHARP, 0), notePitch);
    this.pitch0.put(
        new Note(Note.Name.E, Note.Accidental.DOUBLE_FLAT, 0), notePitch);
    
    // e flat = d sharp
    pitch = octave0(Intonation.CONCERT_A * E_FLAT / A);
    notePitch = new Double(pitch);
    this.pitch0.put(
        new Note(Note.Name.E, Note.Accidental.FLAT, 0), notePitch);
    this.pitch0.put(
        new Note(Note.Name.D, Note.Accidental.SHARP, 0), notePitch);
    
    // e = f flat = d double sharp
    pitch = octave0(Intonation.CONCERT_A * E / A);
    notePitch = new Double(pitch);
    this.pitch0.put(
        new Note(Note.Name.E, Note.Accidental.NATURAL, 0), notePitch);
    this.pitch0.put(
        new Note(Note.Name.F, Note.Accidental.FLAT, 0), notePitch);
    this.pitch0.put(
        new Note(Note.Name.D, Note.Accidental.DOUBLE_SHARP, 0), notePitch);
    
    // f = e sharp = g double flat
    pitch = octave0(Intonation.CONCERT_A * F / A);
    notePitch = new Double(pitch);
    this.pitch0.put(
         new Note(Note.Name.F, Note.Accidental.NATURAL, 0), notePitch);
    this.pitch0.put(
         new Note(Note.Name.E, Note.Accidental.SHARP, 0), notePitch);
    this.pitch0.put(
         new Note(Note.Name.G, Note.Accidental.DOUBLE_FLAT, 0), notePitch);
    
    // f sharp = g flat = e double sharp
    pitch = octave0(Intonation.CONCERT_A * F_SHARP / A);
    notePitch = new Double(pitch);
    this.pitch0.put(
        new Note(Note.Name.F, Note.Accidental.SHARP, 0), notePitch);
    this.pitch0.put(
        new Note(Note.Name.G, Note.Accidental.FLAT, 0), notePitch);
    this.pitch0.put(
        new Note(Note.Name.E, Note.Accidental.DOUBLE_SHARP, 0), notePitch);
    
    // g = a double flat = f double sharp
    pitch = octave0(Intonation.CONCERT_A * G / A);
    notePitch = new Double(pitch);
    this.pitch0.put(
        new Note(Note.Name.G, Note.Accidental.NATURAL, 0), notePitch);
    this.pitch0.put(
        new Note(Note.Name.A, Note.Accidental.DOUBLE_FLAT, 0), notePitch);
    this.pitch0.put(
        new Note(Note.Name.F, Note.Accidental.DOUBLE_SHARP, 0), notePitch);
    
    // g sharp = a flat
    pitch = octave0(Intonation.CONCERT_A * A_FLAT / A);
    notePitch = new Double(pitch);
    this.pitch0.put(
        new Note(Note.Name.G, Note.Accidental.SHARP, 0), notePitch);
    this.pitch0.put(
        new Note(Note.Name.A, Note.Accidental.FLAT, 0), notePitch);
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
   *           if note has greater than double accidentals
   *  @see Intonation
   */
  public double frequency(Note note)
      throws UnsupportedNoteException
    {
    Double notePitch = 
      (Double)this.pitch0.get(new Note(note.name, note.accidental, 0));
    if (notePitch == null)
      {
      throw new UnsupportedNoteException(note.toString());
      }
    double pitch = notePitch.doubleValue();
    int octave = note.octave;
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
    //System.out.println(note + " " + pitch);
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
  
// The note depends on the key: for instance, C+6 could be f# or g flat.

      // discover note name and accidental and create note
      Note note = null;
      switch (noteNumber)
        {
        case MIDI_MIDDLE_C:
          note = new Note(Note.Name.C, Note.Accidental.NATURAL, octave);
          break;
  
        case MIDI_MIDDLE_C+1:
          note = new Note(Note.Name.C, Note.Accidental.SHARP, octave);
          break;
  
        case MIDI_MIDDLE_C+2:
          note = new Note(Note.Name.D, Note.Accidental.NATURAL, octave);
          break;
  
        case MIDI_MIDDLE_C+3:
          note = new Note(Note.Name.E, Note.Accidental.FLAT, octave);
          break;
  
        case MIDI_MIDDLE_C+4:
          note = new Note(Note.Name.E, Note.Accidental.NATURAL, octave);
          break;
  
        case MIDI_MIDDLE_C+5:
          note = new Note(Note.Name.F, Note.Accidental.NATURAL, octave);
          break;
  
        case MIDI_MIDDLE_C+6:
          note = new Note(Note.Name.F, Note.Accidental.SHARP, octave);
          break;
  
        case MIDI_MIDDLE_C+7:
          note = new Note(Note.Name.G, Note.Accidental.NATURAL, octave);
          break;
  
        case MIDI_MIDDLE_C+8:
          note = new Note(Note.Name.G, Note.Accidental.SHARP, octave);
          break;
  
        case MIDI_MIDDLE_C+9:
          note = new Note(Note.Name.A, Note.Accidental.NATURAL, octave);
          break;
  
        case MIDI_MIDDLE_C+10:
          note = new Note(Note.Name.B, Note.Accidental.FLAT, octave);
          break;
  
        case MIDI_MIDDLE_C+11:
          note = new Note(Note.Name.B, Note.Accidental.NATURAL, octave);
          break;
  
        default:
          System.err.println("Should not happen");
          new Exception().printStackTrace();
          System.exit(-1);
        }
  
      // return the note
      return note;
      }
  }
/** end of class ThomasYoungg1799 */
