// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.util;


import org.waveprotocol.wave.model.util.Preconditions;

/**
 * Similar to a {@link Range}, except it is not canonicalised to have a start
 * &lt;= end. Instead, it has an "anchor" and a "focus", which do not have
 * ordering constraints
 *
 * Start and end are also provided for a canonicalised view.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public final class FocusedRange {

  private final int anchor;
  private final int focus;

  private Range range;

  /**
   * Construct a range
   *
   * @param anchor
   * @param focus
   */
  public FocusedRange(int anchor, int focus) {
    if (anchor < 0 || focus < 0) {
      Preconditions.illegalArgument("Bad focused range: (" + anchor + ", " + focus + ")");
    }
    this.anchor = anchor;
    this.focus = focus;
  }

  /**
   * Create from an ordered range
   *
   * @param range
   * @param ordered
   */
  public FocusedRange(Range range, boolean ordered) {
    if (ordered) {
      this.anchor = range.getStart();
      this.focus = range.getEnd();
    } else {
      this.anchor = range.getEnd();
      this.focus = range.getStart();
    }
  }

  /**
   * Construct a collapsed range
   *
   * @param collapsedAt
   */
  public FocusedRange(int collapsedAt) {
    this(collapsedAt, collapsedAt);
  }

  /**
   * @return anchor location, may or may not be before the focus
   */
  public int getAnchor() {
    return anchor;
  }

  /**
   * @return focus location, may or may not be before the anchor
   */
  public int getFocus() {
    return focus;
  }

  /**
   * @return true if the range is collapsed
   */
  public boolean isCollapsed() {
    return anchor == focus;
  }

  /**
   * @return true if the anchor is less than or equal to the focus
   */
  public boolean isOrdered() {
    return anchor <= focus;
  }

  /**
   * Get an guaranteed ordered range out of the current possibly unordered
   * range.
   *
   * The return value is cached
   */
  public Range asRange() {
    if (range == null) {
      range = anchor < focus ? new Range(anchor, focus) : new Range(focus, anchor);
    }
    return range;
  }

  @Override
  public int hashCode() {
    return anchor + 37 * focus;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    final FocusedRange other = (FocusedRange) obj;
    if (focus != other.focus) return false;
    if (anchor != other.anchor) return false;
    return true;
  }

  @Override
  public String toString() {
    return "FocusedRange(" + getAnchor()
        + (isCollapsed() ? "" : "->" + getFocus())
        + ")";
  }

}
