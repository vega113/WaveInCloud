// Copyright 2008 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.util.Preconditions;

/**
 * An immutable range of two locations integers
 */
public final class Range {

  private final int start;
  private final int end;

  /**
   * Construct a range
   *
   * @param start
   * @param end
   */
  public Range(int start, int end) {
    if (start < 0 || start > end) {
      Preconditions.illegalArgument("Bad range: (" + start + ", " + end + ")");
    }
    this.start = start;
    this.end = end;
  }

  /**
   * Construct a collapsed range
   *
   * @param collapsedAt
   */
  public Range(int collapsedAt) {
    this(collapsedAt, collapsedAt);
  }

  /**
   * @return start point
   */
  public int getStart() {
    return start;
  }

  /**
   * @return end point
   */
  public int getEnd() {
    return end;
  }

  /**
   * @return true if the range is collapsed
   */
  public boolean isCollapsed() {
    return start == end;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return start + 37 * end;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    final Range other = (Range) obj;
    if (end != other.end) return false;
    if (start != other.start) return false;
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "Range(" + getStart()
        + (isCollapsed() ? "" : "-" + getEnd())
        + ")";
  }

}
