// Copyright 2008 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.dom;

/**
 * An interface representing a collection of nodes that can be accessed by name.
 * This is modelled on a subset of org.w3c.dom.NamedNodeMap.
 *
*
 */
public interface PrimitiveNamedNodeMap {

  /**
   * Returns the item at a given index in this map.
   *
   * @param index An index into this map.
   * @return The item at the given index in this map.
   */
  PrimitiveNode item(int index);

  /**
   * @return The number of items in this map.
   */
  int getLength();

}
