/**
 * Copyright 2010 Google Inc.
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

package org.waveprotocol.wave.examples.fedone.robots.util;

import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.operation.SilentOperationSink;
import org.waveprotocol.wave.model.schema.SchemaProvider;
import org.waveprotocol.wave.model.wave.data.DocumentFactory;
import org.waveprotocol.wave.model.wave.data.DocumentOperationSink;
import org.waveprotocol.wave.model.wave.data.impl.ObservablePluggableMutableDocument;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;

/**
 * Document factory where a wavelet can be plugged in after construction. This
 * is useful for plugging in wavelets which can only be created after this
 * factory has been used.
 *
 * @author Lennard de Rijk (ljvderijk@google.com)
 */
public class WaveletPluginDocumentFactory implements DocumentFactory<DocumentOperationSink> {
  private final DocumentFactory<? extends ObservablePluggableMutableDocument> factory;

  /** Wavelet context in which this factory creates documents. */
  private OpBasedWavelet wavelet;

  public WaveletPluginDocumentFactory(SchemaProvider schemas) {
    this.factory = ObservablePluggableMutableDocument.createFactory(schemas);
  }

  @Override
  public DocumentOperationSink create(
      WaveletId waveletId, String blipId, DocInitialization content) {
    ObservablePluggableMutableDocument doc = factory.create(waveletId, blipId, content);
    doc.init(documentOutputSink(blipId));
    return doc;
  }

  /**
   * Sets the wavelet which document operation sink is used to consume ops.
   */
  public void setWavelet(OpBasedWavelet wavelet) {
    this.wavelet = wavelet;
  }

  private SilentOperationSink<? super BufferedDocOp> documentOutputSink(final String blipId) {
    return new SilentOperationSink<BufferedDocOp>() {
      private SilentOperationSink<BufferedDocOp> sink;

      @Override
      public void consume(BufferedDocOp op) {
        if (sink == null) {
          sink = wavelet.getDocumentOperationSink(blipId);
        }
        sink.consume(op);
      }
    };
  }
}
