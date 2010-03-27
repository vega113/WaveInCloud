// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.util;


/**
 * A simple priority queue that using double rather than boxed Double
 * for efficiency.
 *
 * @author zdwang@google.com (David Wang)
 */
public interface NumberPriorityQueue {
  /** Number of items in the queue */
  public int size();

  /** Adds e to the back of the queue */
  public boolean offer(double e);

  /** Peek at the head of queue */
  public double peek();

  /** Removes an item from the beginning of the queue */
  public double poll();
}
