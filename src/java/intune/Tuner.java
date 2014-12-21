
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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

/**
 *  Class <tt>Tuner</tt> accepts an input file giving a list of
 *  modulations (a list of keys).
 */
public class Tuner implements Runnable {
 
  // file containing the modulation list
  File modFile;

  /**
   *  Constructor.
   */
  public Tuner(File modFile) {
    this.modFile = modFile;
    new Thread(this).start();
  }

  /**
   *  Reads the modulation list and performs the modulations.
   */
  public void run() {
    try {
      Diatonic diatonic = null;
      BufferedReader in = new BufferedReader(new FileReader(this.modFile));
      String line = null;
      while ((line = in.readLine()) != null) {
        if (line.startsWith("#")) {
          System.out.println(line);
	}	
        if (!line.startsWith("#") && !line.trim().equals("")) {
          StringTokenizer tokens = new StringTokenizer(line);
          if (tokens.hasMoreTokens()) {
            String token = tokens.nextToken();
            if (diatonic == null) {
              // first time: token = initial key name
              diatonic = (Diatonic)
                IntonationFactory.create("diatonic", token);
            } else {
              if (token.startsWith("s")) {
                if (token.indexOf('+') > 0) {
                  diatonic.setSubdominant(true);
                } else if (token.indexOf('-') > 0) {
                  diatonic.setSubdominant(false);
                }
              } else {
                Note note = keyNameToNote(token);
                diatonic.modulate(note);
              }
            }
          }
        } 
      }
    } catch(IOException e) {
      e.printStackTrace();
    } 
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

  public static void main(String[] args) {
    if (args.length < 1) {
      throw new IllegalArgumentException(
        "Usage: java intune.Tuner modulations.txt" );
    }
    new Tuner(new File(args[0]));
  }

}/** end of class Tuner */
