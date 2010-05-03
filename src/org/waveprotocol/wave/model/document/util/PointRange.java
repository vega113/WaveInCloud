// Copyright 2008 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.util;


/**
 * Convenience type alias for a range represented by two Points.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class PointRange<N> {
  private final Point<N> first;
  private final Point<N> second;
  private final boolean isCollapsed;

  /**
   * Constructs a collapsed range.
   *
   * @param collapsedAt The point at which the collapsed range is located.
   */
  public PointRange(Point<N> collapsedAt) {
    assert collapsedAt != null;
    first = collapsedAt;
    second = collapsedAt;
    isCollapsed = true;
  }

  /** Constructor */
  public PointRange(Point<N> first, Point<N> second) {
    assert first != null && second != null;
    this.first = first;
    this.second = second;
    isCollapsed = first.equals(second);
  }

  /**
   * @return True if the range is collapsed
   */
  public boolean isCollapsed() {
    return isCollapsed;
  }

  /**
   * @return first
   */
  public Point<N> getFirst() {
    return first;
  }

  /**
   * @return second
   */
  public Point<N> getSecond() {
    return second;
  }

  // Eclipse generated methods below

  /** {@inheritDoc} */
  @Override
  public final int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((first == null) ? 0 : first.hashCode());
    result = prime * result + ((second == null) ? 0 : second.hashCode());
    return result;
  }

  /** {@inheritDoc} */
  @SuppressWarnings("unchecked")
  @Override
  public final boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof PointRange) {
      final PointRange other = (PointRange) obj;
      return first.equals(other.first) && second.equals(other.second);
    }
    return false;
  }

  @Override
  public String toString() {
    return isCollapsed() ? "Collapsed:[" + first + "]" : "Range:[" + first + ", " + second + "]";
  }
}
