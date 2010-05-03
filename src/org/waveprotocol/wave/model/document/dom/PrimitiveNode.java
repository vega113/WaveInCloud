// Copyright 2008 Google Inc. All Rights Reserved.
package org.waveprotocol.wave.model.document.dom;

/**
 * An interface representing a node in a DOM tree. This is modelled on a subset
 * of org.w3c.dom.Node.
 */
public interface PrimitiveNode {

  /**
   * Gets the parent node of this node.
   *
   * @return null, if there are no parent to this node.
   */
  PrimitiveNode getParentNode();

  /**
   * @return null if there are no children to this node.
   */
  PrimitiveNode getFirstChild();

  /**
   * @return null if there are no children to this node.
   */
  PrimitiveNode getLastChild();

  /**
   * @return the previous sibling to this node. null if there are no more siblings.
   */
  PrimitiveNode getPreviousSibling();

  /**
   * @return the next sibling to this node. null if there are no more siblings.
   */
  PrimitiveNode getNextSibling();

  /**
   * @return A map containing the attributes of this node if it is an element
   *         node, or null otherwise.
   */
  PrimitiveNamedNodeMap getAttributes();

  /**
   * Inserts a given node before a specified existing child of this node and
   * returns the inserted node.
   *
   * @param newChild The node to insert.
   * @param refChild The reference node, before which newChild is to be
   *        inserted.
   * @return The inserted node.
   */
  PrimitiveNode insertBefore(PrimitiveNode newChild, PrimitiveNode refChild);

  /**
   * Removes the given child node and returns it.
   *
   * @param oldChild The child node to remove.
   * @return The removed node.
   */
  PrimitiveNode removeChild(PrimitiveNode oldChild);

  /**
   * Adds the given node as the last child of this node and returns the added
   * node.
   *
   * @param newChild The node to add.
   * @return The added node.
   */
  PrimitiveNode appendChild(PrimitiveNode newChild);

  /**
   * Transforms the subtree rooted at this node into a normal form, such that
   * the subtree contains no empty text nodes and no two text nodes in the
   * subtree are adjacent siblings.
   */
  void normalize();

}
