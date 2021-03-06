
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
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

/**
 *  Class <tt>IntuneFile</tt> provides an in-memory
 *  representation of a file containing MIDI note on and note off
 *  commands, MIDI pitch bend commands and modulation and
 *  subdominant directives.
 */
class IntuneFile {
  
  // nanoseconds per second
  private static final long ONE_SECOND = 1000000000;

  // number of items before and after current item to display
  private static final int DISPLAY_DEPTH = 5;

  /** instance variables */

  // intonation name and key name 
  private String intonationName;
  private String keyName;

  // the intonation used in this run
  Intonation intonation;

  // timbre for this run
  int timbre; 

  // the items from the file, in a time-accessible data structure
  TimedItems timedItems;

  /**
   *  Constructor.
   *  @param   intonationName
   *           name of intonation from command line
   *  @param   keyName
   *           key name from command line (null if none)
   *  @param   timbre
   *           timbre from command line
   * @see      open
   */
  IntuneFile(String intonationName,
             String keyName,
             int timbre) {
    this.intonationName = intonationName;
    this.keyName = keyName;
    this.timbre = timbre;
  }

  /**
   *  Reads in an intune-format file.
   *  @param   fileName
   *           name of the file
   */
  void read(String fileName) {
    DataInputStream in = null;
    List<Item> items = null;
    try {
      // open the input file
      in = new DataInputStream(
             new BufferedInputStream(
               new FileInputStream(fileName)));  

      // read the intonation line 
      // (if there isn't one, substitute blank line--the file may have
      // been produced by the 'save' feature of PlayTuned)
      String intonationLine = " ";
      try {
        intonationLine = in.readUTF();
      } catch(Exception e) {  
        in.close();
        in = new DataInputStream(
               new BufferedInputStream(
                 new FileInputStream(fileName)));
      }

      // extract the intonation class name (and possibly key)
      // and the timbre from the intonation line
      String recordedIntonationName = null;
      String recordedKeyName = null;
      int recordedTimbre = Constants.NONE;
      StringTokenizer tokenizer = new StringTokenizer(intonationLine);
      List<String> tokenList = new ArrayList<String>();
      while (tokenizer.hasMoreTokens()) {
        tokenList.add(tokenizer.nextToken()); 
      }
      String[] tokens = (String[])tokenList.toArray(new String[0]);
      int index = 0;
      if (index < tokens.length && !isSwitch(tokens[index])) {
        recordedIntonationName = tokens[index++];
        if (index < tokens.length && !isSwitch(tokens[index])) {
          recordedKeyName = tokens[index++];
        }
      }
      if (index < tokens.length && isSwitch(tokens[index])) {
        String option = tokens[index++];
        if (option.equals("-t")) {
          recordedTimbre = Integer.parseInt(tokens[index++]); 
        } else {
          System.err.println(
            "Warning: unrecognized option in intonation line");
        }
      }
      System.out.println(
        "Recorded with intonation " + recordedIntonationName +
        (recordedKeyName != null ? ", key " + recordedKeyName : "") +
	  " and timbre " + 
	  (recordedTimbre != Constants.NONE ? recordedTimbre : Constants.NONE));

      // give preference to the intonation and key names and timbre passed
      // in the constructor, otherwise use the ones in the input file
      if (this.intonationName == null) {
        this.intonationName = recordedIntonationName;
        this.keyName = recordedKeyName;
      }
      if (this.timbre == Constants.NONE) {
        this.timbre = recordedTimbre;
      }

      // if there is no intonation specified (as could happen if
      // the input file was not created by the intunation editor and no
      // intonation was given on the command line), we can't proceed
      if (this.intonationName == null) {
        throw new IllegalArgumentException("Must specify intonation");
      }

      // develop the intonation object
      this.intonation = IntonationFactory.create(this.intonationName, 
                                                 this.keyName);
      System.out.println(
        "Intonation " + this.intonation.getClass().getName());

      // read each item from the file and append it to a list
      items = readItems(in);

    } catch(IOException e) {
      System.err.println("Error reading input file");
      e.printStackTrace();
      System.exit(-1);
    }

    // close the input
    try {
      in.close();
    } catch(IOException e) {  }

    // adjust the times so that the first MIDI event occurs at time zero
    zeroStartTime(items); 

    // transform the item list into a time-accessible data structure.
    this.timedItems = new TimedItems(items, this.intonation);
  }

