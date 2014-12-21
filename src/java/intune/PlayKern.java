
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

/** 
 * This module is left over from a previous version that
 * accepted input in the kern musical notation and played 
 * it to output sound.
 */

// How do I coordinate pauses?  All columns needn't
// contains a pause.

// If a section is repeated in an expansion list, it must
// presumably use the second version of any measures
// with version letters, etc.  

// Shall I develop through-composed versions of each spine
// when I encounter an expansion list, or shall I do it
// on the fly when I play it?  I favor the former, even
// though it will take a little more memory.  It has the
// advantage of requiring less processing during play.

// Should I pay any attention to barline numbers, or just
// to the letters?  I could require them to increment by
// 1 (except for alternate versions) and start from 1, for 
// instance, and I could require that all barline numbers
// and letters in a row be equal.  Should I allow fewer than
// all the spines to have alternate versions of a measure?
// (Then I guess the other spines would have placeholders.)

// Things that could be handled in the syntactical or
// semantic part of the translation but are not now:
//   coordination of pauses  
//   succession of barline numbers
//   matching barline numbers in all spines
//   succession of barline letters
//       if don't require them to be in order, just take next alternate
//   matching barline letters in all spines
//       if don't require them, just repeat same measure

// Suppose a section occurs more than once: *[A,B,A,B].
// When run out of alternates, do I just start over with first alternate?

// I've got a better idea: for now, don't bother about barline
// numbering or alternate measures at all.  Any results I want
// I can get with section names and expansion lists, anyway.
// Regard the numbers and letters merely as comments.

package intune;
import java.io.*;
import java.util.*;
import javax.sound.midi.*;

/**
 *  Class <code>PlayKern</code> plays a <code>**kern</code> file.
 */
