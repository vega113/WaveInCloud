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
package org.waveprotocol.box.webclient.client;

import org.waveprotocol.box.webclient.client.CcStackManager.SimpleCcDocument;
import org.waveprotocol.wave.concurrencycontrol.wave.CcBasedWaveView;
import org.waveprotocol.wave.concurrencycontrol.wave.CcBasedWaveViewImpl;
import org.waveprotocol.wave.concurrencycontrol.wave.CcDataDocumentImpl;
import org.waveprotocol.wave.concurrencycontrol.wave.CcDocument;
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.operation.SilentOperationSink;
import org.waveprotocol.wave.model.schema.conversation.ConversationSchemas;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;

import java.util.HashMap;

public class SimpleCcDocumentFactory implements
    CcBasedWaveViewImpl.CcDocumentFactory<CcDocument> {

  HashMap<WaveletId, HashMap<String, CcDocument>> map = new HashMap<WaveletId, HashMap<String, CcDocument>>();
  HashMap<WaveletId, OpBasedWavelet> waveletMap = new HashMap<WaveletId, OpBasedWavelet>();
  private CcBasedWaveView view;

  private HashMap<String, CcDocument> getWaveletIdMap(WaveletId waveletId) {
    if (!map.containsKey(waveletId)) {
      map.put(waveletId, new HashMap<String, CcDocument>());
    }
    return map.get(waveletId);
  }

  @Override
  public CcDocument get(WaveletId waveletId, String blipId) {
    HashMap<String, CcDocument> map = getWaveletIdMap(waveletId);
    if (map.containsKey(blipId)) {
      return map.get(blipId);
    }
    return null;
  }

  @Override
  public CcDocument create(final WaveletId waveletId, final String docId,
      DocInitialization content) {
    if (IdUtil.isBlipId(docId)) {
      // create editor here.
      BlipView blipView = new BlipView(content);
      CcDocument doc = new SimpleCcDocument(blipView);
      blipView.getEditor().setOutputSink(
          new SilentOperationSink<BufferedDocOp>() {
            private SilentOperationSink<BufferedDocOp> sink;

            @Override
            public void consume(BufferedDocOp op) {
              if (sink == null) {
                sink = view.getWavelet(waveletId).getOpBasedWavelet().getDocumentOperationSink(
                    docId);
              }
              sink.consume(op);
            }
          });
      HashMap<String, CcDocument> map = getWaveletIdMap(waveletId);
      map.put(docId, doc);
      return doc;
    }

    CcDataDocumentImpl ccDataDocumentImpl = new CcDataDocumentImpl(
        new ConversationSchemas().getSchemaForId(waveletId, docId), content);
    ccDataDocumentImpl.init(new SilentOperationSink<BufferedDocOp>() {
      private SilentOperationSink<BufferedDocOp> sink;

      @Override
      public void consume(BufferedDocOp op) {
        if (sink == null) {
          sink = view.getWavelet(waveletId).getOpBasedWavelet().getDocumentOperationSink(
              docId);
        }
        sink.consume(op);
      }
    });

    return ccDataDocumentImpl;
  }

  public void setView(CcBasedWaveView view) {
    this.view = view;
  }
}