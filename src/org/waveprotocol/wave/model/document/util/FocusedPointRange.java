// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.util.Preconditions;

/**
 * @author danilatos@google.com (Daniel Danilatos)
 *
 */
public class FocusedPointRange<N> {
  private final Point<N> anchor;
  private final Point<N> focus;
  private final boolean isCollapsed;

  /**
   * Constructs a collapsed range.
   *
   * @param collapsedAt The point at which the collapsed range is located.
   */
  public FocusedPointRange(Point<N> collapsedAt) {
    assert collapsedAt != null;
    anchor = collapsedAt;
    focus = collapsedAt;
    isCollapsed = true;
  }

  /**
   * @param anchor
   * @param focus
   */
  public FocusedPointRange(Point<N> anchor, Point<N> focus) {
    this.anchor = Preconditions.checkNotNull(anchor, "anchor");
    this.focus = Preconditions.checkNotNull(focus, "focus");
    this.isCollapsed = anchor.equals(focus);
  }

  /**
   * @return True if the range is collapsed
   */
  public boolean isCollapsed() {
    return isCollapsed;
  }

  /**
   * @return the anchor
   */
  public Point<N> getAnchor() {
    return anchor;
  }

  /**
   * @return the focus
   */
  public Point<N> getFocus() {
    return focus;
  }

  @Override
  public String toString() {
    return "FocusedPointRange(" + getAnchor() + " -> "  + getFocus() + ")";
  }

  @Override
  public final int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + anchor.hashCode();
    result = prime * result + focus.hashCode();
    return result;
  }

  @SuppressWarnings("unchecked")
  @Override
  public final boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (!(obj instanceof FocusedPointRange)) return false;
    FocusedPointRange other = (FocusedPointRange) obj;
    if (!anchor.equals(other.anchor)) return false;
    if (!focus.equals(other.focus)) return false;
    return true;
  }


}