  /**
   *  Opens an intune-format file so that items can be inserted in it.
   */
  void open() {

    // develop the intonation object
    this.intonation = IntonationFactory.create(this.intonationName, 
                                               this.keyName);

    // create an empty time-accessible data structure for file items
    this.timedItems = new TimedItems(this.intonation);
  }

  /**
   *  Returns the intonation for this run.
   *  @result  the intonation object
   */
  Intonation getIntonation() {
    return this.intonation;
  }

  /**
   *  Returns the timbre for this run.
   *  @result  the timbre
   */ 
  int getTimbre() {
    return this.timbre;
  }

  /**
   *  Returns the time at the current position in the timed items.
   *  @result  the current time
   */ 
  long getCurrentTime() {
    return this.timedItems.getCurrentTime();
  }

  /**
   *  Returns the next available <tt>Message</tt> from the file,
   *  skipping the non-<tt>Message</tt> items.
   *  @result   next message in the timed items list
   */
  Message nextMessage() {
    Item item = this.timedItems.nextItem();
    while (item != null && !(item instanceof Message)) {
      item = this.timedItems.nextItem();
    }
    return (Message)item;
  }

  /**
   *  Moves current position in file.
   *  @param   time
   *           time to move to
   */
  void movePosition(double time) {
    this.timedItems.seek((long)(time * ONE_SECOND));
  }

  /**
   *  Moves current position in file.
   *  @param   count
   *           number of items to move (forward if positive,
   *           backward if negative)
   */
  void movePosition(int count) {

    // if count is positive, move forward
    if (count > 0) {
      Item current = null;
      for (int i = 0; i < count; i++) {
        current = this.timedItems.nextItem();
      }

      // if moved past end, back up one
      if (current == null) {
        this.timedItems.previousItem(); 
      }   

    // if count is negative, move backward
    } else if (count < 0) {
      for (int i = 0; i < -count; i++) {
        this.timedItems.previousItem();
      }
    }
  }

  /**
   *  Deletes the current item.
   */
  void deleteItem() {
    this.timedItems.delete(); 
  }

  /**
   *  Inserts a modulation before the current item.
   *  @param   noteNumber
   *           MIDI note number of tonic of key to modulate to
   */
  void insertModulation(int noteNumber) {
    this.timedItems.insertModulation(noteNumber);
  }

  /**
   *  Inserts a subdominant directive before the current item.
   *  @param   subdominant
   *           true if subdominant tuning is in effect
   */
  void insertSubdominantDirective(boolean subdominant) {
    this.timedItems.insertSubdominantDirective(subdominant);
  }

  /**
   *  Inserts a note message at the indicated time.
   *  @param   timestamp
   *           the indiciated time (nanoseconds)
   *  @param   noteNumber
   *           MIDI note number
   *  @param   velocity
   *           inserts NoteOn if positive, NoteOff if zero
   */
  void insertNoteMessage(long timestamp, int noteNumber, int velocity) {
    this.timedItems.insertNoteMessage(timestamp, noteNumber, velocity);
  }

  /**
   *  Ensures that the tuning is correct if play is begun
   *  at the given time in the file.
   *  @param   time
   *           the given file time
   */
  void tuneFor(long time) {
    this.timedItems.seek(time);
  }

  /**
   *  Displays items around the current item (results not
   *  defined unless Player is paused).
   */
  void display() {
    this.timedItems.display(DISPLAY_DEPTH);
  }

  /**
   *  Writes the items in this <tt>IntuneFile</tt> to disk.
   *  @param   filename
   *           name of file to write to
   */
  void write(String filename) {
    DataOutputStream out = null;
    try {
      // open the output stream
      out = new DataOutputStream(
              new BufferedOutputStream(
                new FileOutputStream(filename)));

      // write the intonation line
      String intonationLine = this.intonationName + 
		(this.keyName != null ? " " + this.keyName : "") +
		(this.timbre != Constants.NONE ? " -t " + this.timbre : "");
      out.writeUTF(intonationLine);

	// write the file items
      movePosition(0.0);
      System.out.println("Writing items:");
      Item item = null;
      while ((item = this.timedItems.nextItem()) != null) {
        item.display(false);
        item.write(out);
      } 

      // close the output
      out.close();
	System.out.println();

    } catch(IOException e) {
      System.err.println("Cannot write output file");
      e.printStackTrace();
      System.exit(-1);
    }
  }

  /**
   *  Displays the current position in the file.
   */
  void displayPosition() {
    this.timedItems.displayPosition();
  }
  
