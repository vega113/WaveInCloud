// Copyright 2009 Google Inc. All Rights Reserved.
package org.waveprotocol.wave.model.util;

/**
 * An immutable pair of integers.
 *
*
 */
public class IntPair {
  private final int first;
  private final int second;

  /**
   * Constructs a pair of integers.
   * @param first
   * @param second
   */
  public IntPair(int first, int second) {
    this.first = first;
    this.second = second;
  }

  /**
   * Returns the first integer in the pair.
   */
  public int getFirst() {
    return first;
  }

  /**
   * Returns the second integer in the pair.
   */
  public int getSecond() {
    return second;
  }
}
