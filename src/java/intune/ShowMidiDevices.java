
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
import javax.sound.midi.*;

public class ShowMidiDevices {

  private static final int NUL = '\0';
  private static final int NL = '\n';

  private static boolean quit = false;

  public static void main (String[] args) {
    MidiDevice.Info[] info = MidiSystem.getMidiDeviceInfo();
    for (int i = 0; i < info.length; i++) {
      System.out.println ("" + i + ": " + info[i].toString());
    }

    MidiDevice device = null;
    for (int i = 0; !quit && i < info.length; i++) {
      try {
        device = MidiSystem.getMidiDevice (info[i]);
        device.open();
        Receiver recv = device.getReceiver();
        if (recv != null) {
          System.out.println();
          System.out.println ("" + i + ": " + info[i].toString());
          System.out.println("Hit Enter to sound device " + i +
                             " or q to quit");
          pause();
          soundNote (recv);
        }
        device.close();
      } catch(Exception e) {  }
    }
    System.exit(0);
  }

  static void soundNote (Receiver receiver) {
    try {
      ShortMessage message = new ShortMessage();
      message.setMessage(ShortMessage.NOTE_ON, 0, 60, 93);
      receiver.send(message, -1L);
      sleep (2.0);
      message.setMessage (ShortMessage.NOTE_OFF, 0, 60, 0);
      receiver.send(message, -1L);
    } catch (Exception e) {  }
  }

  static private void pause() {
    int key = NUL;
    while (key != NL && key != 'q' && key != 'Q') {
      try {
        key = System.in.read();
      } catch (Exception e) { }
    }
    quit = (key == 'q' || key == 'Q');
  }

  static private void sleep (double seconds) {
    long millis = (long)(seconds * 1000 + 0.5);
    try {
      Thread.currentThread().sleep(millis);
    } catch (InterruptedException e) {  }
  }
}
