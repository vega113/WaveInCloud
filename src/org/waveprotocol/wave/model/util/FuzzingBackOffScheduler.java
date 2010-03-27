// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.util;

import org.waveprotocol.wave.model.util.FuzzingBackOffGenerator.BackOffParameters;

/**
 * Schedules a task with fuzzing fibonacci backoff schedule. The
 * Actual scheduling is delegated to a native scheduler.
 * This class does not interpret the delay values, but simply passes
 * them through to the native scheduler.
 *
 * @author zdwang@google.com (David Wang)
 */
public class FuzzingBackOffScheduler implements Scheduler {

  /** The scheduled task that can be cancelled */
  public interface Cancellable {
    /**
     * Cancel this task.
     */
    void cancel();
  }

  /**
   * Allows injection of actually defer executing a task. This because the
   * client and server has different scheduling implementations. A collective
   * scheduler tries to executed the task at the given targetTimeMs, but may
   * execute the task at minAllowedMs if it is more efficient to do so.
   */
  public interface CollectiveScheduler {

    /**
     * Schedule a task to be executed between minAllowedMs and millisec later
     *
     * @param task the task to execute
     * @param minAllowedMs the minimum amount of time to wait.
     * @param targetTimeMs the target delay to wait
     */
    Cancellable schedule(Command task, int minAllowedMs, int targetTimeMs);
  }

  private final FuzzingBackOffGenerator generator;
  private final CollectiveScheduler scheduler;
  private Cancellable scheduledTask;

  /**
   * @param initialBackOffMs Initial value to back off. This class does not interpret the meaning of
   *    this value.
   * @param maxBackOffMs Max value to back off
   * @param randomisationFactor between 0 and 1 to control the range of randomness.
   * @param scheduler assumed not null.
   */
  private FuzzingBackOffScheduler(int initialBackOffMs, int maxBackOffMs,
      double randomisationFactor, CollectiveScheduler scheduler) {
    this.generator = new FuzzingBackOffGenerator(initialBackOffMs, maxBackOffMs,
        randomisationFactor);
    this.scheduler = scheduler;
  }

  @Override
  public void reset() {
    generator.reset();
    if (scheduledTask != null) {
      scheduledTask.cancel();
    }
    scheduledTask = null;
  }

  @Override
  public void schedule(Command task) {
    if (scheduledTask != null) {
      scheduledTask.cancel();
    }
    BackOffParameters parameters = generator.next();
    scheduledTask = scheduler.schedule(task, parameters.minimumDelay, parameters.targetDelay);
  }

  /**
   * Builder for FuzzingBackOffSchedulers.
   */
  public static class Builder {
    private int initialBackOffMs = 10;
    private int maxBackOffMs = 5000;
    private double randomisationFactor = 0.5;
    private final CollectiveScheduler scheduler;

    /**
     * Constructor.
     *
     * @param scheduler the underlying scheduler to use to actually execute tasks.
     */
    public Builder(CollectiveScheduler scheduler) {
      this.scheduler = scheduler;
    }

    /**
     * Build the scheduler.
     *
     * @return the newly created scheduler
     */
    public Scheduler build() {
      return new FuzzingBackOffScheduler(initialBackOffMs, maxBackOffMs, randomisationFactor,
          scheduler);
    }

    public Builder setInitialBackOffMs(int initialBackOffMs) {
      this.initialBackOffMs = initialBackOffMs;
      return this;
    }

    public Builder setMaxBackOffMs(int maxBackOffMs) {
      this.maxBackOffMs = maxBackOffMs;
      return this;
    }

    public Builder setRandomisationFactor(double randomisationFactor) {
      this.randomisationFactor = randomisationFactor;
      return this;
    }
  }
}
