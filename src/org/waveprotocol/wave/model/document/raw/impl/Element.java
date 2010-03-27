// Copyright 2008 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.raw.impl;

import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.indexed.NodeType;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.util.ElementManager;
import org.waveprotocol.wave.model.document.util.Property;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Mimics a DOM Element node.
 *
*
 */
public final class Element extends Node implements Doc.E {

  private final String tagName;
  private final Map<String,String> backingAttributeMap;
  private final Map<String,String> publicAttributeMap;

  private Map<Integer, Object> properties;


  /**
   * Element's manager for non-persistent properties
   */
  public static final ElementManager<Element> ELEMENT_MANAGER =
      new ElementManager<Element>() {
    public <T> void setProperty(Property<T> property, Element element, T value) {
      element.setProperty(property, value);
    }

    public <T> T getProperty(Property<T> property, Element element) {
      return element.getProperty(property);
    }

    public boolean isDestroyed(Element element) {
      return !element.isContentAttached();
    }
  };

  /**
   * Constructs an Element node with the given tag name.
   *
   * @param tagName The tag name the new Element node should have.
   */
  public Element(String tagName) {
    this.tagName = tagName;
    backingAttributeMap = new TreeMap<String,String>();
    publicAttributeMap = Collections.unmodifiableMap(backingAttributeMap);
  }

  public <T> void setProperty(Property<T> property, T value) {
    getTransientData().put(property.getId(), value);
  }

  @SuppressWarnings("unchecked")
  public <T> T getProperty(Property<T> property) {
    return (T) getTransientData().get(property.getId());
  }

  private Map<Integer, Object> getTransientData() {
    if (properties == null) {
      properties = new HashMap<Integer, Object>();
    }
    return properties;
  }

  public boolean isContentAttached() {
    return getParentElement() == null ? false : getParentElement().isContentAttached();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public short getNodeType() {
    return NodeType.ELEMENT_NODE;
  }

  /**
   * Gets the attributes of this node.
   *
   * @return A map that maps attribute names to attribute values for this node's
   *         attributes.
   */
  public Map<String,String> getAttributes() {
    return publicAttributeMap;
  }

  /**
   * Inserts the given node as a child of this node, at the point before the
   * given reference node.
   *
   * @param newChild The node to insert.
   * @param refChild The node before which to perform the insertion.
   * @return The inserted node.
   */
  public Node insertBefore(Node newChild, Node refChild) {
    if (refChild != null && refChild.getParentElement() != this) {
      throw new IllegalArgumentException("insertBefore: refChild is not child of parent");
    }
    if (newChild.isOrIsAncestorOf(this)) {
      throw new IllegalArgumentException("insertBefore: newChild is or is an ancestor of parent!");
    }

    if (newChild.parent != null) {
      newChild.parent.removeChild(newChild);
    }
    Node childBefore;
    if (refChild != null) {
      childBefore = refChild.previousSibling;
      refChild.previousSibling = newChild;
    } else {
      childBefore = lastChild;
      lastChild = newChild;
    }
    if (childBefore != null) {
      childBefore.nextSibling = newChild;
    } else {
      firstChild = newChild;
    }
    newChild.parent = this;
    newChild.previousSibling = childBefore;
    newChild.nextSibling = refChild;
    return newChild;
  }

  /**
   * Removes a child from this node.
   *
   * @param oldChild The child to remove.
   * @return The removed node.
   */
  public Node removeChild(Node oldChild) {
    if (oldChild.getParentElement() != this) {
      throw new IllegalArgumentException("removeChild: oldChild is not child of parent");
    }
    Node siblingBefore = oldChild.previousSibling;
    Node siblingAfter = oldChild.nextSibling;
    if (siblingBefore != null) {
      siblingBefore.nextSibling = siblingAfter;
    } else {
      firstChild = siblingAfter;
    }
    if (siblingAfter != null) {
      siblingAfter.previousSibling = siblingBefore;
    } else {
      lastChild = siblingBefore;
    }
    oldChild.previousSibling = null;
    oldChild.nextSibling = null;
    oldChild.parent = null;
    return oldChild;
  }

  /**
   * Gets this node's tag name.
   *
   * @return The tag name of this node.
   */
  public String getTagName() {
    return tagName;
  }

  /**
   * Gets an attribute of this node.
   *
   * @param name The name of the attribute to get.
   * @return The value of the attribute, or null if the attribute does not
   *         exist. Note that this does not mimic the DOM specification exactly.
   */
  public String getAttribute(String name) {
    return backingAttributeMap.get(name);
  }

  /**
   * Sets an attribute of this node.
   *
   * @param name The name of the attribute to set.
   * @param value The value that the attribute should have.
   */
  public void setAttribute(String name, String value) {
    backingAttributeMap.put(name, value);
  }

  /**
   * Removes an attribute from this node.
   *
   * @param name The name of the attribute to remove.
   */
  public void removeAttribute(String name) {
    backingAttributeMap.remove(name);
  }

  /** {@inheritDoc} */
  @Override
  public int calculateSize() {
    int size = 2;
    for (Node n = getFirstChild(); n != null; n = n.getNextSibling()) {
      size += n.calculateSize();
    }
    return size;
  }

  @Override
  public Element asElement() {
    return this;
  }

  @Override
  public Text asText() {
    return null;
  }

  /**
   * For debugging purposes only, very inefficient implementation!
   */
  @Override
  public String toString() {
    return XmlStringBuilder.createNode(
        RawDocumentImpl.BUILDER.create("x", Attributes.EMPTY_MAP), this).toString();
  }
}
