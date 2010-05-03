// Copyright 2008 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.raw.impl;

import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.util.OffsetList;

/**
 * Mimics a DOM node.
 *
*
 */
public abstract class Node implements Doc.N {

  Element parent;
  Node firstChild;
  Node lastChild;
  Node previousSibling;
  Node nextSibling;
  private OffsetList.Container<Node> indexingContainer;

  /**
   * Gets the parent node of this node.
   *
   * @return The element node that is the parent of this node, or null if there is none.
   */
  public Element getParentElement() {
    return parent;
  }

  /**
   * @return The type of this node.
   */
  public abstract short getNodeType();

  /**
   * @return The first child of this node, or null if this node has no children.
   */
  public Node getFirstChild() {
    return firstChild;
  }

  /**
   * @return The last child of this node, or null if this node has no children.
   */
  public Node getLastChild() {
    return lastChild;
  }

  /**
   * @return The previous sibling of this node, or null if this node has no
   *         previous sibling.
   */
  public Node getPreviousSibling() {
    return previousSibling;
  }

  /**
   * @return The next sibling of this node, or null if this node has no next
   *         sibling.
   */
  public Node getNextSibling() {
    return nextSibling;
  }

  /**
   * @return The registered indexer of this node.
   */
  public OffsetList.Container<Node> getIndexingContainer() {
    return indexingContainer;
  }

  /**
   * Sets the indexer of this node.
   */
  public void setIndexingContainer(OffsetList.Container<Node> indexingContainer) {
    this.indexingContainer = indexingContainer;
  }

  /**
   * @param other
   * @return true if this node is equal to or is an ancestor of other
   */
  public boolean isOrIsAncestorOf(Node other) {
    while (other != null) {
      if (this == other) {
        return true;
      }
      other = other.getParentElement();
    }
    return false;
  }

  /**
   * Calculate the size (item count) of the node
   */
  public abstract int calculateSize();

  /**
   * @return this node as an element, if it is one, otherwise {@code null}.
   */
  public abstract Element asElement();

  /**
   * @return this node as a text node, if it is one, otherwise {@code null}.
   */
  public abstract Text asText();
}
