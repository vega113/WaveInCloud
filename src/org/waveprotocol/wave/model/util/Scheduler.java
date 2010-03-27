// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.util;

/**
 * A scheduler that can be used for scheduling a single task. The most common use case
 * is having a scheduler that backs off the more you call schedule.
 *
 * @author zdwang@google.com (David Wang)
 */
public interface Scheduler {

  /** Simple command that can be invoked */
  public interface Command {
    void execute();
  }

  /**
   * Resets the backoff time and resets the scheduled job.
   */
  void reset();

  /**
   * Schedules a task to be ran in the future. If a previous task was
   * scheduled, it's cancelled.
   */
  void schedule(Command task);
}
