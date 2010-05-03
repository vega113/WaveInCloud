// Copyright 2008 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.dom;

/**
 * An interface representing nodes in a DOM tree that contain character data.
 * This is modelled on a subset of org.w3c.dom.CharacterData.
 *
*
 */
public interface PrimitiveCharacterData extends PrimitiveNode {

  /**
   * @return The character data contained in this node.
   */
  String getData();

  /**
   * Sets this node's character data.
   *
   * @param data The character data that this node is to have.
   */
  void setData(String data);

  /**
   * @return The size of the character data contained in this node, measured in
   *         16-bit units.
   */
  int getLength();

  /**
   * Inserts a string at the specified offset.
   *
   * @param offset The offset at which to insert the string.
   * @param arg The string to insert.
   */
  void insertData(int offset, String arg);

}
