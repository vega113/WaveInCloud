// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document;

import java.util.Map;

/**
 * A factory for creating a document by specifying its only
 * immutable component, the document element. (Though the attributes of
 * the document element of course can change later).
 *
 * @param <D> document type produced by this builder
*
 */
public interface DocumentFactory<D> {

  /**
   * Creates a document, with a specified document element.
   *
   * @param tagName     tag name for the document element
   * @param attributes  attributes for the document element
   * @return new document.
   */
  D create(String tagName, Map<String, String> attributes);
}
