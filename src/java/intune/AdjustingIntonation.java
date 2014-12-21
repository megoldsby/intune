
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
 *  Interface <code>AdjustingIntonation</code> defines the interface that 
 *  must be satisfied by intonation types that adjust tunings according to
 *  the notes that are sounding (or have recently sounded).
 */
interface AdjustingIntonation extends Intonation
  {
  /**
   *  Tells the intonation that a note has begun sounding and
   *  returns notes whose pitch has changed as a result of
   *  adding the note.
   *  @param   note
   *           the note to be added
   *  @result  notes whose pitch is changed as a result
   *           of adding the note
   */
  Note[] addNote(Note note);

  /**
   *  Tells the intonation that a note is no longer sounding
   *  and returns notes whose pitch has been changed by
   *  removing the note.
   *  @param   note
   *           the note to be removed
   *  @result  notes whose pitch is changed as a result of
   *           removing the note
   */
  Note[] removeNote(Note note);

  /**
   *  Performs a modulation to a given degree of the current
   *  scale and returns notes whose pitch has changed as a
   *  result of the modulation.
   *  @param   degree
   *           degree of current scale to modulate to
   *  @param   modifier
   *           intonation-dependent modulation modifier
   *  @result  notes whose pitch has changed as a result
   *           of the modulation
   *  @throws  InvalidSyntaxException
   *           if the modifier syntax is invalid
   *  @throws  UnsupportedNoteException
   *           if an unsupported modulation is specified
   */
  Note[] modulate(int degree, String modifier) 
    throws InvalidSyntaxException,
           UnsupportedNoteException;
  }