  /**
   *  Reads items from the given input stream and returns
   *  a list of them.
   *  @param   in
   *           the input stream
   *  @result  list of item read from the input stream
   */
  private List<Item> readItems(DataInputStream in) throws IOException {
    List<Item> items = new ArrayList<Item>();
    boolean eof = false;
    while (!eof) {
      try {
        // if next item is a Note On or Note Off MIDI message..
        int command = in.readByte() & 0x0ff;
        if (command == ShortMessage.NOTE_ON
            || command == ShortMessage.NOTE_OFF) {

          // read MIDI message and timestamp following it and
          // and append them as a message
          ShortMessage message = readMessage(in, command); 
          long timestamp = readTimestamp(in);

          // append it as a Message
          items.add(new Message(message, timestamp));

        // if next item is a tuning request..
        } else if (command == ShortMessage.TUNE_REQUEST) {
          int type = in.readByte() & 0x0ff;

          // if it is a modulation..
          if (type == Constants.TuneRequest.MODULATION) {

            // read note number of key to modulate to 
            // and timestamp and append item as a modulation
            int noteNumber = in.readByte() & 0x0ff;
            long timestamp = readTimestamp(in);
            items.add(new Modulation(noteNumber, timestamp));
	      System.out.println("readItems added modulation: " +
              "note# " + noteNumber);

          // if it is a subdominant directive..
          } else if (type == Constants.TuneRequest.SUBDOMINANT) {

            // read state (on or off) and timestamp
            // and append item as a  sudominant directive
            boolean state = in.readBoolean();
            long timestamp = readTimestamp(in);
            items.add(new SubdominantDirective(state, timestamp));
	      System.out.println("readItems added subdominant directive: " +
              (state ? "on" : "off"));

          // if it's anything else, complain
          } else {
            System.err.println("Unrecognized tuning request: " + type);
            System.exit(-1);
          }

        // if it's any other kind of MIDI message..
        } else {

          // read past the message and the following timestamp
          // but don't append them to the items
          ShortMessage message = readMessage(in, command); 
          long timestamp = readTimestamp(in);
          System.out.println("readItems skipped ShortMessage " + command);
        }
      } catch(EOFException e) {
        System.out.println("readItems EOF: " + e.getMessage());
        eof = true;
      } 
    }//while not eof

    System.out.println("Item list:");
    Iterator it = items.iterator();
    int count = 0;
    while (it.hasNext()) {
      Item item = (Item)it.next();
      item.display(false); 	
	count++;
    }	
    System.out.println("" + count + " items");
    System.out.println();

    // return the list of items read from the input source
    return items;
  }

  /**
   *  Reads a MIDI message from the input.
   *  @param   input
   *           the input
   *  @param   command
   *           command for the message
   *  @result  MIDI message
   */
  private ShortMessage readMessage(DataInputStream input,
                                   int command) throws EOFException {
    ShortMessage message = null;
    int channel = -1;
    int data1 = -1;
    int data2 = -1;

    try {
      channel = input.readByte() & 0x0ff;
      data1 = input.readByte() & 0x0ff;
      data2 = input.readByte() & 0x0ff;
      message = new ShortMessage();
      message.setMessage(command, channel, data1, data2);

    } catch(InvalidMidiDataException e) {
      System.err.println("Can't set ShortMessage: " +
        command + " " + channel + " " + data1 + " " + data2 + ": " + e);
      throw new EOFException("read invalid data");

    } catch(EOFException e) {
      throw new EOFException();

    } catch(IOException e) {
      System.err.println("Can't read input file: " + e);
      e.printStackTrace();
      throw new EOFException("i/o error");
    }

    System.out.println("readMessage returning " + message);
    return message; 
  }

  /**
   *  Reads timestamp from the input and returns it.
   *  @param   input
   *           the input 
   *  @result  timestamp (nanoseconds from start)
   */
  private long readTimestamp(DataInputStream input) throws EOFException { 
    long timestamp = -1;

    try { 
      timestamp = input.readLong();

    } catch(IOException e) {
      System.err.println("Can't read input file: " + e);
      e.printStackTrace();
      throw new EOFException("i/o error");
    }

    return timestamp;
  }  

  /**
   *  Adjusts the timestamps of all the items in the given list
   *  so that the first event occurs at time zero.
   */
  private void zeroStartTime(List<Item> items) {
    int size = items.size();
    if (size > 0) {
      long offset = items.get(0).timestamp;
      for (int i = 0; i < size; i++) {
        Item item = items.get(i);
        item.timestamp -= offset;
      }
    }
  }

