// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.document.ReadableDocument;
import org.waveprotocol.wave.model.util.Preconditions;

import java.util.Map;

/**
 * Convenience class, designed for subclassing to reduce boilerplate from
 * delegation.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 *
 * @param <N>
 * @param <E>
 * @param <T>
 */
public class IdentityView<N, E extends N, T extends N>
    implements ReadableDocumentView<N, E, T> {

  protected final ReadableDocument<N, E, T> inner;

  protected IdentityView(ReadableDocument<N, E, T> inner) {
    Preconditions.checkNotNull(inner, "IdentityView (or subclass): " +
    		"Inner document may not be null!");
    this.inner = inner;
  }

  /** {@inheritDoc} */
  public E asElement(N node) {
    return inner.asElement(node);
  }

  /** {@inheritDoc} */
  public T asText(N node) {
    return inner.asText(node);
  }

  /** {@inheritDoc} */
  public String getAttribute(E element, String name) {
    return inner.getAttribute(element, name);
  }

  /** {@inheritDoc} */
  public Map<String, String> getAttributes(E element) {
    return inner.getAttributes(element);
  }

  /** {@inheritDoc} */
  public String getData(T textNode) {
    return inner.getData(textNode);
  }

  /** {@inheritDoc} */
  public E getDocumentElement() {
    return inner.getDocumentElement();
  }

  /** {@inheritDoc} */
  public int getLength(T textNode) {
    return inner.getLength(textNode);
  }

  /** {@inheritDoc} */
  public short getNodeType(N node) {
    return inner.getNodeType(node);
  }

  /** {@inheritDoc} */
  public String getTagName(E element) {
    return inner.getTagName(element);
  }

  /** {@inheritDoc} */
  public boolean isSameNode(N node, N other) {
    return inner.isSameNode(node, other);
  }

  /** {@inheritDoc} */
  public N getFirstChild(N node) {
    return inner.getFirstChild(node);
  }

  /** {@inheritDoc} */
  public N getLastChild(N node) {
    return inner.getLastChild(node);
  }

  /** {@inheritDoc} */
  public N getNextSibling(N node) {
    return inner.getNextSibling(node);
  }

  /** {@inheritDoc} */
  public E getParentElement(N node) {
    return inner.getParentElement(node);
  }

  /** {@inheritDoc} */
  public N getPreviousSibling(N node) {
    return inner.getPreviousSibling(node);
  }

  /** {@inheritDoc} */
  public N getVisibleNode(N node) {
    return node;
  }

  /** {@inheritDoc} */
  public N getVisibleNodeFirst(N node) {
    return node;
  }

  /** {@inheritDoc} */
  public N getVisibleNodeLast(N node) {
    return node;
  }

  /** {@inheritDoc} */
  public N getVisibleNodeNext(N node) {
    return node;
  }

  /** {@inheritDoc} */
  public N getVisibleNodePrevious(N node) {
    return node;
  }
}
