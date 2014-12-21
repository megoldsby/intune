
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
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

public class FunnyList<T> {

  private Entry<T> head;

  public FunnyList() {
    this.head = new Entry<T>(null);
    this.head.pred = this.head;
    this.head.succ = this.head;
  }

  public ListIterator<T> append(T contents) {
    Entry<T> entry = new Entry<T>(contents);
    Entry<T> tail = this.head.pred;
    entry.pred = tail;
    entry.succ = this.head;
    tail.succ = entry;
    this.head.pred = entry;
    return (ListIterator<T>)entry;
  }

  private void display() {
    
  }

  /**
   *  Inner class <tt>Entry<T></tt> servers as container for an
   *  an element of a <tt>FunnyList</tt> and also functions
   *  as a <tt>ListIterator</tt>.
   */
  private class Entry<T> implements ListIterator<T> {
    private Entry<T> pred;    
    private Entry<T> succ;    
    private T contents;
    private Entry<T> next;
    private Entry<T> removable;
    private Entry(T contents) {
      this.contents = contents; 
      this.next = this;
    }
    public void add(T contents) {  
      Entry<T> newEntry = new Entry<T>(contents); 
      newEntry.succ = this.next.pred.succ;
      newEntry.pred = this.next.pred;
      this.next.pred.succ = newEntry;
      this.next.pred = newEntry;
      this.removable = null;
    }
    public boolean hasNext() { 
      return !(this.next.succ == FunnyList.this.head);
    }
    public boolean hasPrevious() {  
      return !(this.next.pred == FunnyList.this.head);
    }
    public T next() { 
      if (this.next == FunnyList.this.head) {
        throw new NoSuchElementException();
      }
      T contents = this.next.contents;
      this.removable = this.next;
      this.next = this.next.succ;
      return contents;
    }
    public int nextIndex() { 
      throw new UnsupportedOperationException();
    }
    public T previous() {
      if (this.next.pred == FunnyList.this.head) {
        throw new NoSuchElementException();
      }
      this.next = this.next.pred;
      this.removable = this.next;
      return this.next.contents;
    }
    public int previousIndex() {
      throw new UnsupportedOperationException();
    }
    public void remove() {  
      if (this.removable != null) {
        if (this.removable == this.next) {
          if (!(this.next != FunnyList.this.head)) {
            this.next = this.next.succ;
          }
        }
        this.removable.pred.succ = this.removable.succ;
        this.removable.succ.pred = this.removable.pred; 
        this.removable = null;
      }
    }
    public void set(T contents) {  
      throw new UnsupportedOperationException();
    }
  }

  public static void main(String[] args) {
    FunnyList<Integer> flist = new FunnyList<Integer>();
    List<ListIterator<Integer>> alist = new ArrayList<ListIterator<Integer>>();
    for (int i = 0; i < 10; i++) {
      Integer contents = new Integer(i);
      ListIterator<Integer> it = flist.append(contents);
      alist.add(it);
    }
    
  }

}
