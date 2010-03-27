// Copyright 2009 Google Inc. All Rights Reserved.
package org.waveprotocol.wave.model.util;


/**
 * An integer range.
 *
 * A pair of integers with additional constraint that start must be less than or
 * equal to end.
 *
*
 */
public final class IntRange extends IntPair {

  /**
   * Constructs an integer range.
   *
   * @param start start of the range.
   * @param end end of the range, must be greater than or equal to start.
   */
  public IntRange(int start, int end) {
    super(start, end);
    Preconditions.checkArgument(start <= end, "Start of range must be <= end");
  }

  public IntRange(int collapsedAt) {
    super(collapsedAt, collapsedAt);
  }
}
