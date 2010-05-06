// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.document.raw.RawDocument;
import org.waveprotocol.wave.model.util.OffsetList;

import java.util.Map;

/**
 * A raw document implementation for a filtered view.
 *
 * When the presence of a transparent node conflicts with a raw document mutation
 * method, the transparent node loses and is split. The TransparentManager may
 * be used to specify what node to use as the second element in the split, or
 * can just return null for default behaviour (a shallow clone).
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class PersistentContent<N, E extends N, T extends N>
    extends FilteredView<N, E, T>
    implements RawDocument<N, E, T>, WritableLocalDocument<N, E, T> {

  /**
   * View of the hard elements plus text nodes in a document
   */
  public class HardContent extends FilteredView<N, E, T> {

    HardContent() {
      super(fullDoc);
    }

    @Override
    protected Skip getSkipLevel(N node) {
      E element = asElement(node);
      return (element == null || isHard(element))
          ? Skip.NONE : PersistentContent.this.getSkipLevel(node);
    }

  }

  private static final Object DEEP_TRANSPARENT_MARKER = new Object();

  private static final Property<Object> DEEP_TRANSPARENT = Property.immutable("p_deep_transparent");

  /** @see #makeHard(ElementManager, Object) */
  private static final Property<Object> HARD = Property.immutable("p_hard");

  /**
   * Makes a node "deep transparent" with respect to the persistent view.
   *
   * @param mgr
   * @param element
   */
  public static <E> void makeDeepTransparent(ElementManager<E> mgr, E element) {
    mgr.setProperty(DEEP_TRANSPARENT, element, DEEP_TRANSPARENT_MARKER);
  }

  /**
   * Transparent nodes: Hard vs Soft
   *
   * Soft nodes decorate content, and are inherited where possible. They are sliced through,
   * and around, and are transported with the content when moved. When slicing at the start
   * or end of the inside of one, we jump out and keep slicing upwards.
   *
   * Hard nodes aren't sliced. When slicing at the start or end of the inside of one, we
   * stop, rather than jump out. Hard transparent nodes must be shallow transparent.
   *
   * Persistent elements (not text nodes) are always hard.
   */
  public static <E> void makeHard(ElementManager<E> mgr, E element) {
    mgr.setProperty(HARD, element, HARD);
  }

  private final ElementManager<E> elementManager;

  private final RawDocument<N, E, T> fullDoc;

  /**
   * Provided for clarity. In this class we should never call an
   * unqualified dom method, instead explicitly use filteredDoc
   * so that accidental ommissions of "fullDoc." are easier to catch.
   */
  private final RawDocument<N, E, T> filteredDoc = this;

  private final HardContent hardDoc;

  /**
   * @param fullDoc underlying raw document
   * @param elementManager ElementManager implementation to delegate to
   */
  public PersistentContent(RawDocument<N, E, T> fullDoc, ElementManager<E> elementManager) {
    super(fullDoc);
    this.fullDoc = fullDoc;
    this.elementManager = elementManager;
    makeHard(elementManager, fullDoc.getDocumentElement());
    hardDoc = new HardContent();
  }

  public HardContent hardView() {
    return hardDoc;
  }

  @Override
  protected Skip getSkipLevel(N node) {
    if (isPersistent(node) || node == fullDoc.getDocumentElement()) {
      return Skip.NONE;
    } else if (isDeepTransparent(node)) {
      return Skip.DEEP;
    } else {
      return Skip.SHALLOW;
    }
  }

  private boolean isDeepTransparent(N node) {
    E element = asElement(node);
    return element == null || getProperty(DEEP_TRANSPARENT, element) != null;
  }

  private boolean isHard(N node) {
    E element = asElement(node);
    return element == null || isPersistent(element) || getProperty(HARD, element) != null;
  }

  //////////////

  /** {@inheritDoc} */
  public E transparentCreate(String tagName, Map<String, String> attributes,
      E parent, N nodeAfter) {

    Point.checkRelationship(fullDoc, parent, nodeAfter, "transparentCreate");

    E el = fullDoc.createElement(tagName, attributes, parent, nodeAfter);

    return el;
  }

  /** {@inheritDoc} */
  public T transparentCreate(String text, E parent, N nodeAfter) {
    Point.checkRelationship(fullDoc, parent, nodeAfter, "transparentCreate");
    if (isPersistent(parent)) {
      throw new IllegalArgumentException("transparentCreate: " +
          "Cannot create a local text node inside a persistent element, must " +
          "wrap the local text node in a local element");
    }

    T tx = fullDoc.createTextNode(text, parent, nodeAfter);

    return tx;
  }

  @Override
  public void transparentSetAttribute(E element, String name, String value) {
    if (isPersistent(element)) {
      throw new IllegalArgumentException("transparentSetAttribute: " +
          "node must not be persistent");
    }

    if (value != null) {
      fullDoc.setAttribute(element, name, value);
    } else {
      fullDoc.removeAttribute(element, name);
    }
  }

  /** {@inheritDoc} */
  public void transparentUnwrap(E element) {
    if (isPersistent(element)) {
      throw new IllegalArgumentException(
          "transparentUnwrap: Cannot directly manipulate persistent elements");
    }

    E parent = fullDoc.getParentElement(element);
    fullDoc.insertBefore(parent, fullDoc.getFirstChild(element), null, element);
    fullDoc.removeChild(parent, element);
  }

  /** {@inheritDoc} */
  public void transparentDeepRemove(N node) {
    if (isPersistent(node) || filteredDoc.getFirstChild(node) != null) {
      throw new IllegalArgumentException(
          "transparentDeepRemove: Cannot directly manipulate persistent elements");
    }

    fullDoc.removeChild(fullDoc.getParentElement(node), node);
  }

  /** {@inheritDoc} */
  public void transparentMove(E newParent, N fromIncl,
      N toExcl, N refChild) {

    if (refChild != null && fullDoc.getParentElement(refChild) != newParent) {
      throw new IllegalArgumentException("refChild must be a child of newParent");
    }
    if (toExcl != null) {
      for (N node = fromIncl; node != toExcl; node = fullDoc.getNextSibling(node)) {
        if (node == null) {
          throw new IllegalArgumentException(
              "toExcl must be null or be an indirect, following sibling of fromIncl");
        }
      }
    }

    // XXX(danilatos): DANGEROUS - need more validation that persistent view is not affected

    fullDoc.insertBefore(newParent, fromIncl, toExcl, refChild);
  }

  ///////////////

  /**
   * This implementation slices through any transparent nodes
   *
   * {@inheritDoc}
   */
  @SuppressWarnings("unchecked")
  public N insertBefore(E parent, N fromIncl, N toExcl, N refChild) {

    // Handle the simplest case explicitly
    if (fromIncl == toExcl) {
      return fromIncl;
    }

    // WARNING(danilatos): This code has NOT been updated to take into account hard elements.
    // This means that if, e.g., a split is executed on a line container, it will explode.

    // TODO(danilatos): This code might not work 100% of the time for uses other
    // than split and join - specifically, transparent nodes might end up nested,
    // when they shouldn't.
    // One of the restrictions is that refChild must be null, which is asserted
    // below, but there are others.


    if (refChild != null) {
      // Just to be safe. But it might actually be OK to remove this exception.
      throw new UnsupportedOperationException("insertBefore: Non-null refChild not implemented");
    }

    // We want right affinity for everything, except when fromIncl is the first child
    // (of the old parent, in the filtered view), to avoid leaving anything behind in
    // the old parent
    if (filteredDoc.getPreviousSibling(fromIncl) == null) {
      fromIncl = fullDoc.getFirstChild(filteredDoc.getParentElement(fromIncl));
    } else {
      fromIncl = transparentSlice(fromIncl);
    }
    toExcl = toExcl == null ? null : transparentSlice(toExcl);
    refChild = transparentSlice(refChild);

    return fullDoc.insertBefore(parent, fromIncl, toExcl, refChild);
  }

  /** {@inheritDoc} */
  public N insertBefore(E parent, N newChild, N refChild) {
    return insertBefore(parent,
        newChild, filteredDoc.getNextSibling(newChild), refChild);
  }

  /** {@inheritDoc} */
  public void removeChild(E parent, N oldChild) {
    fullDoc.removeChild(fullDoc.getParentElement(oldChild), oldChild);
  }

  private boolean isPersistent(N node) {
    return fullDoc.getIndexingContainer(node) != null;
  }

  @Override
  public N transparentSlice(N splitAt) {
    if (splitAt == null) {
      return null;
    }

    E parent = fullDoc.getParentElement(splitAt);

    if (parent == null) {
      throw new IllegalArgumentException("transparentSlice: Cannot split before the root element");
    }

    while (!isHard(parent)) {
      // Calculate it now because we might munge up the dom in subsequent code
      E newParent = fullDoc.getParentElement(parent);

      if (newParent == null) {
        throw new RuntimeException("The root node is not persistent!?!?");
      }

      // Try to avoid splitting if possible
      if (splitAt == fullDoc.getFirstChild(parent)) {
        splitAt = parent;
      } else {
        // Need to split
        E newSibling;
        N nodeAfter = fullDoc.getNextSibling(parent);
        newSibling = createShallowCopy(parent, newParent, nodeAfter);
        fullDoc.insertBefore(newSibling, splitAt, null, null);
        splitAt = newSibling;
      }

      parent = newParent;
    }

    return splitAt;
  }

  private E createShallowCopy(E node, E newParent, N nodeAfter) {
    return fullDoc.createElement(
        fullDoc.getTagName(node), fullDoc.getAttributes(node), newParent, nodeAfter);
  }

  /** {@inheritDoc} */
  public T mergeText(T secondSibling) {
    // TODO(danilatos): Proper implementation
    // Return null when persistent view previous sibling of secondSibling is
    // not its sibling in the full view.
    return null;
  }

  // Simple delegation

  /** {@inheritDoc} */
  public void appendData(T textNode, String arg) {
    fullDoc.appendData(textNode, arg);
  }

  /** {@inheritDoc} */
  public E createElement(String tagName, Map<String, String> attributes, E parent, N nodeAfter) {
    nodeAfter = transparentSlice(nodeAfter);
    if (nodeAfter != null) {
      parent = fullDoc.getParentElement(nodeAfter);
    }
    return fullDoc.createElement(tagName, attributes, parent, nodeAfter);
  }

  /** {@inheritDoc} */
  public T createTextNode(String data, E parent, N nodeAfter) {
    nodeAfter = transparentSlice(nodeAfter);
    if (nodeAfter != null) {
      parent = fullDoc.getParentElement(nodeAfter);
    }
    return fullDoc.createTextNode(data, parent, nodeAfter);
  }

  /** {@inheritDoc} */
  public void deleteData(T textNode, int offset, int count) {
    fullDoc.deleteData(textNode, offset, count);
  }

  /** {@inheritDoc} */
  public void insertData(T textNode, int offset, String arg) {
    fullDoc.insertData(textNode, offset, arg);
  }

  /** {@inheritDoc} */
  public void removeAttribute(E element, String name) {
    fullDoc.removeAttribute(element, name);
  }

  /** {@inheritDoc} */
  public void setAttribute(E element, String name, String value) {
    fullDoc.setAttribute(element, name, value);
  }

  /** {@inheritDoc} */
  public T splitText(T textNode, int offset) {
    return fullDoc.splitText(textNode, offset);
  }

  /** {@inheritDoc} */
  public void setIndexingContainer(N domNode, OffsetList.Container<N> indexingNode) {
    fullDoc.setIndexingContainer(domNode, indexingNode);
  }

  /** {@inheritDoc} */
  public OffsetList.Container<N> getIndexingContainer(N domNode) {
    return fullDoc.getIndexingContainer(domNode);
  }

  @Override
  public <X> X getProperty(Property<X> property, E element) {
    return elementManager.getProperty(property, element);
  }

  @Override
  public boolean isDestroyed(E element) {
    return elementManager.isDestroyed(element);
  }

  @Override
  public <X> void setProperty(Property<X> property, E element, X value) {
    elementManager.setProperty(property, element, value);
  }

  @Override
  public String toString() {
    return "PersistentContent " + XmlStringBuilder.innerXml(this);
  }
}