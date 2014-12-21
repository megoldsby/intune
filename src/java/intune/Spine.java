
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
 *  Class <code>Spine</code> contains the Humdrum **kern
 *  elements for one spine of a composition.
 */
class Spine
  {
  /** which spine this is (0, 1, ..) */
  private int spineNumber;

  /** the spine elements, in time order */
  private List elements = new ArrayList();

  /** count of open phrases and slurs */
  private int phraseCount;
  private int slurCount;

  /**
   *  Constructor.
   *  @param   spineNumber   which spine this is 
   */
  Spine (int spineNumber)
    {
    this.spineNumber = spineNumber;
    }
  /**
   *  Returns spine number of this spine.
   *  @result  spine number
   */
  int getSpineNumber()
    {
    return this.spineNumber;
    }
  /**
   *  Appends an element to this spine.
   *  @param  element  the element
   */
  void addElement(String element)
    {
    this.elements.add(element);
    }
  /**
   *  Returns the number of elements in this spine.
   *  @result  number of elements in spine
   */
  int size()
    {
    return this.elements.size();
    }
  /**
   *  Increments the open phrase count.
   */
  void openPhrase()
    {
    this.phraseCount++;
    }
  /**
   *  Decrements the open phrase count.
   */
  void closePhrase()
    {
    this.phraseCount--;
    }
  /**
   *  Returns the current phrase count.
   *  @result  number of open phrases
   */
  int getPhraseCount()
    {
    return this.phraseCount;
    }
  /**
   *  Increments the open slur count.
   */
  void openSlur()
    {
    this.slurCount++;
    }
  /**
   *  Decrements the open slur count.
   */
  void closeSlur()
    {
    this.slurCount--;
    }
  /**
   *  Returns the current slur count.
   *  @result  number of open phrases
   */
  int getSlurCount()
    {
    return this.slurCount;
    }
  /**
   *  Creates a <code>Scanner</code> for this spine 
   *  that scans the entire spine.
   *  @result  Scanner object for this spine
   */
  SpineScanner scanner()
    {
    return new SpineScanner(this, 
                            new SpineIterator(0, this.elements.size()),
                            0, this.elements.size());
    } 
  /**
   *  Creates a <code>Scanner</code> for this spine 
   *  that scans starting at a given start element and
   *  ending at (just before) a given end element.
   *  @param   start   index of start element
   *  @param   end     index of end element
   */
  SpineScanner scanner(int start, int end)
        throws IllegalArgumentException
    {
    if (start < 0 || end > this.elements.size())
      {
      throw new IllegalArgumentException();
      }
    return new SpineScanner(this, new SpineIterator(start, end),
                            start, end);
    }
  /**
   *  Inner class <code>SpineIterator</code> is an 
   *  <code>Iterator</code> over the elements of this spine
   */
  class SpineIterator implements Iterator
    {
    /** current position in spine */
    int position;

    /** index of last element produced minus one */
    int end;

    /**
     *  Constructor.
     *  @param   start   index in spine of first element produced
     *  @param   end     one plus index of last element produced
     */
    SpineIterator(int start, int end)
      {
      this.position = start;
      this.end = end;
      }
    /**
     *  Returns true if the iteration is not exhausted.
     *  @result  true if there is a next element
     */
    public boolean hasNext()
      {
      return (this.position < this.end);
      }
    /**
     *  Returns the first element not yet produced.
     *  @result  the next element
     *  @throws  NoSuchElementException
     *           if the iteration is exhausted
     */
    public Object next()
      {
      Object current;
      if (hasNext())
        {  
        current = (String)Spine.this.elements.get(this.position++);
        }
      else
        {
        throw new NoSuchElementException("SpineScanner exhausted");
        }
      return current;
      }
    /**
     *  Not yet implemented.
     *  @throws  UnsupportedOperationException  always
     */
    public void remove() throws UnsupportedOperationException, 
                                IllegalStateException
      {
      throw new UnsupportedOperationException(); 
      }
    } /** end of inner class SpineIterator */
  }
/** end of class Spine */