  /**
   *   Returns true if argument starts with hyphen.
   *   @param   arg
   *   @result  true if arg starts with "-"
   */
  private static boolean isSwitch(String arg) {
    return (arg.startsWith("-"));
  }

  /**
   *  Static inner class <tt>TimedItems</tt> contains the file 
   *  items made accessible by time.
   */
  private static class TimedItems {

    // If user inserts a new modulation, all tuning status
    // entries after that point may be invalid.
    // for example, suppose you're in the key of C, then modulate
    // to note number 4 (e), then to note number 8 (G#).  Now
    // remove the modulation to note number 4.  Then the
    // modulation to note number 8 becomes a modulation to Ab.
    // Perhaps the tuning status list should be accumulated as
    // you play.  What if you want to leap ahead?

    // There may be another issue here, too.  If I modulate to
    // a certain key, the frequency I give the tonic of
    // that key will depend on the frequency of the tonic
    // of the key I modulated from.  For instance, if I am in C
    // and modulate to d when subdominant tuning is on, the result
    // will be different than if it was off.  So simply recording
    // the modulation status as being in the key of d (with
    // subdominant tuning on or off) does not tell the whole story.
    // It appears I also need to record the frequency of the
    // tonic.  Note that in order to recover this frequency, I
    // actually have to perform the modulation.
    //   That raises the question of whether I should perform modulations
    // in IntuneFile or whether I should let Player perform the
    // modulations and report them back to IntuneFile.  Right now,
    // I favor the former. 

    // What happens when I delete or insert a modulation?  If I
    // start playing right from that point, the key and its
    // tonic frequency are undefined.  I believe I need to take
    // a step back in order to recover a known state.  
    //   If I delete a modulation, I can retrieve the tuning status
    // right before the deletion.  That status may have been established
    // a considerable time earlier, but I should not move the
    // current time.
    //   If I insert a modulation, I can let it determine the new
    // tuning status.

    // What if there is no applicable tuning status when I seek
    // backwards in time?  That means that no modulation or 
    // subdominant directive had been issued as of the seek time.
    // I could prime the tuning status list with a zero-time entry
    // that contained some sort of tuning status (perhaps just
    // a 'subdominant off' directive), then this would never occur.
    // But there's more to it than that.  What I really need in this
    // case is a return to the original key and the original tonic
    // frequency.  To have those, I need to record the original key
    // and original frequency.  To begin with, I have a key name. 
    // I do not have a note number for the tonic, and I do not have
    // a frequency for the tonic.  I could retrieve the tonic
    // frequency if I had the tonic Note, and I could retrieve the 
    // tonic Note if I had its note number.  Recreating the intonation
    // would do the trick, but that would force Player and the editing
    // logic always to do getIntonation instead of holding a reference
    // to the intonation (which seems rather clumsy and arbitrary).
    // Anyway, initially I have the key name and from it I need to
    // recover the tonic Note and tonic frequency.  Added getTonic
    // to Modulator.

    // It might be useful to note which action can invalidate the
    // tuning status.  I believe they are: deleting an item,
    // inserting an item, or backing up in the items.  All of these
    // actions are done when the player is paused (how do I enforce
    // that?).  Bringing the tuning status back to what it should be 
    // when play is resumed must not be done until play is actually
    // resumed, because doing so would change the current position
    // in the items, which must be maintained (until play is resumed)
    // for editing purposes.

    // zero nanoseconds
    private final static long TIME_ZERO = 0L;

    // intonation in use
    Intonation intonation;

    // the file items, in ascending time order
    // (includes any inserted tuning requests)
    List<Item> timedItems = new ArrayList<Item>();

    // current position in timedItems 
    int itemIndex;

    // initial tuning status
    TuningStatus tuningStatus0;

    // time of most recent Message item retrieved by nextItem()
    long currentTime;

    /**
     *  Constructor.
     *  @param   items
     *           the file items as a sequence of <tt>Item</tt> objects
     *  @param   intonation
     *           the intonation that is in use
     */
    TimedItems(List<Item> items, Intonation intonation) {

      // save the intonation
      this.intonation = intonation;

      // save the item list
      this.timedItems = items;
      System.out.println("size of timed items = " + items.size());

      // initialize the current position in the item list
      this.itemIndex = 0;

      // discover the initial the tuning status (the original key
      // and its tonic frequency, with subdominant tuning off) 
      if (this.intonation instanceof Modulator) {
        Note tonic = ((Modulator)this.intonation).getTonicNote();
        double frequency = -1;
        try {
          frequency = this.intonation.frequency(tonic);
        } catch(UnsupportedNoteException e) {
          e.printStackTrace();
          System.exit(-1);
        }
        this.tuningStatus0 = new TuningStatus(tonic, frequency, false);
      }
    }

