// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.document.ReadableDocument;

import java.util.Iterator;

/**
 * Helpers for iterating through documents
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class DocIterate {

  /**
   *
   * @param <V> Value to map the nodes to
   */
  public interface DocIterationFilter<V> {
    /**
     * @param doc useful to keep most implementations as singletons
     * @param current the current node
     * @param stopAt the node to stop at (exclude)
     * @return the next node in the iteration
     */
    <N, E extends N, T extends N>
    N next(ReadableDocument<N, E, T> doc, N current, N stopAt);

    /**
     * @param doc useful to keep most implementations as singletons
     * @param node
     * @return the value the node should map to
     */
    <N, E extends N, T extends N>
    V value(ReadableDocument<N, E, T> doc, N node);
  }

  /**
   * Iterates using through the document using a DocIterationFilter.
   *
   * @param doc
   * @param startNode
   * @param iterateFunction
   * @param stopAt the exact meaning depends on the given iteration function, in
   *        particular whether the node is excluded on the way in or on the way
   *        out of traversal
   */
  public static <V, N, E extends N, T extends N> Iterable<V> iterate(
      final ReadableDocument<N, E, T> doc, final N startNode, final N stopAt,
      final DocIterationFilter<V> iterateFunction) {
    return new Iterable<V>() {
      @Override
      public Iterator<V> iterator() {
        return new Iterator<V>() {
          N next = startNode;

          @Override
          public boolean hasNext() {
            return next != null;
          }

          @Override
          public V next() {
            N currentNode = next;
            assert currentNode != null;
            next = iterateFunction.next(doc, currentNode, stopAt);
            return iterateFunction.value(doc, currentNode);
          }

          @Override
          public void remove() {
            throw new UnsupportedOperationException("remove");
          }
        };
      }
    };
  }

  static final DocIterationFilter<Object> FORWARD_DEPTH_FIRST_ITERATOR =
      new DocIterationFilter<Object>() {

        @Override
        public <N, E extends N, T extends N> N next(ReadableDocument<N, E, T> doc,
            N current, N stopAt) {
          return DocHelper.getNextNodeDepthFirst(doc, current, stopAt, true);
        }

        @Override
        public <N, E extends N,T extends N> N value(ReadableDocument<N, E, T> doc, N node) {
          return node;
        }
      };

  /**
   * Iterates using
   * {@link DocHelper#getNextNodeDepthFirst(ReadableDocument, Object, Object, boolean)}
   * The parameters to this method match those for that method.
   *
   * @param doc
   * @param startNode
   * @param stopAt
   */
  @SuppressWarnings("unchecked") // in the name of a singleton iterator
  public static <N, E extends N, T extends N> Iterable<N> deep(
      final ReadableDocument<N, E, T> doc, final N startNode, final N stopAt) {
    return iterate(doc, startNode, stopAt,
        (DocIterationFilter<N>) FORWARD_DEPTH_FIRST_ITERATOR);
  }


  static final DocIterationFilter<Object> REVERSE_DEPTH_FIRST_ITERATOR =
      new DocIterationFilter<Object>() {

        @Override
        public <N, E extends N, T extends N> N next(ReadableDocument<N, E, T> doc,
            N current, N stopAt) {
          return DocHelper.getPrevNodeDepthFirst(doc, current, stopAt, true);
        }

        @Override
        public <N, E extends N,T extends N> N value(ReadableDocument<N, E, T> doc, N node) {
          return node;
        }
      };

  /**
   * Iterates using
   * {@link DocHelper#getPrevNodeDepthFirst(ReadableDocument, Object, Object, boolean)}
   * The parameters to this method match those for that method.
   *
   * @param doc
   * @param startNode
   * @param stopAt
   */
  @SuppressWarnings("unchecked") // in the name of a singleton iterator
  public static <N, E extends N, T extends N> Iterable<N> deepReverse(
      final ReadableDocument<N, E, T> doc, final N startNode, final N stopAt) {
    return iterate(doc, startNode, stopAt,
        (DocIterationFilter<N>) REVERSE_DEPTH_FIRST_ITERATOR);
  }

  static class ElementIterator implements DocIterationFilter<Object> {
    final boolean rightwards;

    public ElementIterator(boolean rightwards) {
      this.rightwards = rightwards;
    }

    @Override
    public <N, E extends N, T extends N> N next(ReadableDocument<N, E, T> doc,
        N current, N stopAt) {
      N maybeNext = current;
      do {
        maybeNext = DocHelper.getNextOrPrevNodeDepthFirst(doc, maybeNext, stopAt, true, rightwards);
      } while (maybeNext != null && doc.asElement(maybeNext) == null);
      return maybeNext;
    }

    @Override
    public <N, E extends N,T extends N> N value(ReadableDocument<N, E, T> doc, N node) {
      assert doc.asElement(node) != null;
      return node;
    }
  }

  static final ElementIterator FORWARD_DEPTH_FIRST_ELEMENT_ITERATOR = new ElementIterator(true);
  static final ElementIterator REVERSE_DEPTH_FIRST_ELEMENT_ITERATOR = new ElementIterator(false);

  /**
   * Same as {@link #deep(ReadableDocument, Object, Object)}, but filters out
   * non-elements
   */
  @SuppressWarnings("unchecked") // in the name of a singleton iterator
  public static <N, E extends N, T extends N> Iterable<E> deepElements(
      final ReadableDocument<N, E, T> doc, final E startNode, final E stopAt) {
    return iterate(doc, startNode, stopAt,
        (DocIterationFilter<E>) (DocIterationFilter) FORWARD_DEPTH_FIRST_ELEMENT_ITERATOR);
  }

  /**
   * Same as {@link #deep(ReadableDocument, Object, Object)}, but filters out
   * non-elements
   */
  @SuppressWarnings("unchecked") // in the name of a singleton iterator
  public static <N, E extends N, T extends N> Iterable<E> deepElementsReverse(
      final ReadableDocument<N, E, T> doc, final E startNode, final E stopAt) {
    return iterate(doc, startNode, stopAt,
        (DocIterationFilter<E>) (DocIterationFilter) REVERSE_DEPTH_FIRST_ELEMENT_ITERATOR);
  }

  /**
   * Iteration filter that filters out elements by tag name.
   */
  static class ElementByTagNameIterator implements DocIterationFilter<Object> {
    private String tagName;

    public ElementByTagNameIterator(String tagName) {
      this.tagName = tagName;
    }

    @Override
    public <N, E extends N, T extends N> N next(ReadableDocument<N, E, T> doc,
        N current, N stopAt) {
      do {
        current = DocHelper.getNextNodeDepthFirst(doc, current, stopAt, true);
        E element = doc.asElement(current);
        if (element != null && doc.getTagName(element).equals(tagName)) {
          return element;
        }
      } while (current != null);
      return null;
    }

    @Override
    public <N, E extends N, T extends N> E value(ReadableDocument<N, E, T> doc, N node) {
      return doc.asElement(node);
    }
  }

  /**
   * Same as {@link #deepElements(ReadableDocument, Object, Object)}, but filters out
   * elements that do not match the given tag name.
   *
   * @return an iterable used for iterating matching elements.
   */
  @SuppressWarnings("unchecked") // in the name of a singleton iterator
  public static <N, E extends N, T extends N> Iterable<E> deepElementsWithTagName(
      final ReadableDocument<N, E, T> doc, String tagName) {
    return iterate(doc, DocHelper.getElementWithTagName(doc, tagName), null,
        (DocIterationFilter<E>) (DocIterationFilter) new ElementByTagNameIterator(tagName));
  }
}
