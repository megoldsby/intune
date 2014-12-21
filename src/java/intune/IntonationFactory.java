
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

public class IntonationFactory 
  {

  /**
   *  Creates and returns an <tt>Intonation</tt> object given
   *  an intonatin name and a key name.
   *  @param   intonationName
   *           the name of the intonation type
   *  @param   keyName
   *           the name of the starting key
   *  @result  the intonation object
   */
  public static Intonation create(String intonationName, String keyName) {

    // create an intonation object of the specified type
    Intonation intonation = null;
    String name = intonationName.toLowerCase();
    if (Constants.IntonationName.DIATONIC.startsWith(name)) 
      {
      Note key = keyNameToNote(keyName);
      System.out.println("Note " + key + " for keyName " + keyName);
      Diatonic.ScaleType type = (Character.isUpperCase(keyName.charAt(0)) ?
                                   Diatonic.ScaleType.MAJOR :		      
                                   Diatonic.ScaleType.MINOR);      
      System.out.println("Scale type " + type);
      try
        {
        intonation = new Diatonic(new Diatonic.Key(key, type), 
                       new Pythagorean().frequency(key)); 
        }
      catch(UnsupportedNoteException e)
        {
        throw new IllegalArgumentException(
          "Cannot build Diatonic Intonation with given key name");
        }
      }
    else if (Constants.IntonationName.PYTHAGOREAN.startsWith(name))
      {
      if (keyName != null)
        {
        Note key = keyNameToNote(keyName);
        intonation = new Pythagorean(key);
        }
      else
        {
        intonation = new Pythagorean();
        }
      }
    else if (Constants.IntonationName.EQUAL_TEMPERAMENT.startsWith(name))
      {
      intonation = new EqualTemperament();
      }
    else
      {
      // must be the class name of a temperament
      String className = intonationName;
      Class intonationClass = null;
      try 
        {
        intonationClass = Class.forName(className);
        }
      catch(ClassNotFoundException e)
        {
        System.err.println("Cannot find intonation class " + className);
        System.exit(-1);
        }
      try
        {
        intonation = (Intonation)intonationClass.newInstance();
        }
      catch(ClassCastException e)
        {
        System.err.println(className + " is not an Intonation class");
        System.exit(-1);
        }
      catch(Exception e)
        {
        System.err.println("Instantiating intonation class: " + e);
        System.exit(-1);
        }
      }

    // return the intonation
    return intonation;
    }

  /**
   *  Returns note corresponding to given key name.
   *  @param   name
   *           ('A'..'G''a'..'g')['#'|'-']
   *  @result  note for given 
   *  @throws  IllegalArgumentException
   *           if it's not a valid note name
   */
  private static Note keyNameToNote(String name)
    {
    Note note = null;

      // get accidental if any
      Note.Accidental accidental = null;
      if (name.length() == 1)
      {
      accidental = Note.Accidental.NATURAL;
      }
    else if (name.length() == 2)
      {
      char sharpFlat = name.charAt(1);
      accidental = (sharpFlat == '#' ? Note.Accidental.SHARP :
                   (sharpFlat == '-' ? Note.Accidental.FLAT  : accidental));
      }
    if (accidental == null)
      {
      throw new IllegalArgumentException("Invalid key name: " + name);
      }

    // build note from key letter and accidental
    char c = Character.toLowerCase(name.charAt(0));
    switch(c)
      {
      case 'a':
        note = new Note(Note.Name.A, accidental, 0);
        break;
      case 'b':
        note = new Note(Note.Name.B, accidental, 0);
        break;
      case 'c':
        note = new Note(Note.Name.C, accidental, 0);
        break;
      case 'd':
        note = new Note(Note.Name.D, accidental, 0);
        break;
      case 'e':
        note = new Note(Note.Name.E, accidental, 0);
        break;
      case 'f':
        note = new Note(Note.Name.F, accidental, 0);
        break;
      case 'g':
        note = new Note(Note.Name.G, accidental, 0);
        break;
      default:
        throw new IllegalArgumentException("Invalid note name");
      }

    // return note
    return note;
    }

  }/** end of class IntonationFactory */