    /**
     *  Constructor used to create a <tt>TimedItems</tt> object
     *  that has no items (yet).
     *  @param   intonation
     *           the intonation that is in use
     */
    TimedItems(Intonation intonation) {
      this(new ArrayList<Item>(), intonation);
    }

    // when I seek to a new time and pick up the tuning status, I need to
    // know a note, not a note number.  A note number only means something
    // in the context of the current key.  

    /**
     *  Returns the current item, applying it to the intonation if
     *  it is a tuning request, and advances to the next item.
     *  @result   next item in the timed items list
     */
    Item nextItem() {

      // get current item (null it at end of list)
      Item item = null;
      if (this.itemIndex < this.timedItems.size()) {
        item = (Item)this.timedItems.get(this.itemIndex);
        this.itemIndex++;
      }

      // provided are not at end of list..
      if (item != null) { 

        // if current item is a modulation, perform the modulation
        if (item instanceof Modulation) {
          performModulation((Modulation)item);
 
        // if current item is a subdominant directory, carry out
        // the directive
        } else if (item instanceof SubdominantDirective) {
          performSubdominantDirective((SubdominantDirective)item);
 
        // if current item is a message, record its time 
        } else if (item instanceof Message) {
          this.currentTime = ((Message)item).timestamp;
 	    //ShortMessage m = ((Message)item).message;
 	    //System.out.println("nextItem returning Message: " +
          //  m.getCommand() + " " + m.getChannel() + " " +
 	    //  m.getData1() + " " + m.getData2()); 
        }
      }//if item not null

      // return the current item (or null if at end of list)
      return item;
    }

    /**
     *  Performs a given modulation
     *  @param   modulation
     *           the modulation
     */
    private void performModulation(Modulation modulation) {
      if (this.intonation instanceof Modulator) {
        Modulator modulator = (Modulator)this.intonation;
        Note tonic = null;
        double tonicFrequency = -1;
        try {
          int noteNumber = modulation.noteNumber;
          tonic = modulator.midiNoteNumberToNote(noteNumber);
          tonicFrequency = modulator.frequency(tonic);
        } catch(UnsupportedNoteException e) {
          e.printStackTrace();
          System.exit(-1);
        } 
        modulator.modulate(tonic);
      }
    }

    /**
     *  Carries out a given subdominant directive.
     *  @param   directive
     *           the subdominant directive
     */
    private void performSubdominantDirective(SubdominantDirective directive) {
      if (this.intonation instanceof Modulator) {
        Modulator modulator = (Modulator)this.intonation;
        boolean subdominant = directive.on;
        modulator.setSubdominant(subdominant);
      }
    }

    /**
     *  Reverts to original key and tonic frequency.
     */
    private void revert() {
      if (this.intonation instanceof Modulator) {
        ((Modulator)this.intonation).revert();
      }
    }

    /**
     *  Makes the item before the current item current and returns it.
     *  @result   previous item in the timed item list (or first
     *            item if are at beginning of list)
     */
    private Item previousItem() {

      // if not at start of list, back up one and get
      // new current item, making its time the current time
      Item current = null;
      if (this.itemIndex > 0) {
        this.itemIndex--;
        current = (Item)this.timedItems.get(this.itemIndex); 
        this.currentTime = current.timestamp;
      }

      // return new current (former previous) item 
      // or null if at start of list 
      return current;
    }

    /**
     *  Moves the current position in the file to
     *  the position corresponding to the given time.
     *  @param   time
     *           time from begining of file (nanoseconds)
     */
    void seek(long time) {
      System.out.println("seek: time = " + time);

      // clamp seek time at zero
      time = Math.max(0, time);

      // remember current position
      long oldTime = this.currentTime;

      // search for item with given time
      int index = Collections.binarySearch(this.timedItems, 
                                           new Item(time));

      // if such an item was found, make sure are at the first item
      // with that time
      if (index >= 0) {
        this.itemIndex = index;
        Item prev = peek(-1);
        while (prev != null && prev.timestamp == time) {
          this.itemIndex--;
          prev = peek(-1); 
        }

      // if no item with that time was found, the current position
      // is before the first item with a time greater than the
      // given time (or at end of list)
      } else {
        this.itemIndex = -index-1;
      }

      // set the current time
      if (this.itemIndex < this.timedItems.size()) {
        Item current = (Item)this.timedItems.get(this.itemIndex);
        this.currentTime = current.timestamp;
      }

      // now adjust the tuning for the new position: first revert to
      // original key and tonic frequency, then apply every tuning
      // request, in order, up to current item
      revert();
      for (int i = 0; i < this.itemIndex; i++) {
        Item item = (Item)this.timedItems.get(i);
        if (item instanceof Modulation) {
          performModulation((Modulation)item);
        } else if (item instanceof SubdominantDirective) {
          performSubdominantDirective((SubdominantDirective)item);
        } 
      }
    }