public class PlayKern
  {
  /** MIDI ticks per quarter note (tempo resolution) */
  private static final int TICKS_PER_QUARTER = 384;

  /** MIDI clocks per quarter note */
  private static final int MIDI_CLOCKS_PER_QUARTER = 24;

  /** default meter signature components: 4/4 */
  private static final int DEFAULT_BEATS_PER_MEASURE = 4;
  private static final int DEFAULT_BEAT = 4;

  /** default metronome marking (beats per minute) */
  private static final double DEFAULT_METRONOME_MARKING = 66;

  /** the number 32nd notes per 24 MIDI clocks */
  private static final int DEMISEMIQUAVERS_PER_24_MIDI_CLOCKS = 8;

  /** MIDI meta-event types and lengths */
  private static final int TEMPO = 0x51;
  private static final int TEMPO_LENGTH = 4;
  private static final int TIME_SIGNATURE = 0x58;
  private static final int TIME_SIGNATURE_LENGTH = 4;

  /** MIDI note range */
  private static final int MIN_MIDI_NOTE = 0;
  private static final int MAX_MIDI_NOTE = 127;

  /** MIDI note numbers */
  private static final int MIDI_MIDDLE_C = 60;
  private static final int MIDI_CONCERT_A = 69;

  /** default MIDI velocity--controls loudness */
  private static final int DEFAULT_MIDI_VELOCITY = 93;

  /** multiplier that gives frequency to add to rise an 
   *  equal-temperament semitone */
  private static final double MIDI_SEMITONE = Math.pow(2., 1./12.) - 1;

  /** the spines from the kern input file */
  private Spine[] spines;

  /** the spine currently being interpreted */
  private SpineScanner currentSpine;

  /** (index in spine) vs. (section name) from kern input file */
  private HashMap sections = new HashMap();

  /** the current meter signature */
  private Meter meter = 
      new Meter(DEFAULT_BEATS_PER_MEASURE, DEFAULT_BEAT);

  /** the current metronome marking */
  private double metronomeMarking = DEFAULT_METRONOME_MARKING;

  /** the intonation type (defaults to commonest low denominator) */
  private Intonation intonation = new EqualTemperament();

  /** equal temperament intonation (for use with MIDI notes) */
  private Intonation equalTemperament = new EqualTemperament();

  /** Pythagorean intonation (for use in setting diatonic key) */
  private Intonation pythagorean = new Pythagorean();

  /** time in current track (initially 0 for each track) */
  private long currentTick;

  /** the timbre (soundbank program number) to be used
   *  for the respective spines (null if default) */
  private int[] timbre;

private Note testNote;

  /**
   *  Runs the <code>PlayKern</code> program.
   *  @param   args   command-line arguments: args[0] contains
   *                  the name of a <code>**kern</code> file
   *                  and args[1..] contain the soundbank program
   *                  numbers for the timbres to be used for the
   *                  respective spines of the <code>**kern</code>
   *                  file or the program number to be used for
   *                  all the spines if there is just one
   */
  static public void main (String[] args)
    {
    int[] timbre = null;
    try
      {
      if (args.length < 1)
        {
        throw new Exception();
        }
      else if (args.length > 1)
        {
        timbre = new int[args.length-1];
        for (int i = 0; i < timbre.length; i++)
          {
          timbre[i] = Integer.parseInt(args[i+1]);
          }
        }
      }
    catch (Exception e) 
      {
      System.err.println(
        "Usage: java PlayKern filename [instr# instr# ...]");
      System.exit(-1);
      }
    new PlayKern(args[0], timbre);
    }

  /**
   *  Constructor.
   *  @param  kernFileName  name of the **kern file to play
   *  @param  timbre        program numbers of the MIDI instruments
   *                        to use for the respective **kern spines
   *                        or (if there is just one timbre) the
   *                        program number to be used for all the spines
   */
  private PlayKern(String kernFileName, int[] timbre)
    {
    try
      {
      // save the timbres
      this.timbre = timbre;

      // load the timbre patches
      loadInstruments();

      // read all the lines of the file and turn them into spines
      BufferedReader in = new BufferedReader(new FileReader(kernFileName));
      String line = null;
      while ((line = in.readLine()) != null)
        {
        extractSpines(line);
        }

      // make sure the validity rules for rows are obeyed
      checkValidityOfRows();

      // get the sequencer
      Sequencer sequencer = MidiSystem.getSequencer();
      sequencer.open();

      // build a sequence from the spines and give it to the sequencer
      Sequence sequence = buildSequence(TICKS_PER_QUARTER);
System.out.println("sequence built");

      // give the sequence to the sequencer and start it
      sequencer.setSequence(sequence);
      sequencer.start();
      }
    catch (Exception e)
      {
      e.printStackTrace();
      }
    }

  /**
   *  Extract the spines from a line of the input **kern file.
   *  @param   line   the line
   *  @throws  InvalidSyntaxException  if spine syntax is invalid
   */
  private void extractSpines(String line) throws InvalidSyntaxException
    {
System.out.println(line);
    // skip global comment lines 
    if (line.startsWith("!!"))
      ;

    else
      {
      // put line in tokenizer with tab delimiters
      final int HT = 9;
      char[] delimiters = new char[] { HT };
      StringTokenizer tokens =
          new StringTokenizer(line, new String(delimiters));

      // if this is the first (non-comment) line,
      // count the spines and allocate them
      if (this.spines == null)
        {
        int n = tokens.countTokens();
        this.spines = new Spine[n];
        for (int i = 0; i < this.spines.length; i++)
          {
          spines[i] = new Spine(i);
          }
        }

      // construct a row of tokens, one per spine
      String[] row = new String[this.spines.length];
      int i = 0;
      for (; tokens.hasMoreTokens() && i < row.length; i++)
        {
        row[i] = tokens.nextToken();
        }

      // each row must have the same number of spines
      if (i < row.length)
        {
        throw new InvalidSyntaxException("Too few spines in line: " + line);
        }
      else if (tokens.hasMoreTokens())
        {
        throw new InvalidSyntaxException("Too many spines in line: " + line);
        }

      // if row is not a tandem comment, add it to the spines
      if (noneStartsWith(row, "!"))
        {
        for (int j = 0; j < spines.length; j++)
          {
          this.spines[j].addElement(row[j]);
          }
        }

      // check for valid tandem comment
      else if (!allStartWith(row, "!"))
        {
        throw new InvalidSyntaxException(
          "If one column is a tandem comment, all columns must be:" 
          + line);
        }

      }//if not comment
    }/** end of extractSpines */

  /**
   *  Make sure that the spines obey the validity rules for rows.
   *  @throws  InvalidSyntaxException  if row syntax is invalid
   */
  private void checkValidityOfRows() throws InvalidSyntaxException
    {
    // make an iterator for each spine
    SpineScanner[] spines = new SpineScanner[this.spines.length];
    for (int i = 0; i < spines.length; i++)
      {
      spines[i] = this.spines[i].scanner(); 
      }

    // for each row in order (using the fact 
    // that all spines are of same length)..
    while (spines[0].more())
      {
      // build the row
      String[] row = new String[spines.length];
      for (int i = 0; i < row.length; i++)
        {
        spines[i].advance();
        row[i] = spines[i].current(); 
        }

      // check the row's validity
      checkRowValidity(row);
      }
    }

  /**
   *  Returns true if row is valid.
   *  @param   row   the row
   *  @throws  InvalidSyntaxException  if row syntax is invalid
   */
  private void checkRowValidity(String[] row) throws InvalidSyntaxException
    {
    // we'll check rowwise validity for directives and barlines
    boolean directivesOk = false;
    boolean barlinesOk = false;

    // if any column contains a directive, all must, and the
    // directives must be consistent with one another
    directivesOk = noneStartsWith(row, "*")
                   || (allStartWith(row, "*")
                       && consistentDirectives(row));
    if (!directivesOk)
      {
      throw new InvalidSyntaxException(
        "Inconsistent directives in row " + rowToString(row));
      }

    // if any column contains a barline, all must
    barlinesOk = (noneStartsWith(row, "=")
                  || allStartWith(row, "="));
    if (!barlinesOk)
      {
      throw new InvalidSyntaxException(
        "If one column has barline, all must in row " + rowToString(row));
      }  
    }

  /**
   *  Converts a row to a string.
   *  @param   row   the row
   *  @result  string representing the row
   */
  private String rowToString(String[] row)
    {
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < row.length; i++)
      {
      buf.append(row[i] + " ");
      }
    return buf.toString();
    }

  /**
   *  Returns true if no element in the given string array
   *  starts with the given pattern.
   *  @param   s         the string array
   *  @param   pattern   the pattern
   *  @result  true if no element starts with pattern
   */
  private boolean noneStartsWith(String[] s, String pattern)
    {
    int i = 0;
    for (; i < s.length && !s[i].startsWith(pattern); i++);
    return (i == s.length); 
    }

  /**
   *  Returns true if all elements in the given string array
   *  start with the given pattern.
   *  @param   s         the string array
   *  @param   pattern   the pattern
   *  @result  true if no element starts with pattern
   */
  private boolean allStartWith(String[] s, String pattern)
    {
    int i = 0;
    for (; i < s.length && s[i].startsWith(pattern); i++);
    return (i == s.length); 
    }

  /**
   *  Returns true if all elements in the given string array
   *  are the same.
   *  @param   s         the string array
   *  @result  true if all elements are equal
   */
  private boolean allAreEqual(String[] s)
    {
    int i = 1;
    for (; i < s.length && s[i].equals(s[i-1]); i++);
    return (i >= s.length);
    }

  /**
   *  Returns true if all columns of a row contain directives
   *  that are consistent with one another.<p>
   *  It is assumed that all columns of the row contain
   *  a directive.
   *  @param   row   the row
   *  @result  true if all elements of the row consistent
   */
  private boolean consistentDirectives(String[] row)
    {
    boolean consistent = true;
    // It so happens that all of the non-interpretation directives
    // we recognize must be identical in each column; if it's
    // some non-interpretation directive we don't recognize,
    // we don't care about it.  The only interpretation recognized
    // is **kern, and it must be the same in all columns; all
    // other interpretations we don't care about.

    // if it's not an interpretation but is a directive we
    // recognize, then it must be the same in each column
    if (!row[0].substring(0,2).equals("**"))
      {
      if (isRecognizedDirective(row[0]))
        {
        consistent = allAreEqual(row);
        }
      }

    // if it's the **kern interpretation, then it must be
    // the same in all columns
    else if (row[0].equals("**kern"))
      {
      consistent = allAreEqual(row);
      }

    // return true if columns consistent
    return consistent;
    }

  /**
   *  Returns true if a given directive is one we recognize.
   *  @param   s   the directive
   *  @result  true if it's a directive we recognize
   */
  private boolean isRecognizedDirective(String s)
    {
    // we recognize: 
    //   all interpretations (and ignore them, except **kern)
    //   sections names
    //   expansion lists
    //   meter signatures
    //   metronome markings
    //   intonation types (this one's ours)
    //   modulations (so's this)
    //   end of spine marks
    boolean recognized = (isDirective(s)
                          && (isInterpretation(s)
                              || isSectionName(s)
                              || isExpansionList(s)
                              || isMeterSignature(s)
                              || isMetronomeMarking(s)
                              || isIntonationType(s)
                              || isModulation(s)
                              || isSpineTerminator(s)));

    // return true if recognized directive 
    return recognized;
    }

  /**
   *  Returns true if given string is a directive.
   *  @result  true if directive
   */
  private boolean isDirective(String s)
    {
    return s.startsWith("*");
    }

  /**
   *  Returns true if given string is an interpretation.
   *  @result  true if interpretation
   */
  private boolean isInterpretation(String s)
    {
    return s.startsWith("**");
    }

  /**
   *  Returns true if given string is a section name.
   *  @result  true if section name
   */
  private boolean isSectionName(String s)
    {
    return s.startsWith("*>");
    }

  /**
   *  Returns true if given string is an expansion list.
   *  @result  true if expansion list
   */
  private boolean isExpansionList(String s)
    {
    return s.startsWith("*[");
    }

  /**
   *  Returns true if given string is a meter signature.
   *  @result  true if meter signature
   */
  private boolean isMeterSignature(String s)
    {
    return (s.startsWith("*M")
           && isDigit(s.charAt(2)));
    }

  /**
   *  Returns true if given string is a metronome marking.
   *  @result  true if metronome marking
   */
  private boolean isMetronomeMarking(String s)
    {
    return s.startsWith("*MM");
    }

  /**
   *  Returns true if given string is an intonation type.
   *  @result  true if intonation type
   */
  private boolean isIntonationType(String s)
    {
    return s.startsWith("*i");
    }

  /**
   *  Returns true if given string is a modulation.
   *  @result  true if modulation
   */
  private boolean isModulation(String s)
    {
    return s.startsWith("*m");
    }

  /**
   *  Returns true if given string is a key signature.
   *  @result  true if key signature
   */
  private boolean isKeySignature(String s)
    {
    return s.startsWith("*k");
    }

  /**
   *  Returns true if given string is a spine terminator.
   *  @result  true if spine terminator
   */
  private boolean isSpineTerminator(String s)
    {
    return s.startsWith("*-");
    }

  /**
   *  Constructs a MIDI sequence containing a track for
   *  each **kern spine and returns it.
   *  @result  the sequence
   *  @throws  InvalidSyntaxException
   *           if the syntax of the input spines is invalid
   *  @throws  UnsupportedNoteException
   *           if the input spines contain a note outside MIDI range
   */
  private Sequence buildSequence(int ticksPerSecond)
        throws InvalidSyntaxException,
               UnsupportedNoteException
    {
    // make a new sequence
    Sequence sequence = null;
    try
      {
      sequence = new Sequence(Sequence.PPQ, ticksPerSecond);
      }
    catch (Exception e)
      {
      e.printStackTrace();
      }

    // for each spine..
    for (int i = 0; i < this.spines.length; i++)
      {
      // create a track in the sequence for the spine
      Track track = sequence.createTrack();

      // convert the spine to MIDI events
      // and append them to the track
      buildTrack(this.spines[i].scanner(), track);
System.out.println("track " + i + " built");
      }

    // return the sequence
    return sequence;
    } /** end of buildSequence */

  /**
   *  Converts a kern spine to MIDI events and
   *  appends them to a given track.
   *  @param   spine  a scanner for the kern spine
   *  @param   track  the track
   *  @throws  InvalidSyntaxException
   *           if the syntax of the spine is invalid
   *  @throws  UnsupportedNoteException
   *           if the spine contains a note outside the MIDI range
   */
  private void buildTrack(SpineScanner spine, Track track)
        throws InvalidSyntaxException,
               UnsupportedNoteException
    {
    // set eleapsed track time to zero
    this.currentTick = 0;

    // output default meter and metronome marking
    outputDefaults(track);

    // set the timbre to be used for this spine
    setTimbre(spine.getSpineNumber(), track);

    // provided spine is non-empty..
    if (spine.more())
      {
      // make first token current
      spine.advance();

      // consume the **kern interpretation directive
      if (spine.current().equals("**kern"))
        {
        spine.advance();
        }   
      else
        {
        System.err.println("Warning: **kern interpretation should " +
          "be the first thing in each spine");
        }

      // interpret the rest of the elements of the spine
      interpretSpine(spine, track);

      // spine should end with terminator
      if (!spine.current().equals("*-"))
        {
        System.err.println("Warning: *- spine terminator should " +
          "be the last thing in each spine");
        }
      }
    }

  /**
   *  Set the timbre to be used for a given spine.
   *  @param   spineNumber  number of the spine
   *  @param   track        track corresponding to the spine
   */
  private void setTimbre(int spineNumber, Track track)
    {
System.out.println("***** setTimbre");
    // if no timbres were specified, just use the default,
    // otherwise..
    if (this.timbre != null)
      {
      // if timbre specification is valid..
      if (this.timbre.length == 1
          || spineNumber < this.timbre.length)
        {
        // if only one timbre was specified, use it for all spines,
        // otherwise use the timbre specified for this spine
        int programNumber = (this.timbre.length == 1 ?
                             this.timbre[0] :
                             this.timbre[spineNumber]);

        // insert a program change message into the track,
        // using the spine number as the channel and
        // a MIDI bank number of zero
        try
          {
          ShortMessage message = new ShortMessage();
          message.setMessage(ShortMessage.PROGRAM_CHANGE,
                             spineNumber,
                             programNumber, 0);
          addToTrack(message, 0, track);
System.out.println("Added PROGRAM CHANGE " + programNumber +
" to track " + spineNumber);
          }
        catch (InvalidMidiDataException e) 
          {
          System.err.println("Should not happen");
          e.printStackTrace();
          System.exit(-1);
          }
        }
      else
        {
        System.err.println("Warning: more spines than timbres: " +
          "not setting timbre for spine # " + spineNumber);
        }
      }
    }

  /**
   *  Load the patches needed for the timbres used in this
   *  performance.
   */
  private void loadInstruments()
    {
    final String filename = 
        "d:\\jdk1.3\\jre\\lib\\audio\\soundbank.gm.orig";
    if (this.timbre != null)
      {
      try
        {
        // get the synthesizer and open it if necessary
        Synthesizer synth = MidiSystem.getSynthesizer();
        if (!synth.isOpen())
          {
          synth.open();
          }

        // read the external soundbank file
        Soundbank soundbank = MidiSystem.getSoundbank(new File(filename));

        // build a description of each patch needed
        Patch[] patch = new Patch[this.timbre.length];
        for (int i = 0; i < patch.length; i++)
          {
          patch[i] = new Patch(0, this.timbre[i]);
          }

        // load the patches from the soundbank into the synthesizer
        synth.loadInstruments(soundbank, patch);
        }
      catch (Exception e)
        {
        System.err.println("Cannot load instruments");
        e.printStackTrace();
        }
      }
    }

  /**
   *  Converts a kern spine to MIDI events and
   *  appends them to a given track.
   *  @param   spine          a scanner for the kern spine
   *  @param   track          the MIDI track
   *  @throws  InvalidSyntaxException
   *           if the spine syntax in invalid
   *  @throws  UnsupportedNoteException
   *           if the spine contains a note outside the MIDI range
   */
  private void interpretSpine(SpineScanner spine, Track track)
        throws InvalidSyntaxException,
               UnsupportedNoteException
    {
    while (spine.more())
      {
      interpretElements(spine, track);
      }
    }

  /**
   *  Converts one or more **kern spine elements to MIDI
   *  events and appends them to a given track.
   *  @param   spine          
   *           a scanner for the kern spine
   *  @param   track          
   *           the MIDI track
   *  @throws  InvalidSyntaxException
   *           if note syntax is invalid
   *  @throws  UnsupportedNoteException
   *           if note is outside the MIDI range
   */
  private void interpretElements(SpineScanner spine, Track track)
        throws InvalidSyntaxException,
               UnsupportedNoteException
    {
    String current = spine.current();
    if (isDirective(current))
      {
      interpretDirective(spine, track);
      }
    else if (isNote(current))
      {
      interpretNote(spine, track);
      }
    else if (isRest(current))
      {
      interpretRest(spine, track);
      }
    else if (isBarline(current))
      {
      interpretBarline(spine, track);
      }
    else if (isPlaceholder(current))
      {
      interpretPlaceholder(spine, track);
      }
    else
      {
//    System.err.println("Warning: ignoring unrecognized spine element: " + 
      System.out.println("Warning: ignoring unrecognized spine element: " + 
        current);
      spine.advance();
      }
    }

  /**
   *  Returns true if given spine element is note.
   *  @param   element   the element
   *  @result  true if element is note
   */
  private boolean isNote(String element)
    {
    // element is a note if it starts with a phrase opening,
    // or a slur opening, a tie opening, or a pitch letter,
    // or if it starts with a duration and is not a rest
    return (element.startsWith("{")
            || element.startsWith("(")
            || element.startsWith("[")
            || isPitchLetter(element.charAt(0))
            || (isDigit(element.charAt(0))
                && element.indexOf('r') < 0));
    }

  /**
   *  Returns true if given spine element is rest.
   *  @param   element   the element
   *  @result  true if element is rest
   */
  private boolean isRest(String element)
    {
    return (element.startsWith("r")
            || (isDigit(element.charAt(0))
                && element.indexOf('r') >= 0));
    }

  /**
   *  Returns true if given spine element is barline.
   *  @param   element   the element
   *  @result  true if element is barline
   */
  private boolean isBarline(String element)
    {
    return element.startsWith("=");
    }

  /**
   *  Returns true if given spine element is placeholder.
   *  @param   element   the element
   *  @result  true if element is placeholder
   */
  private boolean isPlaceholder(String element)
    {
    return element.equals(".");
    }

  /**
   *  Returns true if given character is a base-10 digit.
   *  @param   c   the character
   *  @result  true if character is digit
   */
  private boolean isDigit(int c)
    {
    return Character.isDigit((char)(c & 0xff));
    }

  /**
   *  Interprets a **kern directive, adding MIDI events
   *  to a given track if appropriate.
   *  @param   spine        
   *           spine scanner with the directive at its current position
   *  @param   track        
   *           the track
   *  @throws InvalidSyntaxException
   *          if the syntax of the directive is invalid
   *  @throws UnsupportedNoteException
   *          if the directive contains a note outside MIDI range
   */
  private void interpretDirective(SpineScanner spine, Track track)
        throws InvalidSyntaxException,
               UnsupportedNoteException
    {
    String current = spine.current();
    if (isInterpretation(current))
      {
      interpretInterpretation(spine, track);
      }
    else if (isSectionName(current))
      {
      interpretSection(spine, track);
      }
    else if (isExpansionList(current))
      {
      interpretExpansionList(spine, track);
      }
    else if (isMeterSignature(current))
      {
      interpretMeterSignature(spine, track);
      }
    else if (isMetronomeMarking(current))
      {
      interpretMetronomeMarking(spine, track);
      }
    else if (isIntonationType(current))
      {
      interpretIntonationType(spine, track);
      }
    else if (isModulation(current))
      {
      interpretModulation(spine, track);
      }
    else if (isKeySignature(current))
      {
      interpretKeySignature(spine, track);
      }
    else if (isSpineTerminator(current))
      {
      interpretSpineTerminator(spine, track);
      }
    else 
      {
      System.err.println("Unrecognized directive: " + current);
      spine.advance();
      }
    }

  /**
   *  Interprets a Humdrum interpretation, adding MIDI events
   *  to a given track if appropriate.
   *  @param   spine     spine scanner with the interpretation
   *                     element at its current position
   *  @param   track     the track
   */
  private void interpretInterpretation(SpineScanner spine, Track track)
    {
    // All interpretations are ignored except for **kern, and
    // it must occur as the first element in each spine.  
    // The first element in the spine will already have been 
    // parsed if we have made it this far.
    }

  /**
   *  Interprets a **kern section, adding MIDI events
   *  to a given track if appropriate.
   *  @param   spine   spine scanner with the section
   *                   name at its current position
   *  @param   track   the track
   *  @throws  InvalidSyntaxException
   */
  private void interpretSection(SpineScanner spine, Track track)
        throws InvalidSyntaxException
    {
    // extract the section name
    String sectionName = spine.current();
    spine.advance();

    // start a new section to keep track of this section's elements
    Section section = spine.startSection(sectionName);

    // make sure there is just one section of this name
    if (this.sections.get(section.getLookupName()) != null)
      {
      throw new InvalidSyntaxException(
        "Section name " + sectionName + " may not be reused");
      }

    // advance in the spine to the end of the section
    while (!isSectionName(spine.current())
           && !isSpineTerminator(spine.current())
           && spine.more())
      {
      // if element is an expansion list, make sure it refers only
      // to sections that have already been interpreted
      if (isExpansionList(spine.current()))
        {
        checkExpansionList(spine);
        }

      // if it's anything else, no processing is required
      else
        {
        spine.advance();
        }
      }

    // store the section under its look-up name
    this.sections.put(section.getLookupName(), section);
    }

  /**
   *  Checks to ensure that an expansion list cites only sections
   *  that have already been interpreted.
   *  @param   spine   spine scanner positions at the expansion list
   *  @throws  InvalidSyntax
   */
  private void checkExpansionList(SpineScanner spine)
      throws InvalidSyntaxException
    {
    // the format of an expansion list is 
    //   "[" sectionName { "," sectionName } "]"

    // get the expansion list and advance past it
    String current = spine.current();
    spine.advance();

    // for each section name in the expansion list..
    StringTokenizer tokens = 
        new StringTokenizer(current.substring(1), ",]");
    while (tokens.hasMoreTokens())
      {
      // if no such section has been interpreted yet, it is an error
      String sectionName = tokens.nextToken();
      String lookupName = Section.getLookupName(sectionName,
                                                spine.getSpineNumber());
      if (this.sections.get(lookupName) == null)
        {
        throw new InvalidSyntaxException("Expansion list " + current +
          " names section " + sectionName + " that is not yet defined");
        }
      } 
    }

  /**
   *  Interprets a **kern expansion list, adding MIDI events
   *  to a given track if appropriate.
   *  @param   spine     a scanner for the spine, positioned at the
   *                     start of the expansion list
   *  @param   track     the track
   *  @throws  InvalidSyntaxException
   *           if the syntax of list or the sections referred 
   *           to by it is invalid
   *  @throws  UnsupportedNoteException
   *           if a section in the list contains a note
   *           outside the MIDI range
   */
  private void interpretExpansionList(SpineScanner spine, Track track)
        throws InvalidSyntaxException,
               UnsupportedNoteException
    {
    // the format of an expansion list is 
    //   "[" sectionName { "," sectionName } "]"

    // get the expansion list and advance past it
    String current = spine.current();
    spine.advance();

    // for each section name in the expansion list..
    StringTokenizer tokens = 
        new StringTokenizer(current.substring(1), ",]");
    while (tokens.hasMoreTokens())
      {
      String sectionName = tokens.nextToken();

      // fetch the section of that name in this spine
      String lookupName = Section.getLookupName(sectionName,
                                                spine.getSpineNumber());
      Section section = (Section)this.sections.get(lookupName);

      // expand the section
      expandSection(section.scanner(), track);
      } 
    }

  /**
   *  Expands a section, adding MIDI events to a given track
   *  if appropriate.
   *  @param   section   a scanner for the section
   *  @param   track     the track
   *  @throws  InvalidSyntaxException
   *           if the syntax of the section is invalid
   *  @throws  UnsupportedNoteException
   *           if the section contains a note outside MIDI range
   */
  private void expandSection(SpineScanner section, Track track)
        throws InvalidSyntaxException,
               UnsupportedNoteException
    {
    // note that nested sections are now allowed in kern,
    // so the expansion of a section can never result
    // in the (re-)interpretation of a section
    
    // get a spine scanner for just the elements in the section
    SpineScanner scanner = section.scanner();

    // interpret the elements in the section
    while (scanner.more())
      {
      interpretElements(scanner, track);
      }
    }

  /**
   *  Interprets a **kern meter signature, adding MIDI events
   *  to a given track if appropriate.
   *  @param   spine     a scanner for the spine, positioned at 
   *                     meter signature element.
   *  @param   track     the track
   *  @throws  InvalidMeterSignature  if meter signature is invalid
   */
  private void interpretMeterSignature(SpineScanner spine, Track track)
        throws InvalidSyntaxException
    {
    // the format of the meter signature is *Mn/d, e.g. *M2/4 

    // get the meter signature and advance past it
    String meter = spine.current();
    spine.advance();

    // create a tokenizer for the meter string
    StringTokenizer tokens = new StringTokenizer(meter.substring(2), "/");

    // extract the two components of the meter
    int numerator;   
    int denominator;
    try
      {
      String token = tokens.nextToken();
      numerator = Integer.parseInt(token);   
      token = tokens.nextToken();
      denominator = Integer.parseInt(token);   
      }
    catch (Exception e)
      {
      throw new InvalidSyntaxException("Invalid meter signature: " +
        meter);
      }

    // save the meter signature
    this.meter = new Meter(numerator, denominator);

    // output the meter signature
    outputMeterSignature(track);
    }

  /**
   *  Interprets a **kern metronome marking, adding MIDI events
   *  to a given track if appropriate.
   *  @param   spine     a scanner for the spine, positioned at the 
   *                     metronome marking element
   *  @param   track     the track
   *  @throws  InvalidMeterSignature  if meter signature is invalid
   */
  private void interpretMetronomeMarking(SpineScanner spine, Track track)
        throws InvalidSyntaxException
    {
    // the format of the meter signature is *MMf, where f is a
    // floating point number
    
    // get the meter signature and advance past it
    String marking = spine.current();
    spine.advance();

    // convert and save the marking
    try
      {
      this.metronomeMarking = Double.parseDouble(marking.substring(3));
      }
    catch (Exception e)
      {
      throw new InvalidSyntaxException("Invalid metronome marking: " +
        metronomeMarking);
      }

    // output the metronome marking
    outputMetronomeMarking(track);
    }

  /**
   *  Interprets an intonation type, adding MIDI events
   *  to a given track if appropriate.
   *  @param   spine     a scanner for the spine, positioned at 
   *                     the intonation type element
   *  @param   track     the track
   *  @throws  InvalidSyntaxException
   *           if the intonation type is invalid
   *  @throws  UnsupportedNoteException
   *           if the diatonic key name is invalid
   */
  private void interpretIntonationType(SpineScanner spine, Track track)
        throws InvalidSyntaxException,
               UnsupportedNoteException
    {
    // the recognized intonation designators are "*py" for Pythagorean
    // and "*dia"key for diatonic, where key is a keyletter (A through G,
    // upper or lower case) and an optional sharp or flat (# or -)

    // get the intonation designator and advance past it
    String intonation = spine.current().substring(2);
    spine.advance();
 
    // create an intonation object of the proper type
    if (intonation.startsWith("pyth"))
      {
      this.intonation = new Pythagorean();
      }
    else if(intonation.startsWith("dia"))
      {
      // the format is 
      //    "dia" notename [ "#" | "-" ]
      // where 
      //    notename = ( "a" | "b" | "c" | "d" | "e" | "f" | "g" |
      //                 "A" | "B" | "C" | "D" | "E" | "F" | "G" )
      String kernPitch = intonation.substring(3);
      Note note = kernPitchToNote(kernPitch);
      Diatonic.ScaleType type = (Character.isUpperCase(kernPitch.charAt(0)) ?
                                 Diatonic.ScaleType.MAJOR :
                                 Diatonic.ScaleType.MINOR);
      this.intonation = new Diatonic(
          new Diatonic.Key(note, type), this.pythagorean.frequency(note));
      }
    else if(intonation.startsWith("eq"))
      {
      this.intonation = new EqualTemperament();
      }
    else
      {
      throw new InvalidSyntaxException(
        "Unrecognized intonation type: " + intonation);
      }
    }

  /**
   *  Interprets a **kern modulation, adding MIDI events
   *  to a given track if appropriate.<p>
   *  The syntax of a modulation is 
   *     mod { mod }
   *  where mod is
   *     ("1" | "2" | ... | "7" ) [ s ]
   *  where s depends on the intonation type and is parsed
   *  by the current <code>Intonation</code> object.
   *
   *  @param   spine     a scanner for the modulation, positioned at 
   *                     the modulation element
   *  @param   track     the track
   *  @throws  InvalidSyntaxException
   *           if modulation sequence syntax is invalid
   *  @throws  UnsupportedNoteException
   *           if the modulation is to an unsupported key
   */
  private void interpretModulation(SpineScanner spine, Track track)
        throws InvalidSyntaxException,
               UnsupportedNoteException
    {
    // capture modulation sequence 
    String seq = spine.current().substring(2); 
 
    // until modulation sequence is exhausted..
    int i = 0;
    while (i < seq.length()) 
      {
      char d = seq.charAt(i);
      if (!isScaleDegree(d))
        {
        throw new InvalidSyntaxException(
           "Invalid modulation " + spine.current());
        }
      int degree = d - '0';
      i++;

      // consume any intonation-specific part
      int j = i;
      while (j < seq.length() 
             && !isScaleDegree(seq.substring(j,j+1).charAt(0)))
        {
        j++;
        }
      String modifier = seq.substring(i,j);
      i = j;

      // if the current intonation supports modulation, modulate
      if (this.intonation instanceof ModulatingIntonation)
        {
        ((ModulatingIntonation)this.intonation).modulate(degree, modifier);
        }

      }// while
    
    // advance past the modulation element
    spine.advance();
    }

  /**
   *  Interprets a **kern key signature, adding MIDI events
   *  to a given track if appropriate.
   *  @param   spine     a scanner for the spine, positioned at the
   *                     spine terminator element 
   *  @param   track     the track
   */
  private void interpretKeySignature(SpineScanner spine, Track track)
    {
    // the key signature is informational only
// might the nominal key affect the intonation??
    spine.advance();
    }

  /**
   *  Interprets a **kern spine terminator, adding MIDI events
   *  to a given track if appropriate.
   *  @param   spine     a scanner for the spine, positioned at the
   *                     spine terminator element 
   *  @param   track     the track
   */
  private void interpretSpineTerminator(SpineScanner spine, Track track)
    {
    // is there anything else to do for a spine terminator?
    }

  /**
   *  Interprets a **kern note, adding MIDI events
   *  to a given track if appropriate.
   *  @param   spine     
   *           a scanner for the spine, positioned at the note
   *  @param   track     
   *           the track
   *  @throws  InvalidSyntaxException
   *           if note syntax is invalid
   *  @throws  UnsupportedNoteException
   *           if note is outside the MIDI range
   */
  private void interpretNote(SpineScanner spine, Track track)
        throws InvalidSyntaxException,
               UnsupportedNoteException
    {
// note the kern syntax allows { ( [ to occur in any order
// and interspersed with the other note components (ditto ] ) })
    /* the format of a kern note is
     *    [ phraseOpening ] [ slurOpening ]
     *      ( tiedNote | untiedNote )
     *        [ slurClosing ] [ phraseClosing ]
     *    where 
     *      phraseOpening = "{"
     *      slurOpening = "["
     *      slurClosing = "]"
     *      phraseClosing = "}"
     *      tiedNote = "(" untiedNote { "_" untiedNote } ")"
     *
     *      untiedNote = ( duration pitch [ pause ] |
     *                       pitch duration [ pause ] )
     *      duration = digit { digit } { "." }
     *      pitch = noteLetter { noteLetter } { accidental }
     *        where all note letters must be the same
     *      pause = ";"
     */  
    // get the current element and initialize position in it
    String current = spine.current();
    int index = 0;

    // check for phrase opening
    if (index < current.length() && current.charAt(index) == '{')
      {
      spine.getSpine().openPhrase();
      index++;
      }

    // check for slur opening
    if (index < current.length() && current.charAt(index) == '[')
      {
      spine.getSpine().openSlur();
      index++;
      }

    // interpret tied or untied note
    if (index < current.length() && current.charAt(index) == '(')
      {
      index += interpretTiedNote(spine, index, track);
      }
    else
      {
      index += interpretUntiedNote(spine, index, track);
      }

    // check for slur closing
    if (index < current.length() && current.charAt(index) == ']')
      {
      Spine s = spine.getSpine();
      if (s.getSlurCount() > 0)
        {
        s.closeSlur();
        }
      else
        {
        throw new InvalidSyntaxException(
          "Slur close with no open slur: " + current);
        }
      index++;
      }      
      
    // check for phrase closing
    if (index < current.length() && current.charAt(index) == '}')
      {
      Spine s = spine.getSpine();
      if (s.getPhraseCount() > 0)
        {
        s.closePhrase();
        }
      else
        {
        throw new InvalidSyntaxException(
          "Phrase close with no open phrase: " + current);
        }
      }      

    // advance past current note element 
    spine.advance();
    }

  /**
   *  Interprets a tied note, given the index of the start of
   *  the tie in the current spine element, consuming further 
   *  spine elements and adding events to a given MIDI track 
   *  as appropriate, and returns the index in the current spine 
   *  element of the first character past the last note in the tie.
   *  
   *  @param   spine    
   *           the spine being interpreted
   *  @param   index    
   *           index of the left parenthesis beginning the tied 
   *           note in the current spine element
   *  @param   track    
   *           the track to which MIDI events are added
   *  @result  the index of the right parenthesis ending the tie
   *  @throws  InvalidSyntaxException
   *           if note syntax is invalid
   *  @throws  UnsupportedNoteException
   *           if the note is outside the MIDI range
   */
  private int interpretTiedNote(SpineScanner spine, 
                                int index,
                                Track track)
        throws InvalidSyntaxException,
               UnsupportedNoteException
    {
    /**  The format of a tied note is
     *      tiedNote = "(" untiedNote { "_" untiedNote } ")"
     */

    // consume the opening left parenthesis
    index++;

    // extract the first note in the tie
    String noteText = extractNoteText(spine.current().substring(index)); 
    index += noteText.length();
    Note note0 = parseNote(noteText);

    // initialize the duration (in ticks) to the duration of the first note
    long duration = noteDurationToTicks(note0);

    // for each intermediate note in the tie..
    while (index == spine.current().length()
           || spine.current().charAt(index) != ')')
      {
      // advance to the next spine element
      spine.advance();
      index = 0;

      // consume the tie-continuation character
      if (spine.current().charAt(index) == '_')
        {
        index++;
        }
      else
        {
        throw new InvalidSyntaxException(
          "Intermediate note in tied sequence must " +
          "begin with underscore: " + spine.current()); 
        }

      // extract the intermediate note
      noteText = extractNoteText(spine.current().substring(index));
      index += noteText.length();
      Note note = parseNote(noteText);

      // make sure all notes in tie have the same pitch
      if (note.name != note0.name
          || note.octave != note0.octave)
        {
        throw new InvalidSyntaxException(
          "Tied notes must have the same pitch: " + note);
        }

      // increment the total duration by the duration of this note
      duration += noteDurationToTicks(note0);
      }

    // advance past the right paren closing the tie
    index++;

    // get the frequency of the note
    double frequency = this.intonation.frequency(note0);

    // put the note into the MIDI track
    playNote(spine.getSpineNumber(), frequency, duration, track);

    // return position in current spine element
    return index;
    }

  /**
   *  Interprets a note exclusive of any tie it may be participating
   *  in, given the index of the start of the note in the current
   *  spine element, and returns the index in the current spine
   *  element of the first character past the note.
   *  
   *  @param   spine    
   *           the spine being interpreted
   *  @param   index    
   *           index of the first character of the note
   *           in the current spine element
   *  @param   track    
   *           the track to which MIDI events are added
   *  @result  the index of the first character (if any) past the note
   *  @throws  InvalidSyntaxException
   *           if note syntax is invalid
   *  @throws  UnsupportedNoteException
   *           if note is outside the MIDI range
   */
  private int interpretUntiedNote(SpineScanner spine, 
                                   int index, 
                                   Track track)
        throws InvalidSyntaxException,
               UnsupportedNoteException
    {
    // extract the note text
    String noteText = extractNoteText(spine.current().substring(index)); 
    index += noteText.length();
System.out.println("***** untied: " + noteText);
    Note note = parseNote(noteText);
System.out.println("***** untied: " + note);

    // see if there is a pause
    boolean pause = false;
    if (index < noteText.length()
        && noteText.charAt(index) == ';')
      {
      pause = true;
      index++;
      }
System.out.println("***** untied pause = " + pause);

    // check that whole string was parsed
    if (index < noteText.length())
      {
      throw new InvalidSyntaxException(
        "Invalid **kern note: " + noteText);
      }

    // discover the duration of the note (in ticks)
    long duration = noteDurationToTicks(note);
System.out.println("***** untied duration " + duration);

    // for now, ignore pauses

    // get the frequency of the note
    double frequency = this.intonation.frequency(note);
System.out.println("***** untied frequency " + frequency);

testNote = note;

    // put the note into the MIDI track
    playNote(spine.getSpineNumber(), frequency, duration, track);

    // return position in current spine element
    return index;


/*** obsolete
    // maximum non-triplet duration value
    final int MAX_DURATION = 128;
    while (index < spine.current().length()
           && Character.isDigit(spine.current().charAt(index)))
      {
      index++;
      }
    int durationValue = 
        Integer.parseInt(spine.current().substring(0,index));

    // see if it is a valid duration value.
    // The valid duration values are
    //   0    breve
    //   1    whole note
    //   2    half note
    //   3    half-note triplet
    //   4    quarter note
    //   6    quarter-note triplet
    //   8    eighth note
    //   12   eighth-note triplet
    //   16   sixteenth note
    //   24   sixteenth-note triplet
    //   32   thirty-second note
    //   48   thirty-second note triplet
    //   64   sixty-fourth note
    //   96   sixty-fourth note triplet 
    //   128  one hundred twenty-eighth note
    //   192  one-hundred-twenty-eighth-note triplet
    if (durationValue != 0)
      {
      int value = 1;
      while (value <= MAX_DURATION
             && durationValue != value
             && durationValue != value + (value / 2))
        {
        value += value;
        }
      if (value > MAX_DURATION)
        {
        throw new InvalidSyntaxException(
          "Invalid note duration: " + spine.current());
        }
      }

    // parse the duration dots
    int dotCount = 0;
    while (index < spine.current().length()
           && spine.current().charAt(index) == '.')
      { 
      index++;
      dotCount++;
      }

    // convert the kern pitch to a note
    Note note = kernPitchToNote(spine.current().substring(index));

    // get the frequency of the note
    double frequency = this.intonation.frequency(note);

    // get the duration of the note in ticks
    long duration = noteDurationToTicks(note);

    // put the note into the MIDI track
    playNote(spine.getSpineNumber(), frequency, duration, track);

    // return position in current spine element
    return index; 
***/
    }

  /**
   *  Extracts the text representing a **kern note from 
   *  given spine element text and returns it.  
   *
   *  @param   elementText  
   *           text from a spine element
   *  @result  the text of the note with the duration before the
   *           pitch (no matter which came first in the input)
   *  @throws  InvalidSyntaxException
   *           if note syntax is invalid
   */
  private String extractNoteText(String elementText) 
        throws InvalidSyntaxException
    {
    // if the text begins with a digit, assume the duration comes first;
    // otherwise assume the pitch comes first
    int index = 0;
    String duration = "";
    String pitch = "";
    String pause = "";
    if (index < elementText.length()
        && Character.isDigit(elementText.charAt(index)))
      {
      duration = extractDuration(elementText);
System.out.println("***** extracted duration " + duration);
      index += duration.length();
      pitch = extractPitch(elementText.substring(index));
System.out.println("***** extracted pitch " + pitch);
      index += pitch.length();
      }
    else
      {
      pitch = extractPitch(elementText);
System.out.println("***** extracted pitch " + pitch);
      index += pitch.length();
      duration = extractDuration(elementText.substring(index));
System.out.println("***** extracted duration " + duration);
      index += pitch.length();
      }

    // if there is a pause, extract it
    if (index < elementText.length()
        && elementText.charAt(index) == ';')
      {
      pause = ";";
      }

    // return the entire note, duration first
    return duration + pitch + pause;
    }

  /**
   *  Extracts the text representing a **kern rest from 
   *  given spine element text and returns it.  
   *
   *  @param   elementText  
   *           text from a spine element
   *  @result  the text of the rest with the duration before the
   *           pitch (no matter which came first in the input)
   *  @throws  InvalidSyntaxException
   *           if rest syntax is invalid
   */
  private String extractRestText(String elementText) 
        throws InvalidSyntaxException
    {
    // if the text begins with a digit, assume the duration comes first;
    // otherwise assume the 'r' comes first
    int index = 0;
    String duration = "";
    String r = "";
    String pause = "";
    if (index < elementText.length()
        && Character.isDigit(elementText.charAt(index)))
      {
      duration = extractDuration(elementText.substring(index));
      index += duration.length();
      if (index < elementText.length()
          && elementText.charAt(index) == 'r')
        {
        r = "r";
        index++;
        }
      }
    else
      {
      if (index < elementText.length()
          && elementText.charAt(index) == 'r')
        {
        r = "r";
        index++;
        }
      duration = extractDuration(elementText.substring(index));
      index += duration.length();
      }

    // if there is a pause, extract it
    if (index < elementText.length()
        && elementText.charAt(index) == ';')
      {
      pause = ";";
      index++;
      }

    // return the entire note, duration first
    return duration + r + pause;
    }

  /**
   *  Extracts the text representing a **kern note's duration
   *  from given text and returns it.
   *
   *  @param   text   the given text
   *  @result  the text for the note duration
   */
  private String extractDuration(String text)
    {
    // extract any leading string of digits and dots
    int index = 0;
    while (index < text.length()
           && (Character.isDigit(text.charAt(index))
               || text.charAt(index) == '.'))
      {
      index++;
      }
    return text.substring(0, index);
    }

  /**
   *  Extracts the text representing a **kern note's pitch
   *  from given text and returns it.
   *
   *  @param   text   the given text
   *  @result  the text for the note's pitch
   */
  private String extractPitch(String text)
    {
    // extract any leading string of pitch letters and accidentals
    // followed by an optional pitch modifier.
    int index = 0;
    while (index < text.length()
           && isPitchLetter(text.charAt(index)))
      {
      index++;
      }
    while (index < text.length()
           && isAccidental(text.charAt(index)))
      {
      index++;
      }
    if (index < text.length()
        && isPitchModifier(text.substring(index)))
      {
      index += 2;
      }
    return text.substring(0, index);
    }

  /**
   *  Parses the **kern text for a note and returns a
   *  <code>Note</code> object representing the note.
   *  It is assumed that the duration comes before the
   *  pitch in the text representation.
   *
   *  @param   noteText  the text for the note
   *  @result  a <code>Note</code> for the text
   */
  private Note parseNote(String noteText)
      throws InvalidSyntaxException
    {
    // parse the duration number
    int index = 0;
    while (index < noteText.length()
           && Character.isDigit(noteText.charAt(index)))
      {
      index++;
      }
    String duration = noteText.substring(0,index);
    if (duration.length() == 0)
      {
      throw new InvalidSyntaxException("Missing duration: " + noteText);
      }
    int durationNumber = 
        Integer.parseInt(noteText.substring(0,index));

    // parse the duration dots
    int dotCount = 0;
    while (index < noteText.length()
           && noteText.charAt(index) == '.')
      { 
      index++;
      dotCount++;
      }

    // now parse the note's name
    if (index == noteText.length())
      {
      throw new InvalidSyntaxException(
        "Missing pitch letter: " + noteText);
      }
    else if (!isPitchLetter(noteText.charAt(index)))
      {
      throw new InvalidSyntaxException(
        "Invalid pitch letter: " + noteText);
      }
    char letter = noteText.charAt(index);
    index++;
    int letterCount = 1;
    while (index < noteText.length()
           && noteText.charAt(index) == letter)
      {
      index++;
      letterCount++;
      }
    Note.Name noteName = (letter == 'C' ? Note.Name.C :
                         (letter == 'D' ? Note.Name.D :
                         (letter == 'E' ? Note.Name.E :
                         (letter == 'F' ? Note.Name.F :
                         (letter == 'G' ? Note.Name.G :
                         (letter == 'A' ? Note.Name.A :
                         (letter == 'B' ? Note.Name.B :
                         (letter == 'c' ? Note.Name.C :
                         (letter == 'd' ? Note.Name.D :
                         (letter == 'e' ? Note.Name.E :
                         (letter == 'f' ? Note.Name.F :
                         (letter == 'g' ? Note.Name.G :
                         (letter == 'a' ? Note.Name.A : 
                                          Note.Name.B  )))))))))))));
    int octaveNumber = 0;
    if (Character.isLowerCase(letter))
      {
      octaveNumber = letterCount - 1;
      }
    else
      {
      octaveNumber = -letterCount;
      }

    // and now parse the note's accidentals, if any
    char accidental = 'n';
    int accidentalCount = 0;
    if (index < noteText.length()
        && isAccidental(noteText.charAt(index)))
      {
      accidental = noteText.charAt(index);
      accidentalCount = 1;
      index++;

      while (index < noteText.length()
             && noteText.charAt(index) == accidental)
        {
        accidentalCount++;
        index++;
        }
      }

    // see if there is a pitch modifier
    Note.PitchModifier modifier = Note.PitchModifier.NONE;
    System.out.println("***** left: " + noteText.substring(index));
    if (index < noteText.length()
        && isPitchModifier(noteText.substring(index)))
      {
      index++;
      if (noteText.charAt(index) == '2')
        {
        System.out.println("***** parseNote: LOWERED_2ND");
        modifier = Note.PitchModifier.LOWERED_2ND; 
        index++;
        }
/***
      else if (noteText.substring(index).length() >= 2 &&
               noteText.substring(index, index+2).toLowerCase().equals("ii"))
        {
System.out.println("***** parseNote: LOWERED_2ND");
        modifier = Note.PitchModifier.LOWERED_2ND; 
        index += 2;
        }
***/
      else if (noteText.charAt(index) == '7')
        {
        modifier = Note.PitchModifier.SEPTIMAL_7TH; 
        index++;
        }
/***
      else if (noteText.substring(index).length() >= 3 &&
               noteText.substring(index, index+3).toLowerCase().equals("vii"))
        {
        modifier = Note.PitchModifier.SEPTIMAL_7TH; 
        index += 3;
        }
***/
      else
        {
        throw new InvalidSyntaxException(
            "Invalid pitch modifier " + noteText);
        }
      }

    // finally, construct a Note
    int accidentalNumber = (accidental == '#' ? accidentalCount  :
                           (accidental == '-' ? -accidentalCount :
                                                0 ));  
    Note note = new Note(noteName,
                         Note.accidental(accidentalNumber), 
                         octaveNumber, 
                         durationNumber,
                         dotCount,
                         modifier);

    // return a Note object corresponding to the note text
    return note;
    }

  /**
   *  Returns the number of MIDI ticks corresponding to a given
   *  note's duration.
   *  @param   note   the note
   *  @result  number of ticks
   */
  private long noteDurationToTicks(Note note)
    {
    // compute and return number of ticks
    return durationToTicks(note.duration());
    }

  /**
   *  Returns the number of MIDI ticks corresponding to a given duration.
   *  @param   duration   the duration in whole notes
   *  @result  number of ticks
   */
  private long durationToTicks(double duration)
    {
    double ticks = duration * 4. * TICKS_PER_QUARTER;
    return (long)(ticks + 0.5);
    }

  /**
   *  Puts the MIDI events for playing a note into a MIDI track.
   *  @param   channel     MIDI channel number
   *  @param   frequency   the frequency of the note (in Hz)
   *  @param   duration    the duration of the note (in MIDI ticks)
   *  @param   track       the MIDI track
   *  @throws  UnsupportedNoteException
   *           if frequency is not in the MIDI rante
   */
  private void playNote(int channel, 
                        double frequency, 
                        long duration, 
                        Track track)
      throws UnsupportedNoteException
    {
    // get note number of nearest MIDI note
    int noteNumber = frequencyToMIDINoteNumber(frequency);
System.out.println("***** noteNumber " + noteNumber);
/***
if (testNote.accidental == Note.Accidental.DOUBLE_SHARP)
{
noteNumber = noteNumber - 1;
//frequency = frequency - (frequency/0x1ffc);
}
else if (testNote.accidental == Note.Accidental.DOUBLE_FLAT)
{  
noteNumber = noteNumber + 1;
//frequency = frequency + (frequency/0x1ffc);
}
***/

    // get its frequency
//  double midiFrequency = midiNoteNumberToFrequency(noteNumber);
    Note midiNote = midiNoteNumberToNote(noteNumber);
    double midiFrequency = this.equalTemperament.frequency(midiNote);
System.out.println("***** midiFrequency " + midiFrequency);

    // include pitch bend for the difference
// for now, assume the pitch bend sensitivity is set to
// the default 2 semitones--later figure out how to set it
    final double pitchBendRange = 2.0;
    int[] data = computePitchBendData(midiFrequency, 
                                      frequency, 
                                      pitchBendRange); 
System.out.println("***** pitch bend data " + 
  Integer.toHexString(data[0]) + " " + Integer.toHexString(data[1]));

    try
      {
      ShortMessage message = new ShortMessage();
      message.setMessage(ShortMessage.PITCH_BEND,
                         channel,
                         data[0], data[1]);                   
      addToTrack(message, 0, track);
  
      // include the MIDI note on
      message = new ShortMessage();
      message.setMessage(ShortMessage.NOTE_ON,
                         channel,
                         noteNumber,
                         DEFAULT_MIDI_VELOCITY);
      addToTrack(message, duration, track);
  
      // include the MIDI note off
      message = new ShortMessage();
      message.setMessage(ShortMessage.NOTE_OFF,
                         channel,
                         noteNumber,
                         DEFAULT_MIDI_VELOCITY);
      addToTrack(message, 0, track);
      }
    catch (InvalidMidiDataException e)
      {
      System.err.println("Should not happen");
      e.printStackTrace();
      System.exit(-1);
      }
System.out.println("Message added to track");
    }

  /**
   *  Returns the MIDI note number for the MIDI note nearest to
   *  a given frequency.
   *  @param  frequency  
   *          the frequency
   *  @result the MIDI note number
   *  @throws UnsupportedNoteException
   *          if note is not in the MIDI range
   */
  private int frequencyToMIDINoteNumber(double frequency)
      throws UnsupportedNoteException
    {
    /**
     *  if a = pitch of concert A, we have
     *     a * r^n <= f < a * r^(n+1) 
     *  for some MIDI note number n, where f is the frequency
     *  and r is the twelfth root of 2.  Hence
     *     n <= (ln f - ln a) / ln r < n+1, or
     *     n <= (ln f - ln a) * 12 / ln 2 < n+1
     */
    final double A = Intonation.CONCERT_A;

    // compute the offset in MIDI note numbers from A,
    // rounding away from zero to get the nearest MIDI note
    double v = (((Math.log(frequency) - Math.log(A)) * 12)
                     / Math.log(2.0));
    int n = (int)(v >= 0 ? v + 0.5 : v - 0.5);

    // convert offset to MIDI note number 
    int noteNumber = n + MIDI_CONCERT_A;

/***
    // now make sure the frequency of the selected MIDI 
    // note is <= the given frequency
    if (midiNoteNumberToFrequency(noteNumber) > frequency)
      {
      noteNumber--;
      }
***/

    // check validity of MIDI note number
    if (noteNumber < MIN_MIDI_NOTE || noteNumber > MAX_MIDI_NOTE)
      {
      throw new UnsupportedNoteException(
        "Frequency " + frequency + " is outside the MIDI range");
      }

    // return the MIDI note number
    return noteNumber;
    }

  /**
   *  Returns frequency corresponding to the given MIDI note number.
   *  @param  noteNumber
   *          the MIDI note number
   *  @result the corresponding frequency
   *  @throws UnsupportedNoteException
   *          if note is not in the MIDI range
   */
  private double midiNoteNumberToFrequency(int noteNumber)
      throws UnsupportedNoteException
    {
    // first make sure it's really a MIDI note
    if (noteNumber < MIN_MIDI_NOTE || noteNumber > MAX_MIDI_NOTE)
      {
      throw new UnsupportedNoteException(
        "MIDI note number out of range: " + noteNumber);
      }

    // f = f(A) * r^(N - N(A))
    // where f(A) = frequency of concert A,
    //       r = twelfth root of 2,
    //       N = the given MIDI note number
    //       N(A) = the MIDI note number of concert A
    double frequency = Intonation.CONCERT_A *
                         Math.pow(EqualTemperament.SEMITONE,
                           noteNumber - MIDI_CONCERT_A);
// I'm not sure that is correct..
    // return frequency
    return frequency;
    }

  /**
   *  Returns <code>Note</code> corresponding to a given MIDI note number.
   *  @param  noteNumber
   *          the MIDI note number
   *  @result the corresponding <code>Note</code>
   *  @throws UnsupportedNoteException
   *          if note is not in the MIDI range
   */
  private Note midiNoteNumberToNote(int noteNumber)
      throws UnsupportedNoteException
    {
    final int MIDI_C_ABOVE_MIDDLE_C = MIDI_MIDDLE_C + 12;
    final int OCTAVE = 12;

    // first make sure it's really a MIDI note
    if (noteNumber < MIN_MIDI_NOTE || noteNumber > MAX_MIDI_NOTE)
      {
      throw new UnsupportedNoteException(
        "MIDI note number out of range: " + noteNumber);
      }

    // discover what octave the note is in
    int octave = 0;
    while (noteNumber >= MIDI_C_ABOVE_MIDDLE_C)
      {
      octave++;
      noteNumber -= OCTAVE;
      }
    while (noteNumber < MIDI_MIDDLE_C)
      {
      octave--;
      noteNumber += OCTAVE;
      }

    // discover note name and accidental and create note
    Note note = null;
    switch (noteNumber)
      {
      case MIDI_MIDDLE_C:
        note = new Note(Note.Name.C, Note.Accidental.NATURAL, octave);
        break;

      case MIDI_MIDDLE_C+1:
        note = new Note(Note.Name.C, Note.Accidental.SHARP, octave);
        break;

      case MIDI_MIDDLE_C+2:
        note = new Note(Note.Name.D, Note.Accidental.NATURAL, octave);
        break;

      case MIDI_MIDDLE_C+3:
        note = new Note(Note.Name.E, Note.Accidental.FLAT, octave);
        break;

      case MIDI_MIDDLE_C+4:
        note = new Note(Note.Name.E, Note.Accidental.NATURAL, octave);
        break;

      case MIDI_MIDDLE_C+5:
        note = new Note(Note.Name.F, Note.Accidental.NATURAL, octave);
        break;

      case MIDI_MIDDLE_C+6:
        note = new Note(Note.Name.F, Note.Accidental.SHARP, octave);
        break;

      case MIDI_MIDDLE_C+7:
        note = new Note(Note.Name.G, Note.Accidental.NATURAL, octave);
        break;

      case MIDI_MIDDLE_C+8:
        note = new Note(Note.Name.G, Note.Accidental.SHARP, octave);
        break;

      case MIDI_MIDDLE_C+9:
        note = new Note(Note.Name.A, Note.Accidental.NATURAL, octave);
        break;

      case MIDI_MIDDLE_C+10:
        note = new Note(Note.Name.B, Note.Accidental.FLAT, octave);
        break;

      case MIDI_MIDDLE_C+11:
        note = new Note(Note.Name.B, Note.Accidental.NATURAL, octave);
        break;

      default:
        System.err.println("Should not happen");
        new Exception().printStackTrace();
        System.exit(-1);
      }

    // return the note
    return note;
    }

  /**
   *  Returns the two data bytes for a MIDI pitch bend message,
   *  given the frequency of the MIDI note to be bent, the frequency
   *  it is to be bent to, and the pitch bend range (sensitivity).
   *  @param   midiFrequency   note to bend
   *  @param   frequency       resulting bent note
   *  @param   pitchBendRange  full range in semitones
   *  @result  high- and low-order pitch bend message bytes
   */
  private int[] computePitchBendData(double midiFrequency,
                                     double frequency,
                                     double pitchBendRange)
    {
    // range of pitch bend data is [0, PITCH_BEND_EXTENT-1],
    // zero bend corresponds to PITCH_BEND_EXTENT / 2
    final double PITCH_BEND_EXTENT = 0x4000;

    // compute fraction of a semitone to bend
/***
    double semitones = (frequency - midiFrequency) /
                         ((EqualTemperament.SEMITONE - 1) * frequency);
***/
    
/***
    double semitones = (frequency - midiFrequency) /
//                       (MIDI_SEMITONE * frequency);
                         (MIDI_SEMITONE * midiFrequency);
***/
//semitones *= 0.5;

    double semitones = 0.0;
    if (frequency > midiFrequency) 
      {
      semitones = (frequency - midiFrequency) / 
                    (MIDI_SEMITONE * midiFrequency);
      }
    else if (frequency < midiFrequency)
      {
      semitones = (frequency - midiFrequency) /
                    (MIDI_SEMITONE * frequency);
      }
  
System.out.println("***** pitch bend semitones " + semitones);

    double fractionOfRange = semitones / pitchBendRange;

    // derive the pitch bend data value from that
    int dataValue = (int)((PITCH_BEND_EXTENT/2) + 
                            fractionOfRange * (PITCH_BEND_EXTENT/2));
System.out.println("***** pitch bend dataValue " + 
                   Integer.toHexString(dataValue));

    // extract the data value's high and low order bytes
    int lo = dataValue & 0x7f;
    int hi = (dataValue >> 7) & 0x7f;

    // return the low and high order bytes
    return new int[] { lo, hi };
    }

  /**
   *  Interprets a **kern rest, adding MIDI events
   *  to a given track if appropriate.
   *  @param   spine     a scanner for the spine, positioned at the rest
   *  @param   track     the track
   *  @throws  InvalidSyntaxException
   *           if 
   */
  private void interpretRest(SpineScanner spine, Track track)
      throws InvalidSyntaxException
    {
    /*
     * the syntax of a rest is
     *     rest = (  durationNumber 'r' | 'r' durationNumber ) [ pause ]
     * where
     *     durationNumber = digit { digit }
     * and 
     *     pause = ';'
     */
    
    // extract the rest text, which puts the duration first
    String restText = extractRestText(spine.current()); 
System.out.println("***** " + restText);

    // parse the duration number
    int index = 0;
    while (index < restText.length()
           && Character.isDigit(restText.charAt(index)))
      {
      index++;
      }
    int durationNumber = 
        Integer.parseInt(restText.substring(0,index));
System.out.println("***** durationNumber " + durationNumber);

    // parse the duration dots
    int dotCount = 0;
    while (index < restText.length()
           && restText.charAt(index) == '.')
      { 
      index++;
      dotCount++;
      }
System.out.println("***** dot count " + dotCount);
    
    // the 'r' comes next
    if (index < restText.length() && restText.charAt(index) == 'r')
      {
      index++;
      }
    else
      {
      throw new InvalidSyntaxException("Invalid rest: " + restText);
      }
System.out.println("***** Past r");

    // see if there is a pause
    boolean pause = (index < restText.length() 
                     && restText.charAt(index) == ';');
System.out.println("***** pause = " + pause);

    
    // find the duration in whole notes
    double duration = Note.duration(durationNumber, dotCount);
System.out.println("***** duration = " + duration);
    
    // get the duration in ticks
    long ticks = durationToTicks(duration);
System.out.println("***** ticks = " + ticks);
 
    // advance this track's ticks by that amount
    advanceTicks(ticks);

    // for now, ignore pauses

    // advance past the rest element
    spine.advance();
    }

  /**
   *  Interprets a **kern barline, adding MIDI events
   *  to a given track if appropriate.
   *  @param   spine     a scanner for the spine, positioned 
   *                     at the barline
   *  @param   track     the track
   */
  private void interpretBarline(SpineScanner spine, Track track)
    {
System.out.println("***** bar # " + spine.current().substring(1));
    // advance past barline element
    spine.advance();
    }
  /**
   *  Interprets a **kern placeholder, adding MIDI events
   *  to a given track if appropriate.
   *  @param   spine     a scanner for the spine, positioned 
   *                     at the placeholder
   *  @param   track     the track
   */
  private void interpretPlaceholder(SpineScanner spine, Track track)
    {
    // advance past placeholder element
    spine.advance();
    }

  /**
   *  Output default stuff at the beginning of a track.
   *  @param   track  track to output them to
   */
  private void outputDefaults(Track track)
    {
    outputMeterSignature(track);
    outputMetronomeMarking(track);
    }

  /**
   *  Output current meter signature.
   *  @param   track  track to output it to
   */
  private void outputMeterSignature(Track track)
    {
    // compose a time signature message 
    byte[] data = new byte[TIME_SIGNATURE_LENGTH];
    data[0] = (byte)this.meter.numerator;
    data[1] = (byte)powerOfTwo(this.meter.denominator);
    data[2] = (byte)midiClocksPerMetronomeTick();
    data[3] = (byte)DEMISEMIQUAVERS_PER_24_MIDI_CLOCKS;
    MetaMessage message = composeMetaMessage(TIME_SIGNATURE, data);

    // put the time signature into the track
    addToTrack(message, 0, track);
    }

  /**
   *  Output current metronome marking.
   *  @param   track  track to output it to
   */
  private void outputMetronomeMarking(Track track)
    {
    // compose a tempo message
    double usecPerBeat = (60.0/this.metronomeMarking)*1000000.0; 
    int temp = (int)usecPerBeat;
    byte[] data = new byte[TEMPO_LENGTH];
    data[0] = (byte)((temp >> 16) & 0xff); 
    data[1] = (byte)((temp >> 8) & 0xff);
    data[2] = (byte)(temp & 0xff);
    MetaMessage message = composeMetaMessage(TEMPO, data);

    // put the tempo into the track
    addToTrack(message, 0, track);
    }

  /**
   *  Adds a given MIDI message with a given duration
   *  to a given track.
   *  @param   message   the message
   *  @param   duration  the duration
   *  @param   track     the track
   */
  private void addToTrack(MidiMessage message,
                          long duration,
                          Track track)
    {
    // embed the message in a MIDI event at the current time tick
    // and append it to the track
    track.add(new MidiEvent(message, this.currentTick));

    // advance the current tick
    advanceTicks(duration);
    }

  /**
   *  Advance the cumulative tick count for this track.
   *  @param   duration  amount to advance by
   */
  private void advanceTicks(long duration)
    {
    this.currentTick += duration;
    }

  /**
   *  Returns a Note object having the same pitch as
   *  a kern pitch signification.
   *  @param   kernPitch
   *  @result  Note corresponding to given kern pitch
   *  @throws  InvalidSyntaxException
   *           if kern pitch signification is invalid
   */
  private Note kernPitchToNote(String kernPitch)
        throws InvalidSyntaxException
    {
    // a kern pitch signification has the format
    //    x {x} ["#" {"#"} | "-" {"-"} | "n" {"n"}]
    // where x is a letter in the range a..g or A..G
    
    // scan the pitch letters
    int index = 0;
    char letter = kernPitch.charAt(index++);
    boolean ok = isPitchLetter(letter);
    while (ok 
            && index < kernPitch.length()
             && letter == kernPitch.charAt(index))
      {
      index++;
      }
    int letterCount = index;

    // scan the accidentals
    char accidental = 0; 
    if (ok && index < kernPitch.length())
      {
      accidental = kernPitch.charAt(index++);
      ok = isAccidental(accidental);
      }
    while (ok
            && index < kernPitch.length()
             && accidental == kernPitch.charAt(index))
      {
      index++;
      }
    int accidentalCount = index - letterCount;

    // make sure the whole string was scanned
    ok = (ok && index == kernPitch.length());
    
    // if the kern pitch was invalid, say so
    if (!ok)
      {
      throw new InvalidSyntaxException(
        "Invalid kern pitch signification: " + kernPitch);
      }

    // set the note name (ignoring repetitions)
    Note.Name noteName = (letter == 'C' ? Note.Name.C :
                         (letter == 'D' ? Note.Name.D :
                         (letter == 'E' ? Note.Name.E :
                         (letter == 'F' ? Note.Name.F :
                         (letter == 'G' ? Note.Name.G :
                         (letter == 'A' ? Note.Name.A :
                         (letter == 'B' ? Note.Name.B :
                         (letter == 'c' ? Note.Name.C :
                         (letter == 'd' ? Note.Name.D :
                         (letter == 'e' ? Note.Name.E :
                         (letter == 'f' ? Note.Name.F :
                         (letter == 'g' ? Note.Name.G :
                         (letter == 'a' ? Note.Name.A : 
                                          Note.Name.B  )))))))))))));

    // develop octave number for Note
    int octaveNumber = 0;
    if ('a' <= letter && letter <= 'g')
      {
      octaveNumber = letterCount - 1;
      }
    else
      {
      octaveNumber = -letterCount;
      }
    
    // develop accidental number for Note
    int accidentalNumber = (accidental == '#' ? 1  : 
                            accidental == '-' ? -1 :
                                                0  );
    accidentalNumber *= accidentalCount;

    // make the Note
    Note note = new Note(noteName, 
                         Note.accidental(accidentalNumber), 
                         octaveNumber);

    // return the Note
    return note;
    }

  /**
   *  Returns the MIDI note number corresponding to
   *  a kern pitch signification.
   *  @param   kernPitch
   *  @result  MIDI note number
   *  @throws  InvalidSyntaxException
   *           if kern pitch signification is invalid
   *           or specifies a note outside the MIDI range
   */
  private int kernPitchToMIDINote(String kernPitch)
        throws InvalidSyntaxException
    {
    // a kern pitch signification has the format
    //    x {x} ["#" ["#"] | "-" ["-"] | "n"]
    // where x is a letter in the range a..g or A..G

    // scan the pitch letters
    int index = 0;
    char letter = kernPitch.charAt(index++);
    boolean ok = isPitchLetter(letter);
    while (ok 
            && index < kernPitch.length()
             && letter == kernPitch.charAt(index))
      {
      index++;
      }
    int letterCount = index;

    // scan the accidentals
    char accidental = 0; 
    if (ok && index < kernPitch.length())
      {
      accidental = kernPitch.charAt(index++);
      ok = isAccidental(accidental);
      }
    while (ok
            && index < kernPitch.length()
             && accidental == kernPitch.charAt(index))
      {
      index++;
      }
    int accidentalCount = index - letterCount;

    // make sure the whole string was scanned
    ok = (ok && index == kernPitch.length());
    
    // if the kern pitch was invalid, say so
    if (!ok)
      {
      throw new InvalidSyntaxException(
        "Invalid kern pitch signification: " + kernPitch);
      }

    // set the MIDI note number ignoring repetitions
    int midiNote = (letter == 'C' ? 52 :
                   (letter == 'D' ? 54 :
                   (letter == 'E' ? 56 :
                   (letter == 'F' ? 57 :
                   (letter == 'G' ? 59 :
                   (letter == 'A' ? 61 :
                   (letter == 'B' ? 59 :
                   (letter == 'c' ? 60 :
                   (letter == 'd' ? 62 :
                   (letter == 'e' ? 64 :
                   (letter == 'f' ? 65 :
                   (letter == 'g' ? 67 :
                   (letter == 'a' ? 69 : 71)))))))))))));
     
    // now go up or down an octave for each repetition
    if (Character.isLowerCase(letter))
      {
      while (--letterCount > 0)
        {
        midiNote += 12;
        }
      }
    else
      {
      while (--letterCount > 0)
        {
        midiNote -= 12;
        }
      }

    // adjust the note number according to the accidentals, if any
    if (accidental == '#')
      {
      while (accidentalCount-- > 0)
        {
        midiNote++;
        }
      }
    else if (accidental == '-')
      {
      while (accidentalCount-- > 0)
        {
        midiNote--;
        }
      }

    // if the note is outside the MIDI range, complain
    if (midiNote < 0 || midiNote > 127)
      {
      throw new InvalidSyntaxException(
        "Pitch not in MIDI range: " + kernPitch);
      }

    // return the MIDI note number
    return midiNote;
    }

  /**
   *  Returns true if a given letter is a pitch letter
   *  ('a' through 'g' or 'A' through 'G').
   *  @param   letter   the given letter
   *  @result  true if letter is a pitch letter
   */
  private boolean isPitchLetter(char letter)
    {
    return (('a' <= letter && letter <= 'g')
            || ('A' <= letter && letter <= 'G'));
    }

  /**
   *  Returns true if a given character is an accidental character.
   *  ('#', '-' or 'n').
   *  @param   c   the given character
   *  @result  true if charcater is an accidental.
   */
  private boolean isAccidental(char c)
    {
    return ((c == '#') || (c == '-') || (c == 'n'));
    }

  /**
   *  Returns true if a given string is a pitch modifier clause.
   *  @param   s   the given string
   *  @result  true if string is a pitch modifier
   */
  private boolean isPitchModifier(String s)
    {
    return (s.charAt(0) == '<');
    }

  /**
   *  Returns true if a given character is a degree of the scale.
   *  @param   s   the given string
   *  @result  true if string is '1' through '7'
   */
  private boolean isScaleDegree(char c)
    {
    return ('1' <= c && c <= '7');
    }

  /**
   *  Returns the power to which 2 must be raised
   *  to equal a given value, which is assumed to be
   *  a power of two.
   *  @param   value  a value that must be a power of two
   *  @result  x such (2^x) = value
   */
  int powerOfTwo(int value)
    {
    int power = 0;
    int x = 1;
    while (x < value)
      {  
      power++;
      x += x;
      }
    return power;
    }

  /**
   *  Returns the number of MIDI clocks per metronome tick.
   */
  int midiClocksPerMetronomeTick()
    {
    int clocks;

    // if it's triple time and more than 3 beats per measure,
    // it's one metronome tick per three eighth notes
    if (this.meter.numerator % 3 == 0 && this.meter.numerator > 3)
      {
      clocks = 3 * MIDI_CLOCKS_PER_QUARTER / 2;
      }

    // otherwise, it's one metronome tick per quarter note
    else
      {
      clocks = MIDI_CLOCKS_PER_QUARTER;
      }

    // return MIDI clock per metronome tick
    return clocks;
    }

  /**
   *  Returns the number of javax.sound ticks corresponding
   *  to a given fraction of a whole note.
   *  @param   duration  fraction of whole note
   *  @result  number of ticks
   */
  long ticksPerNote(double fraction)
    {
    return (long)(fraction * TICKS_PER_QUARTER * 4);  
    }

  /**
   *  Returns a <code>MetaMessage</code> given its type and data.
   *  @param   type    meta-message type
   *  @param   data    meta-message data
   *  @result  a meta-message 
   */
  private MetaMessage composeMetaMessage(int type, byte[] data)
    {
    MetaMessage message = new MetaMessage();
    try
      {
      message.setMessage(type, data, data.length);
      }
    catch (InvalidMidiDataException e)
      {
      System.err.println("Should not happen");
      e.printStackTrace();
      System.exit(-1);
      }
    return message;
    }
  }/** end of class PlayKern */

/**
 *  Class <code>Meter</code> is used to hold a meter signature.
 */
class Meter
  {
  int numerator;
  int denominator;
  Meter(int numerator, int denominator)
    {
    this.numerator = numerator;
    this.denominator = denominator;
    }
  }

//ticksPerSecond = resolution * (currentTempoInBeatsPerMinute / 60.0);
//tickSize = 1.0 / ticksPerSecond;
