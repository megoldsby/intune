
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
import java.util.*;

/**
 *  Class <code>SpineScanner</code> is a <code>Scanner</code>
 *  that scans all or part of a <code>Spine</code>.
 *  @see  intune.Scanner
 *  @see  intune.Spine
 */
class SpineScanner implements Scanner
  {
  /** the spine scanned by this scanner */
  private Spine spine;

  /** iterator for the spine */
  private Iterator iterator;

  /** index of first element of spine that is produced */
  private int start;

  /** one plus index of last element of spine that is produced */
  private int end;

  /** current element in the scan */
  private String current;

  /** position in the iteration */
  private int position;

  /**
   *  Constructor.
   *  @param   spine     spine that this scanner scans
   *  @param   iterator  iterator for the scan
   *  @param   start     index of starting element in spine
   *  @param   end       one plus index of last element scanned
   */
  SpineScanner (Spine spine, Iterator iterator, int start, int end)
    {
    this.spine = spine;
    this.iterator = iterator;
    this.start = start;
    this.end = end;
    this.position = 0;
    }
  /**
   *  Returns the spine underlying this scanned spine.
   *  @result  spine 
   */
  Spine getSpine() 
    {
    return this.spine;
    }
  /**
   *  Returns the spine number of the scanned spine.
   *  @result  spine number
   */
  int getSpineNumber() 
    {
    return this.spine.getSpineNumber();
    }
  /**
   *  Returns true if the iteration is not exhansted.
   *  @result  true if can advance
   */
  public boolean more()
    {
    return this.iterator.hasNext();
    }
  /**
   *  Advances to the next elements, making it the current element.
   */
  public void advance() throws NoSuchElementException
    {
    this.current = (String)this.iterator.next(); 
    this.position++;
System.out.println(this.current + " (Scanner)");
    } 
  /**
   *  Returns the current element.
   *  @result  current element
   */
  public String current()
    {
    return this.current;
    }
  /**
   *  Returns a new <code>Section</code> object starting
   *  at the current position.
   *  @param   sectionName  the section name
   *  @result  a new <code>Section</code> object
   */  
  Section startSection(String sectionName)
    {
    return new Section(sectionName, this, this.position);
    }
  /**
   *  Returns new <code>SpineScanner</code> object that scans from
   *  a given position to the current position in the same spine.
   *  @param   start   the starting position for the scan
   *  @result  spine scanner 
   */
  SpineScanner scanner(int start)
    {
    return this.spine.scanner(this.start, this.position);
    }
  /**
   *  Returns new <code>SpineScanner</code> object that scans 
   *  the same range of elements as this spine scanner.
   *  @result  spine scanner with same range
   */
  SpineScanner scanner()
    {
    return this.spine.scanner(this.start, this.end);
    }
  }