    /**
     *  Deletes the current item.
     */
    private void delete() {

      // remove current item (provided are not at end of list)
      if (this.itemIndex < this.timedItems.size()) {
        this.timedItems.remove(this.itemIndex);
      }

      // current time is now time of new current item (unless at
      // end of list, then it is infinity)
      if (this.itemIndex < this.timedItems.size()) {
        this.currentTime = 
          ((Item)this.timedItems.get(this.itemIndex)).timestamp;
      } else {
        this.currentTime = Long.MAX_VALUE;
      }
    }

    /**
     *  Inserts a modulation before the current item (leaving
     *  the current item the same).
     *  @param   noteNumber
     *           MIDI note number of tonic of key to modulate to
     */
    private void insertModulation(int noteNumber) {

      // create Modulation item, giving it the timestamp
      // of the current item, and insert it before the
      // current item (provided not at end of list)
      if (this.itemIndex < this.timedItems.size()) {
        Item current = (Item)this.timedItems.get(this.itemIndex);
        long timestamp = current.timestamp;
        Modulation modulation = new Modulation(noteNumber, timestamp);
        this.timedItems.add(this.itemIndex, modulation);
      }
      // if at end of list, don't insert anything
    }

    /**
     *  Inserts a subdominant directive before the current item (leaving
     *  the current item the same).
     *  @param   subdominant
     *           true means subdominant tuning is in effect
     */
    private void insertSubdominantDirective(boolean subdominant) {

      // create subdominant directive item, giving it the timestamp
      // of the current item, and insert it before the
      // current item (provided not at end of list)
      if (this.itemIndex < this.timedItems.size()) {
        Item current = (Item)this.timedItems.get(this.itemIndex);
        long timestamp = current.timestamp;
        SubdominantDirective directive = 
          new SubdominantDirective(subdominant, timestamp);
        this.timedItems.add(this.itemIndex, directive);
      }
      // if at end of list, don't insert anything
    }

    /**
     *  Inserts a NoteOn or NoteOff message at the indicated
     *  time at makes it the current item.
     */
    private void insertNoteMessage(long timestamp, 
                                   int noteNumber, 
                                   int velocity) {

      // make a NoteOn or NoteOff message
      // (note that the channel doesn't matter, since the
      // TuningReceiver allocates the channels)
      ShortMessage message = new ShortMessage();
      try {
        if (velocity > 0) {
          message.setMessage(ShortMessage.NOTE_ON,
                             0,
                             noteNumber,
                             velocity & 0x7f);
        } else {
          message.setMessage(ShortMessage.NOTE_OFF,
                             0,
                             noteNumber,
                             0);
        }
      } catch(InvalidMidiDataException e) {
        System.err.println("Should not happen");
        e.printStackTrace();
      }

      // find the item following the new item (or end of list)
      int index = Collections.binarySearch(this.timedItems, 
                                           new Item(timestamp));
      if (index < 0) {
        index = -index-1;
      }

      // insert the new item
      Item item = new Message(message, timestamp); 
      this.timedItems.add(index, item);

      // make it the current item 
      this.itemIndex = index;
      this.currentTime = timestamp;
    }

    /**
     *  Returns the item at the given offset from the current item.
     *  @param   offset
     *           -1 means the item before the current item,
     *           1 means the item after the current item, etc.
     *  @result  item at given offset (null if none)
     */ 
    private Item peek(int offset) {
      Item item = null;
      if (offset < 0) {
        item = peekBack(-offset);
      } else if (offset > 0) {
        item = peekForward(offset);
      } else {
        item = (this.itemIndex < this.timedItems.size() ?
                (Item)this.timedItems.get(this.itemIndex) : null);
      }
      return item; 
    }

