// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.raw;

/**
 * Interface for making changes to text nodes, such that the XML itself is not
 * affected.
 *
 * @param <T> Text node type
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
// TODO(danilatos): Consider merging this interface elsewhere.
public interface TextNodeOrganiser<T> {

  /**
   * Splits the given text node at the given offset, and return the resulting
   * second node.
   *
   * If offset is zero, no split occurs, and the given text node is returned. If
   * the offset is greater than, or equal to, the length of the given text node,
   * null will be returned.
   *
   * @param textNode
   * @param offset
   * @return the resulting second node, or null if the split failed
   */
  T splitText(T textNode, int offset);

  /**
   * Merges secondSibling and its previous sibling, which must exist and be a
   * text node.
   *
   * The merge may fail, if it is not possible for the text node to merge with
   * its previous sibling, even if it too is a text node (perhaps because this
   * is a filtered view of the document, and in some other view they are not
   * siblings).
   *
   * @param secondSibling
   * @return the first sibling, which is now merged with secondSibling, or null
   *         if the merge failed.
   */
  T mergeText(T secondSibling);
}
