// Copyright 2008 Google Inc. All Rights Reserved.
package org.waveprotocol.wave.model.document.indexed;

import org.waveprotocol.wave.model.document.ReadableDocument;
import org.waveprotocol.wave.model.document.ReadableWDocument;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.SuperSink;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.raw.TextNodeOrganiser;

/**
 * A DOM-style document with indexed nodes.
 *
 * @author danilatos@google.com (Daniel Danilatos)
*
 *
 * @param <N> The type of DOM nodes.
 * @param <E> The type of DOM Element nodes.
 * @param <T> The type of DOM Text nodes.
 */
public interface IndexedDocument<N, E extends N, T extends N> extends
    ReadableWDocument<N, E, T>, TextNodeOrganiser<T>, SuperSink {

  /**
   * Specialization of {@link ReadableDocument.Provider} for {@link IndexedDocument}.
   *
   * @param <D> document type produced
   */
  interface Provider<D extends IndexedDocument<?,?,?>> extends ReadableDocument.Provider<D> {

    /**
     * Creates a document from the provided operation
     *
     * The initialization MUST match the schema
     *
     * @return The created document
     */
    D build(DocInitialization operation, DocumentSchema schema);
  }

  DocumentSchema getSchema();
}
