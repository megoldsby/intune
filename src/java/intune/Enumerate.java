
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 *  Class <code>Enumerate</code> is the base class for enumeration types.
 */
public abstract class Enumerate  {

  /** component of map key for maximum ordinal so far */
  private static final int MAX_KEY = -1;
  
  /** map containing all enumerates so can do iterations over them */
  private static Map enumerations = new HashMap();
  
  /** class name of this enumerated value */
  private String type;
  
  /** name of this enumerated value */
  private String name;
  
  /** order number of this enumerated value within its type */ 
  private int ordinal;

  /**
   *  Constructor.
   *  @param   name of enumerated value
   */
  public Enumerate(String name) {
    this.name = name;
    this.type = this.getClass().getName();
    addToMap();
  }
 
  /**
   *  Adds this object to the map of all enumerates.
   */
  private void addToMap(){
    synchronized (Enumerate.enumerations){

      // this object's ordinal = max ordinal for this type so far + 1
      Key maxKey = new Key(this.type, MAX_KEY);
      Object max = Enumerate.enumerations.get(maxKey);
      this.ordinal = (max == null ? MAX_KEY + 1 : 
                      ((Integer)max).intValue() + 1);

      // install new max ordinal for this type
      // and this instance of this type
      Enumerate.enumerations.put(maxKey, new Integer(this.ordinal));
      Enumerate.enumerations.put(new Key(this.type, this.ordinal), this);
    }
  }

  /**
   *  Returns an iterator over the values in this enumerated class.
   *  @result  the iterator
   */
  public java.util.Iterator iterator() { 
    // create and return an Iterator
    Integer max = (Integer)Enumerate.enumerations.get(
      new Key(this.type, MAX_KEY));
    return new Enumerate.Iterator(this.type, this.name, max.intValue());  
  }
  
  public String getName(){
    return this.name;
  }
  
  public int getOrdinal(){
    return this.ordinal;
  }
  
  public int getSize(){
    Integer max = 
      (Integer)Enumerate.enumerations.get(new Key(this.type, MAX_KEY));
    return max.intValue() + 1;
  }
  
  public Enumerate get(int ordinal){
    Enumerate e = null;
    if (ordinal > MAX_KEY){
      e = (Enumerate)Enumerate.enumerations.get(new Key(this.type, ordinal));
    }
    return e;
  }
  
  public Enumerate[] toArray(){
    Enumerate[] array = null;
    Object o = Enumerate.enumerations.get(new Key(this.type, MAX_KEY));
    if (o != null){
      array = new Enumerate[((Integer)o).intValue()];
    } else {
      array = new Enumerate[0];
    }
    for (int i = 0; i < array.length; i++){
      array[i] = (Enumerate)Enumerate.enumerations.get(new Key(this.type, i));
    }
    return array;
  }

  public boolean equals(Object that){
    return (this == that
            || (that instanceof Enumerate
                && this.type.equals(((Enumerate)that).type)
                && this.name.equals(((Enumerate)that).name)
                && this.ordinal == ((Enumerate)that).ordinal));
  }

  public int hashCode(){
    return this.type.hashCode() ^ this.name.hashCode() ^ this.ordinal;
  }

  
  public String toString(){
    return this.type + " " + this.name + " (" + this.ordinal + ")"; 
  }
    
    /**
     *  Inner class <code>Iterator</code> implements an iterator
     *  for an enumeration class.
     */
    private static class Iterator implements java.util.Iterator {
      
      /** type and name of enumeration */
      String type;
      String name;
      int max;
      int current = 0;
      Iterator(String type, String name, int max) {  
        this.type = type;
        this.name = name;
        this.max = max;
      }

      public void remove() {
        throw new UnsupportedOperationException(
          "Enumerate.Iterator does not implement remove");
      }
        
      public boolean hasNext() {
        return this.current <= this.max;
      }
      
      public java.lang.Object next() {
        Object o = Enumerate.enumerations.get(
          new Key(this.type, this.current++));
        if (o == null){
          throw new IllegalStateException("Iteration exhausted");
        }          
        return o;
      }
      
    }
    
    /**
     *  Static inner class <code>Key</code> defines the key used
     *  in the map of all enumerated values.
     */
    private static class Key {
      private String type;
      private int ordinal;
      Key(String type, int ordinal) {
        this.type = type;
        this.ordinal = ordinal;
      }
      public boolean equals(Object o) {
        return ((o == this)
                || (o instanceof Key
                    && this.type.equals(((Key)o).type)
                    && this.ordinal == ((Key)o).ordinal));
      }
      public int hashCode(){
        return this.type.hashCode() ^ this.ordinal;
      }
    }
    
  /**
   *  Tests this module.
   */
  public static void main(String[] args){
    try {
      Color c = Color.GREEN;
      Color k = Color.GREEN;
      System.out.println(c.equals(k));
      System.out.println(c == k);
      display();
      System.out.println(c.getName());
      System.out.println(c.getOrdinal());
      System.out.println(c.getSize());
      System.out.println(c.get(1));
      java.util.Iterator iterator = c.iterator();
      while (iterator.hasNext()){
        System.out.println(iterator.next());
      }
      Color b = Color.BLUE;
      Color g = Color.GREEN;
      System.out.println(b.equals(c));
      System.out.println(g.equals(c));
    }catch(Exception e){
      e.printStackTrace();
    }
  }
  /**
   *  For testing.
   */
  static void display()
    {
    Set keyset = Enumerate.enumerations.keySet();
    Object[] keys = keyset.toArray();
    for (int i = 0; i < keys.length; i++)
      {
      Key key = (Key)keys[i];
      System.out.println("> " + key.type + " " + key.ordinal + ": " +
        Enumerate.enumerations.get(keys[i]));
      } 
    }
  }

      /**
       *  For testing.
       */
      class Color extends Enumerate { 
        public static final Color RED = new Color("red");
        public static final Color GREEN = new Color("green");
        public static final Color BLUE= new Color("blue");
        private Color(String name) { super(name); /*display();*/ }
      }
