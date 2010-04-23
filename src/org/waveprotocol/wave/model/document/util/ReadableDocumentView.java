// Copyright 2008 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.document.ReadableDocument;

/**
 * Extension, adding methods for filtered views to map nodes to ones visible in
 * the view. Each method will return the given node if it is visible, but have
 * different behaviour for acquiring a visible node if the given one is not.
 *
 * @param <N> Node
 * @param <E> Element
 * @param <T> Text
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface ReadableDocumentView<N,E extends N,T extends N> extends ReadableDocument<N,E,T> {
  /**
   * TODO(danilatos): Rename this to getVisibleNodeUpwards or something in a separate CL
   *
   * Given a node of the correct type that may or may not actually be in the
   * view, returns the nearest node that IS in the view by traversing upwards.
   *
   * @param node
   * @return A node in the view.
   */
  N getVisibleNode(N node);

  /**
   * Get the next visible node in a rightwards depth first traversal, starting from
   * the next sibling, if the given node is not visible
   *
   * @param node
   */
  N getVisibleNodeNext(N node);

  /**
   * Get the next visible node in a leftwards depth first traversal, starting from
   * the previous sibling, if the given node is not visible
   *
   * @param node
   */
  N getVisibleNodePrevious(N node);

  /**
   * Get the next visible node in a rightwards depth first traversal, starting from
   * the given node, if the given node is not visible
   *
   * @param node
   */
  N getVisibleNodeFirst(N node);

  /**
   * Get the next visible node in a leftwards depth first traversal, starting from
   * the given node, if the given node is not visible
   *
   * @param node
   */
  N getVisibleNodeLast(N node);


  /**
   * Notify the document that a particular point is about to be filtered against it.
   *
   * @param at
   */
  void onBeforeFilter(Point<N> at);
}