    /**
     *  Returns the item at the given offset from the current item.
     *  @param   offset
     *           positive offset: 1 means next item, etc.
     *  @result  item at given offset (null if none)
     */ 
    private Item peekForward(int offset) {
      Item item = null;
      int index = this.itemIndex + offset;
      if (index < this.timedItems.size()) {
        item = (Item)this.timedItems.get(index);
      }
      return item; 
    }

    /**
     *  Returns the item at the given offset from the current item.
     *  @param   offset
     *           positive offset: 1 means previous item, etc.
     *  @result  item at given offset (null if none)
     */ 
    private Item peekBack(int offset) {
      //System.out.println("peekBack " + offset);
      Item item = null;
      int index = this.itemIndex - offset;
      if (index >= 0) {
        item = (Item)this.timedItems.get(index);
      }
      return item;
    }

    /**
     *  Returns the time at the current position in the timed items.
     *  @result  current time
     */
    private long getCurrentTime() {
      return this.currentTime;
    }

    /**
     *  Displays items around the current item.
     *  @param   depth
     *           number of items before and after current item
     *           to display (2*depth+1 items displayed)
     */
    private void display(int depth) {
      for (int i = -depth; i <= -1; i++) {
        Item item = peek(i);
        if (item != null) {
          item.display(false);
        }
      }
      if (this.itemIndex < this.timedItems.size()) {
        ((Item)this.timedItems.get(this.itemIndex)).display(true); 
      }
      for (int i = 1; i <= depth; i++) {
        Item item = peek(i);
        if (item != null) {
          item.display(false);
        }
      }
    }

    /**
     *  Displays the current position in the items.
     */
    private void displayPosition() {
      int size = this.timedItems.size();
      if (this.itemIndex < size) {
        System.out.println("At item " + this.itemIndex + " of " + size);
        System.out.println("Item time " + this.currentTime + 
          " of " + this.timedItems.get(size-1).timestamp);
      } else {
        System.out.println("At end of items");
        System.out.println("Total items " + size + 
          ", total time " + this.timedItems.get(size=1).timestamp);
      }
    }
  }/** end of static inner class TimedItems */

  /**
   *  Static inner class <tt>TuningStatus</tt> contains
   *  the tuning status.
   */
  private static class TuningStatus {  
    Note key;
    double tonicFrequency;
    boolean subdominant;
    TuningStatus(Note key, double tonicFrequency, boolean subdominant) {
      this.key = key;
      this.tonicFrequency = tonicFrequency;
      this.subdominant = subdominant;
    }
    public String toString() {
      return "TuningStatus: key " + this.key + 
        " tonic frequency " + this.tonicFrequency + 
        " subdominant " + this.subdominant;
    }
  }/** end of static inner class TuningStatus */

  /**
   *  Static inner class <tt>Item</tt> is the Base class 
   *  for the items in an <tt>IntuneFile</tt>.  
   */
  private static class Item implements Comparable<Item> {  

    // time (since start) in nanoseconds
    protected long timestamp;

    /**
     *  Constructor.
     *  @param   timestamp
     */
    private Item(long timestamp) {
      this.timestamp = timestamp;
    }

    /**
     *  Writes the <tt>Item</tt> to the given stream.
     *  @param   out
     *           the output stream
     */
    void write(DataOutputStream out) throws IOException {  }

    /** 
     *  Displays the <tt>Item</tt> on the standard output device
     *  (to be overridden by derived class).
     *  @param   marked
     *           if true, distinguish the display with a mark
     */
    void display(boolean marked) {  }

    /**
     *  Returns a negative, zero or positive result depending
     *  on whether the lhs is less than, equal to or greater
     *  then the rhs (needed for binarySearch).
     *  @param   lhs
     *  @param   rhs
     *  @see Comparable
     */
    public int compareTo(Item lhs) {
      //System.out.println("compareTo: " + 
      //  lhs.getClass().getName() + " " + this.getClass().getName());
      return (this.timestamp < lhs.timestamp ?  -1 :
             (this.timestamp == lhs.timestamp ?  0 :
                                                 1));
    }
  }/** end of static inner class Item */

//  /**
//   *  Static inner class <tt>ComparisonItem</tt> is a descendant
//   *  of <tt>Item</tt> that is used as a <tt>binarySearch</tt> argument.
//   */
//  private static class ComparisonItem extends Item {
//    /**
//     *  Constructor.
//     */
//    ComparisonItem(long timestamp) {
//      super(timestamp);
//    }
 
//    /**
//     *  Satisfies abstract method.
//     */
//    void write(DataOutputStream out) throws IOException {  }
//    
//    /**
//     *  Satisfies abstract method.
//     */
//    void display(boolean marked) {  }
//  }/** end of static inner class ComparisonItem */ 

