// Copyright 2008 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.document.indexed.IndexedDocProvider;
import org.waveprotocol.wave.model.document.indexed.IndexedDocument;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.document.raw.RawDocument;
import org.waveprotocol.wave.model.document.raw.RawDocumentProviderImpl;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.RawDocumentImpl;
import org.waveprotocol.wave.model.document.raw.impl.Text;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.OperationRuntimeException;
import org.waveprotocol.wave.model.operation.OperationSequencer;
import org.waveprotocol.wave.model.operation.OperationSink;

/**
 * Useful open implementations of {@code DocumentProvider} go here
 */
public class DocProviders {
  /**
   * RawDocument provider based on RawDocumentImpl as the document
   * implementation.
   */
  public final static RawDocument.Provider<RawDocumentImpl> ROJO =
      RawDocumentProviderImpl.create(RawDocumentImpl.BUILDER);

  /**
   * IndexedDocumentProvider with a substrate based on the "Rojo" dom
   * implementation
   */
  public final static IndexedDocProvider<Node, Element, Text, RawDocumentImpl> POJO =
      IndexedDocProvider.create(ROJO);

  /**
   * A simple sequencer
   * @param doc the document to apply non-invertible ops to, and get the invertible ones from
   */
  public final static <N, E extends N> OperationSequencer<Nindo> createTrivialSequencer(
      IndexedDocument<N, E, ? extends N> doc) {

    return createTrivialSequencer(doc, null);
  }

  /**
   * A simple sequencer
   * @param doc the document to apply non-invertible ops to, and get the invertible ones from
   * @param outputSink optional, may be null.
   */
  public final static <N, E extends N> OperationSequencer<Nindo> createTrivialSequencer(
      final IndexedDocument<N, E, ? extends N> doc, final OperationSink<DocOp> outputSink) {
    return new OperationSequencer<Nindo>() {
      @Override
      public void begin() {
      }

      @Override
      public void end() {
      }

      @Override
      public void consume(Nindo op) {
        try {
          DocOp docOp = doc.consumeAndReturnInvertible(op);
          if (outputSink != null) {
            outputSink.consume(docOp);
          }
        } catch (OperationException oe) {
          throw new OperationRuntimeException("DocProviders trivial sequencer consume failed.", oe);
        }
      }
    };
  }

  protected DocProviders() {
  }
}
