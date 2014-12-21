
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
 *  Class <tt>StoppableScalableClock</tt> implements a clock
 *  that can be stopped and restarted and that runs in a ratio
 *  to real time determined by a settable scale factor.  The
 *  clock can also be set to a show given time as its current time.
 */
public class StoppableScalableClock 
  {
  // clock that gives real time since start of run
  private NanoClock clock = new NanoClock();

  // real time at start of current or most recent epoch
  private long startTime;

  // true when clock is stopped
  private boolean stopped = true;

  // scaled time from past epochs (accumulated while clock was running)
  private long history;

  // scale factor (ratio of clock time to real time)
  private double scale = 1.0;

  /**
   *  Stops the clock.
   */
  public void stop()
    {
    if (!this.stopped)
      {
      long endTime = this.clock.now();
      this.history += (endTime - this.startTime) * this.scale;
      this.stopped = true;
      }
    }

  /**
   *  Restarts the clock.
   */
  public void restart()
    {
    if (this.stopped)
      {
      this.startTime = this.clock.now();
      this.stopped = false;
      }
    }

  /**
   *  Sets the clock to a given time.
   *  @param   time
   *           time to set the clock to
   */
  public void set(long time) 
    {
    if (this.stopped)
      {
      this.history = time;
      }
    else
      {
      stop();
      this.history = time;
      restart();
      } 
    }

  /**
   *  Sets the time scale.
   *  @param   scale
   *           units of real time per unit of this clock
   */
  public void setScale(double scale) 
    {
    if (this.stopped)
      {
      this.scale = scale;
      }
    else
      {
      stop();
      this.scale = scale;
      restart();
      }
    }

  /**
   *  Returns the current time.
   *  @result   scaled, non-stopped time since beginning of run (nanosec).
   */
  public long now()
    {
    long time = -1;
    if (!this.stopped)
      {
      time = (long)((this.clock.now() - this.startTime) * this.scale
                      + this.history);
      }
    else
      {
      time = this.history;
      }
    return time;
    }

  /**
   *  Clock that gives nanoseconds since start of run.
   */
  private static class NanoClock
    {
    // system time at start of run
    long t0 = System.nanoTime();

    /**
     *  Returns time since start of run.
     *  @result  time in nanoseconds
     */
    long now()
      {
      return (System.nanoTime() - t0);
      } 
    }
  /** end of static inner class NanoClock */

  /**
   *  Tests this module.
   */
  public static void main(String[] args) 
    {
    StoppableScalableClock clock = new StoppableScalableClock();
    int k = 0;
    double scale = 1;
    while (true)
      { 
      System.out.println("time = " + clock.now());
      clock.stop();
      delay(1.0);
      System.out.println("       "  + clock.now());
      clock.restart();
      delay(1.0);
      scale += 1;
      clock.setScale(scale);
      if (++k % 5 == 0) 
        {
        System.out.println();
        scale = 1;
        clock.setScale(scale);
        clock.set(0); 
        }
      } 
    }

  /**
   *  Delays for a given time interval (for testing).
   *  @param   t
   *           the time interval in seconds
   */
  private static void delay(double t)
    {
    long interval = (long)(t * 1000 + 0.5);
    long end = System.currentTimeMillis() + interval;
    long left = -1;
    while ((left = (end - System.currentTimeMillis())) > 0)
      {
      try
        {
        Thread.sleep(left);
        } 
      catch(InterruptedException e) {  }
      }
    }

  }
/** end of class StoppableScalableClock */
