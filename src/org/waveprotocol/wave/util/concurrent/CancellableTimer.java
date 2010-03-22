/**
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.waveprotocol.wave.util.concurrent;

import com.google.common.annotations.VisibleForTesting;
import static com.google.common.base.Preconditions.checkState;

import org.joda.time.Duration;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Executors;

/**
 * Class for creating and managing timers that are cancellable, and when
 * cancelled, will not reference the Runnable that is the callback for the timer.
 * This is required in some circumstances because even if you cancel the
 * timer, the scheduler keeps it around until the original time expires,
 * possibly locking stuff in memory.
 *
 * Thread safe.
 *
 * @author amcrae@google.com (Andrew McRae)
 */
public class CancellableTimer {

  /**
   * Interface used for holding reference to cancellable task.
   */
  public interface CancellableTask {
    /**
     * Cancel this task, if it hasn't already started.
     * There is no guarantee that the task isn't already in progress,
     * in which it will run to completion.
     */
    void cancel();
  }

  /**
   * Class used for timeout callback. Allows the handle to be cleared
   * so that when or if the timeout is cancelled, the callback does not
   * reference any of the resources via the runnable.
   * Upon a cancel request, the scheduled task is cancelled, and the
   * reference to the runnable is nulled so that no reference is kept
   * to the runnable (and all the associated objects). In essence
   * this class acts as a "cut-out" between the scheduled task and the callback.
   * It is done this way instead of relying on Future.cancel()
   * because Future.cancel() will not actually remove the Future from the queue
   * to be executed, just mark it as cancelled - which means that Future is
   * still referencing all the resources associated with the Runnable for an
   * indeterminate length of time.
   */
  private static class CancellableTaskImpl implements Runnable, CancellableTask {
    private volatile Runnable runnable;
    private volatile ScheduledFuture<?> future;

    private CancellableTaskImpl(Runnable callback) {
      this.runnable = callback;
    }

    @Override
    public void run() {
      // Make a copy in case it gets cancelled under us.
      Runnable copy = runnable;
      if (copy != null) {
        // Make sure there are no longer any references to runnable.
        runnable = null;
        copy.run();
      }
    }

    @Override
    public void cancel() {
      // Remove the runnable so that this task does not any hanging
      // reference to the runnable (and any resources associated with the runnable).
      runnable = null;
      // Cancel the scheduled task. If it has already run, this will be a no-op.
      if (future != null) {
        future.cancel(false);
      }
    }

    /**
     * Register the future of the task.
     */
    private void setFuture(ScheduledFuture<?> taskFuture) {
      future = taskFuture;
    }

  }

  private final ScheduledExecutorService scheduler;
  private final Stopwatch stopwatch;

  /**
   * Constructor for testing.
   *
   * @param scheduler Executor service to use.
   * @param stopwatch The stopwatch to use for timing.
   */
  @VisibleForTesting
  CancellableTimer(ScheduledExecutorService scheduler, Stopwatch stopwatch) {
    this.scheduler = scheduler;
    this.stopwatch = stopwatch;
    stopwatch.start();
  }

  /**
   * Constructor with custom scheduler.
   *
   * @param scheduler Executor service to use.
   */
  public CancellableTimer(ScheduledExecutorService scheduler) {
    this(scheduler, new Stopwatch());
  }

  /**
   * Default constructor. Uses a single threaded executor, so that each of the
   * tasks are executed in sequence, and never in parallel.
   */
  public CancellableTimer() {
    // TODO(arb): STPE eats exceptions. open source the SafeExecutor class from the waveserver code.
    this(new ScheduledThreadPoolExecutor(1, Executors.defaultThreadFactory()), new Stopwatch());
  }

  /**
   * Factory to create a new timeout task using a Duration.
   *
   * @param runnable Callback to invoke when timeout expires.
   * @param duration Duration until timeout runnable is called.
   */
  public CancellableTask newTask(Runnable runnable, Duration duration) {
    return newTask(runnable, duration.getMillis(), TimeUnit.MILLISECONDS);
  }

  /**
   * Factory for using timeunits.
   *
   * @param runnable Callback to invoke when timeout expires.
   * @param time Timeout value.
   * @param units TimeUnits of value
   */
  public CancellableTask newTask(Runnable runnable, long time, TimeUnit units) {
    CancellableTaskImpl task = new CancellableTaskImpl(runnable);
    task.setFuture(scheduler.schedule(task, time, units));
    return task;
  }

  /**
   * @return the current elapsed duration.
   */
  public Duration elapsedDuration() {
    return stopwatch.elapsedDuration();
  }

  private static class Stopwatch {

    private Ticker ticker;
    private boolean isRunning;
    private long startTick;
    private long elapsedNanos;

    /**
     * A time source; returns a time value representing the number of nanoseconds elapsed since some
     * fixed but arbitrary point in time.
     */
    public interface Ticker {

      /**
       * Returns the number of nanoseconds elapsed since this ticker's fixed point of reference.
       */
      long read();
    }

    /**
     * The ticker that returns {@link System#nanoTime()}.
     */
    public static final Ticker JAVA_TICKER = new Ticker() {
      public long read() {
        return System.nanoTime();
      }
    };

    /**
     * Creates (but does not start) a new stopwatch using {@link System#nanoTime} as its time
     * source.
     */
    public Stopwatch() {
      this.ticker = JAVA_TICKER;
    }

    /**
     * Starts the stopwatch.
     *
     * @throws IllegalStateException if the stopwatch is already running.
     */
    public Stopwatch start() {
      checkState(!isRunning);
      isRunning = true;
      startTick = ticker.read();
      return this;
    }

    /**
     * Stops the stopwatch. Future reads will return the fixed duration that had elapsed up to this
     * point.
     *
     * @throws IllegalStateException if the stopwatch is already stopped.
     */
    public Stopwatch stop() {
      long tick = ticker.read();
      checkState(isRunning);
      isRunning = false;
      elapsedNanos += tick - startTick;
      return this;
    }

    private long elapsedNanos() {
      return isRunning ? ticker.read() - startTick + elapsedNanos : elapsedNanos;
    }

    /**
     * Returns the current elapsed time shown on this stopwatch, expressed in the desired time unit,
     * with any fraction rounded down.
     *
     * <p>Note that the overhead of measurement can be more than a microsecond, so it is generally
     * not useful to specify {@link TimeUnit#NANOSECONDS} precision here.
     */
    public long elapsedTime(TimeUnit desiredUnit) {
      return desiredUnit.convert(elapsedNanos(), NANOSECONDS);
    }

    /**
     * Returns the current elapsed time value shown on this stopwatch, rounded down to the nearest
     * millisecond, expressed as a Joda-Time {@code Duration}.
     */
    public Duration elapsedDuration() {
      return new Duration(elapsedTime(TimeUnit.MILLISECONDS));
    }

  }
}
