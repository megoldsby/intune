
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
 *  Interface <code>Intonation</code> defines the interface that must
 *  be satisfied by specific intonation types.
 */
interface Intonation
  {
  /** frequency of A above middle C */
  static final double CONCERT_A = 440;

  /**
   *  Returns the frequency of a given note.
   *  @param   note   the note
   *  @result  frequency of the note
   *  @throws  UnsupportedNoteException
   *           if the frequency of the note cannot be computed
   *           (for instance, if it has too many accidentals)
   */
  double frequency(Note note) throws UnsupportedNoteException;

  /**
   *  Returns the note corresponding to a MIDI note number.
   *  @param   noteNumber   the MIDI note number
   *  @result  the corresponding note
   *  @throws  UnsupportedNoteException
   *           if there is no such note (for instance,
   *           if the note would have too many accidentals)
   */
  Note midiNoteNumberToNote(int noteNumber) throws UnsupportedNoteException;
  }
