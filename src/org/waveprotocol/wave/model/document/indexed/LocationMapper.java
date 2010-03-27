// Copyright 2008 Google Inc. All Rights Reserved.
package org.waveprotocol.wave.model.document.indexed;

import org.waveprotocol.wave.model.document.util.Point;


/**
 * Provides methods for mapping between nodes and int locations
 *
 * @author danilatos@google.com (Daniel Danilatos)
 * @param <N> Node type
 */
public interface LocationMapper<N> extends SizedObject {

  /**
   * Gets the point in the DOM indexed by a given location.
   *
   * @param location a location.
   * @return the point at the given location.
   */
  Point<N> locate(int location);

  /**
   * Gets the location of a given node in the DOM.
   *
   * @param node a DOM node.
   * @return the location of the given node.
   */
  int getLocation(N node);

  /**
   * Gets the location of a given point in the DOM.
   *
   * @param point a point in the DOM.
   * @return the location of the given point.
   */
  int getLocation(Point<N> point);
}