  /**
   *  Static inner class <tt>Message</tt> contains a MIDI message 
   *  and a timestamp giving the time of issuance of the message.
   */
  static class Message extends Item {
    ShortMessage message;

    /**
     *  Constructor.
     *  @param   message
     *           Midi message
     *  @param   timestamp
     *           time at which message is issued
     */
    Message(ShortMessage message, long timestamp) {
      super(timestamp);
      this.message = message;
    }

    /**
     *  Writes the message to an output stream.
     *  @param   out 
     *           the output stream
     */
    void write(DataOutputStream out) throws IOException {
      out.writeByte(this.message.getCommand());
      out.writeByte(this.message.getChannel());
      out.writeByte(this.message.getData1());
      out.writeByte(this.message.getData2());
      out.writeLong(this.timestamp);
    }

    /**
     *  Displays the <tt>Message</tt> on the standard output device.
     *  @param   marked
     *           if true, distinguish the display with a mark
     */
    void display(boolean marked) {
      String descrip = null;
      if (Midi.isNoteOn(message)) {
        descrip = "NoteOn note# " + message.getData1() + 
                    " velocity " + message.getData2();
      } else if (Midi.isNoteOff(message)) {
        descrip = "NoteOff note# " + message.getData1();
      } else if (message.getCommand() == ShortMessage.PITCH_BEND) {
        descrip = "Pitch Bend " + 
		       message.getData1() + " " + message.getData2();
      } else {
        descrip = "" +message.getCommand() + " " + message.getChannel() +
		  " " + message.getData1() + " " + message.getData2();
      }
      System.out.println("Message: " + descrip + (marked ? " ***" : ""));
    }
  }/** end of static inner class Message */

  /**
   *  Class <tt>Modulation</tt> contains a request 
   *  for a modulation.
   */
  private static class Modulation extends Item {

    // MIDI note number of target of modulation
    int noteNumber;

    /**
     *  Constructor.
     *  @param   noteNumber
     *           MIDI note number of note to modulate to
     *  @param   timestamp
     *           time in nanoseconds
     */
    Modulation(int noteNumber, long timestamp) {
      super(timestamp);
      this.noteNumber = noteNumber;
    }

    /**
     *  Writes the modulation to an output stream.
     *  @param   out 
     *           the output stream
     */
    void write(DataOutputStream out) throws IOException {
      out.writeByte((byte)ShortMessage.TUNE_REQUEST);
      out.writeByte((byte)Constants.TuneRequest.MODULATION);
      out.writeByte((byte)this.noteNumber);
      out.writeLong(this.timestamp);
    }

    /**
     *  Displays the <tt>Modulation</tt> on the standard output device.
     *  @param   marked
     *           if true, distinguish the display with a mark
     */
    void display(boolean marked) {
      System.out.println("Modulation: " +
        " to note number " + this.noteNumber + (marked ? " ***" : ""));
    }
  }/** end of static inner class Modulation */

  /**
   *  Static inner class <tt>SubdominantDirective</tt> contains 
   *  a subdominant directive.
   */
  private static class SubdominantDirective extends Item {
    boolean on;

    /**
     *  Constructor.
     *  @param   on
     *           turn subdominant tuning on if true, off if false
     *  @param   timestamp
     *           time in nanoseconds
     */
    SubdominantDirective(boolean on, long timestamp) {
      super(timestamp);
      this.on = on;
    }

    /**
     *  Writes the subdominant directive to an output stream.
     *  @param   out 
     *           the output stream
     */
    void write(DataOutputStream out) throws IOException {
      out.writeByte((byte)ShortMessage.TUNE_REQUEST);
      out.writeByte((byte)Constants.TuneRequest.SUBDOMINANT);
      out.writeBoolean(this.on);
      out.writeLong(this.timestamp);
    }

    /**
     *  Displays the <tt>SubdominantDirective</tt> on the 
     *  standard output device.
     *  @param   marked
     *           if true, distinguish the display with a mark
     */
    void display(boolean marked) {
      System.out.println("SubdominantDirective: " +
        (this.on ? "on" : "off") + (marked ? " ***" : ""));
    }
  }/** end of static inner class SubdominantDirective */

}/** end of class IntuneFile */

