
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

import java.util.*;
class MapTest
  {
  static public void main(String[] args)
    {
    Map map = new HashMap();

    // this way produces a map of size 2
    String name = "xyz";
    map.put(new Key(name,0), "first");
    map.put(new Key(name,0), "second");
/**
    // this way produces a map of size 1
    Key key = new Key("xyz",0);
    map.put(key, "first");
    map.put(key, "second");
**/
    System.out.println(map.size());
    }

  static class Key
    {
    String name;
    int ordinal;
    Key(String name, int ordinal)
      {
      this.name = name;
      this.ordinal = ordinal;
      }
    public int hashCode()
      {
      return (this.name.hashCode() ^ this.ordinal);
      }
    public boolean equals(Object o)
      {
      boolean eq = (o instanceof Key
                    && this.name.equals(((Key)o).name)
                    && this.ordinal == ((Key)o).ordinal);
      System.out.println(eq);
      return eq;
      }
    }
  }
/***
  class Key
    {
    String name;
    int ordinal;
    Key(String name, int ordinal)
      {
      this.name = name;
      this.ordinal = ordinal;
      }
    public boolean equals(Object o)
      {
      return (o instanceof Key
              && this.name.equals(((Key)o).name)
              && this.ordinal == ((Key)o).ordinal);
      }
    }
***/
