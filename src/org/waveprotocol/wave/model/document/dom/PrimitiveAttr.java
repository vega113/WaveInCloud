// Copyright 2008 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.dom;

/**
 * An interface representing an attribute in an element node. This is modeled
 * on a subset of org.w3c.dom.Attr.
 *
*
 */
public interface PrimitiveAttr extends PrimitiveNode {

  /**
   * @return The name of this attribute.
   */
  String getName();

  /**
   * @return The value of this attribute.
   */
  String getValue();

}
