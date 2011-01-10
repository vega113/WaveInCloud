/**
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.waveprotocol.wave.model.testing;

import org.waveprotocol.wave.model.schema.SchemaProvider;
import org.waveprotocol.wave.model.wave.data.DocumentFactory;
import org.waveprotocol.wave.model.wave.data.impl.ObservablePluggableMutableDocument;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;

import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.SilentOperationSink;

import java.util.HashMap;
import java.util.Map;

/**
 * A document implementation and factory for use in tests.
 *
 */
public class FakeDocument extends ObservablePluggableMutableDocument {

  public static class Factory implements DocumentFactory<FakeDocument> {

    private final SchemaProvider schemas;

    private final Map<WaveletId, OpBasedWavelet> sinkFactories =
        new HashMap<WaveletId, OpBasedWavelet>();

    public static Factory create(SchemaProvider schemas) {
      return new Factory(schemas);
    }

    private Factory(SchemaProvider schemas) {
      this.schemas = schemas;
    }

    private DocumentSchema getSchemaForId(WaveletId waveletId, String documentId) {
      DocumentSchema result = schemas.getSchemaForId(waveletId, documentId);
      return (result != null) ? result : DocumentSchema.NO_SCHEMA_CONSTRAINTS;
    }

    @Override
    public FakeDocument create(final WaveletId waveletId, final String blipId,
        DocInitialization content) {
      FakeDocument doc =
          new FakeDocument(content, getSchemaForId(waveletId, blipId));

      // The sink factory will have a sink ready soon after this method returns.
      // This means we can't inject the sink now, since it's not ready. Instead,
      // we inject a sink that will pull out the real sink lazily.
      doc.init(new SilentOperationSink<DocOp>() {
        private SilentOperationSink<DocOp> output;

        private void fetchOutputSink() {
          OpBasedWavelet sinkFactory = sinkFactories.get(waveletId);
          if (sinkFactory == null) {
            throw new IllegalStateException("Can not create a document without a sink factory");
          } else {
            output = sinkFactory.getDocumentOperationSink(blipId);
          }
        }

        @Override
        public void consume(DocOp op) {
          if (output == null) {
            fetchOutputSink();
          }
          output.consume(op);
        }

      });
      return doc;
    }

    public void registerSinkFactory(OpBasedWavelet wavelet) {
      sinkFactories.put(wavelet.getId(), wavelet);
    }
  }

  public static Factory createFactory(SchemaProvider schemas) {
    return Factory.create(schemas);
  }

  private DocOp consumed;

  public FakeDocument(DocInitialization initial, DocumentSchema schema) {
    super(schema, initial);
  }

  @Override
  public void consume(DocOp op) throws OperationException {
    super.consume(op);
    this.consumed = op;
  }

  public DocOp getConsumed() {
    return consumed;
  }

  @Override
  public String toString() {
    return toXmlString();
  }
}
