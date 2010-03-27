// Copyright 2008 Google Inc. All Rights Reserved.
package org.waveprotocol.wave.model.document.dom;

/**
 * An interface representing a text node in a DOM tree. This is modelled on a
 * subset of org.w3c.dom.Text.
 */
public interface PrimitiveText extends PrimitiveCharacterData {

  /**
   * Splits the text node into two nodes at the given offset. The object itself
   * will contain all the text before the offset, and the returned text node
   * object will contain all the text after the offset. Both text nodes will
   * remain in the DOM tree as siblings.
   *
   * @param offset The offset of the point at which to split the node.
   * @return The text node containing all text after the split point.
   */
  PrimitiveText splitText(int offset);

}
