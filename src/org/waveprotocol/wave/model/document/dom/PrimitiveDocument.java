// Copyright 2008 Google Inc. All Rights Reserved.
package org.waveprotocol.wave.model.document.dom;

/**
 * An interface representing an XML document. This is modelled on a subset of
 * org.w3c.dom.Document.
 */
public interface PrimitiveDocument extends PrimitiveNode {

  /**
   * This is a convenience attribute that allows direct access to the child
   * node that is the document element of the document.
   *
   * @return Root element of the document.
   */
  PrimitiveElement getDocumentElement();

  /**
   * Creates a node of the given tag name.
   *
   * @param tagName The tag name of the node to create
   * @return the new node, that does not have a parent assigned.
   */
  PrimitiveElement createElement(String tagName);

  /**
   * Creates a node that holds the given text.
   *
   * @param data the text to set in the text node.
   * @return the new node created.
   */
  PrimitiveText createTextNode(String data);

}
