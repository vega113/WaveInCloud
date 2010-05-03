// Copyright 2008 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.dom;

/**
 * An interface representing an element node in a DOM tree. This is a subset of
 * org.w3c.dom.Element.
 *
*
 */
public interface PrimitiveElement extends PrimitiveNode {

  /**
   * @return The tag name of the attribute. At least for now, includes namespace, if not html.
   *
   * TODO(danilatos): Check if this is the correct behaviour.
   */
  String getTagName();

  /**
   * Retrieves an attribute value from an element node given the attribute's
   * name.
   *
   * @param name The name of the attribute to retrieve.
   * @return The value of the attribute. This is an empty string if the
   *         attribute does not have a value.
   */
  String getAttribute(String name);

  /**
   * Retrieves an attribute value from an element node given the attribute's
   * name.
   *
   * @param name The name of the attribute to set.
   * @param value The value to which to set the attribute.
   */
  void setAttribute(String name, String value);

  /**
   * Removes an attribute from an element.
   *
   * @param name The name of the attribute to remove.
   */
  void removeAttribute(String name);

  /**
   * Determines whether an element has a value for an attribute, given the
   * attribute's name.
   *
   * @param name The name of the attribute.
   * @return Whether this element has the specified attribute.
   */
  boolean hasAttribute(String name);

}
