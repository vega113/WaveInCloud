// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.util;

/**
 * Exposes the interface for retrieving style properties of an element.
 *
*
 *
 * @param <N> The type of Node
 * @param <E> The type of Element
 * @param <T> The type of TextNode
 */
public interface ElementStyleView<N, E extends N, T extends N>
    extends ReadableDocumentView<N, E, T> {
  /**
   * Gets the value of a style property on an element. Returns null if the
   * property was not set.
   *
   * @param element The element whose style attribute to get.
   * @param name The name of the style property to get the value for.
   * @return The value of the style property or null if it does not exist.
   */
  String getStylePropertyValue(E element, String name);
}
