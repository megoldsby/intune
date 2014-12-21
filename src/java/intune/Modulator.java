
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
 *  Interface <code>Modulator</code> defines the interface that must
 *  be satisfied by intonation types that support modulation.
 */
interface Modulator extends Intonation
  {
  /**
   *  Effects a modulation to a new key.
   *  @param   note
   *           tonic note of the new key
   */
  void modulate(Note note); 

  /**
   *  Effects a modulation to a new key, forcing the tonic
   *  frequency to the given value..
   *  @param   note
   *           tonic note of the new key
   *  @param   tonic
   *           frequency of the tonic
   */
  void modulate(Note note, double tonic); 

  /**
   *  Begins or ends use of tunings consistent with the subdominant.
   *  @param   subdominant
   *           use tuning consistent with subdominant if true,
   *           use tuning consistent with dominant if false.
   */
  void setSubdominant(boolean subdominant);

  /**
   *  Returns the tonic note of the current key.
   *  @result  tonic 
   */
  Note getTonicNote();

  /**
   *  Returns to the original key and tonic frequency.
   */
  void revert();
  }
