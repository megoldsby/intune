
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
 *  Class <code>Section</code> represents a **kern section
 *  within a spine.
 */
class Section
  {
  /** the section name */
  private String sectionName;

  /** the spine in which the section lies */
  private SpineScanner spine;

  /** starting position within the spine */
  private int start;

  /** a scanner that scans exactly this section's elements 
   *  in the section's spine */
  SpineScanner scanner = null;

  /**
   *  Returns the name under which a section is stored given
   *  the section name and the number of its spine.
   */
  static String getLookupName(String sectionName, int spineNumber)
    {
    return sectionName + "-" + spineNumber;
    }

  /**
   *  Constructor.
   *  @param   sectionName  the section name
   *  @param   spine        spine in which section occurs
   *  @param   start        
   */
  Section(String sectionName, SpineScanner spine, int start)
    {
    this.sectionName = sectionName;
    this.spine = spine;
    this.start = start;
    }
  /**
   *  Returns a <code>SpineScanner</code> object that scans
   *  exactly the elements of the spine belonging to this section.
   *  @result  a scanner for this section
   */
  SpineScanner scanner()
    {
    // if the scanner hasn't been created yet, create it 
    // (it will scan from the starting element supplied when this
    // section was created to the current position in the spine)
    if (this.scanner == null)
      {
      this.scanner = this.spine.scanner(this.start);
      }

    // return a replica of the section scanner
    return this.scanner.scanner();
    }
  String getName()
    {
    return this.sectionName;
    }
  String getLookupName()
    {
    return getLookupName(this.sectionName, this.spine.getSpineNumber());
    }
  }
/** end of class Section */
