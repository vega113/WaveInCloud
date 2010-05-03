// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document;

import org.waveprotocol.wave.model.document.indexed.LocationMapper;

import org.waveprotocol.wave.model.document.operation.DocInitialization;

/**
 * Document containing everything you need for a readable persistent view
 *
 * TODO(danilatos) Rename:
 *   ReadableDocument -> ReadableDomDocument
 *   RawDocument -> DomDocument
 *   ReadableWDocument -> ReadableDocument
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface ReadableWDocument<N, E extends N, T extends N> extends
    ReadableDocument<N, E, T>,
    ReadableAnnotationSet<String>,
    LocationMapper<N> {

  /**
   * @return Minimal normalized xml string.
   */
  String toXmlString();

  /**
   * @return this document represented as an initialization, suitable for
   *         {@link org.waveprotocol.wave.model.document.MutableDocument#hackConsume(org.waveprotocol.wave.model.document.operation.Nindo)
   *         copying} this document's content into another document.
   */
  DocInitialization toInitialization();
}
