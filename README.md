intune
======

Software to play keyboard music with perfect intonation.

The purpose of this software is to play keyboard music in perfect (just) intonation, the intonation a good _a capella_ singing group or a good string quartet uses.  The package is aimed at diatonic music, music with key signatures and modulations.

    You may think that playing in just intonation in a particular key is
    just a matter of assigning fixed pitchs to the different degrees of
    the scale, but you would be WRONG!  Even if you stay in one key without
    modulations, there is a need for notes to have variable pitches.

    For example, in the key of C, take the frequency of the tonic note, C,
    to be 1.  Then the frequency of the dominant note, G, is 3/2, and the
    frequency of the subdominant note, F, is 4/3.  In order to form a
    perfect fifth with G, the second note of the scale, D, must have
    frequency 9/8 ((3/2)(3/4) = 9/8).  However, in order to form a
    minor third with F, D must have frequency 10/9 ((4/3)(5/6) = 10/9).
    So the pitch of D varies with the musical context without ever straying
    from the key of C.

One way to use this package is to use the `PlayTuned` program to allow you to enter (play) a piece of music on a MIDI keyboard, using whatever temperament pleases you (I favor the Young 1799 tuning), while recording it to disk.  Then open the recorded file in the `Editor` and annotate it with tuning instructions denoting modulations and passages where the subdominant (10/9) second should be used.  When you play back the annotated file (using the `Playback` program), provided the annotations are correct, it will sound with perfect intonation.  The saved file is a `.tun` file, a format used only by intune, but the program `Tun2Mid` will convert it to a MIDI file (there is also a `Mid2Tun` program).  If you convert a correctly annotated `.tun` file to MIDI, the MIDI will will sound with perfect intonation when played on any MIDI player that supports pitch bend, which I believe is just about all of them.

Another way to use the package is to use an auxiliary MIDI device to allow you to play in perfect intonation in real time.  The auxiliary device, say a set of organ foot pedals, lets you specify the current key and whether to use the subdominant tuning for the second tone of the scale.  I admit I never had much practical success in this mode.  Perhaps if I were an organist instead of a pianist and had had a proper set of foot pedals it would have worked out better.

All the Java modules are in the `intune` package.  To run, just put the `.jar` file in your classpath, for example

    java -cp intune.jar intune.PlayTuned
    
To compile, set the classpath to `<...>/src/java`, `cd` to `<...>/src/java/intune` and enter `javac *java`.
To rebuild the jar file, `cd` to `<...>/src/java` and enter

    jar cf intune.jar intune
    
Complete instructions on the use of the package and some examples are in the `intune.html` file.

In my opinion, one of the more interesting pieces of code in the package is the brief `StoppableScalableClock` module.  There are also a few modules left over from a previous version that read and play the `**kern` musical notation (see `PlayKern.java`).

It is my ambition to use intune to record (using `PlayTuned`) and annotate (using the `Editor`) all the examples in Mathieu's _Harmonic Experience_.  Then the listener could play them back in perfect intonation or in any temperament for comparison.
                                    





