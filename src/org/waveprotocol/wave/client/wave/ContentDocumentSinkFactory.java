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
package org.waveprotocol.wave.client.wave;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.document.indexed.IndexedDocumentImpl;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.operation.SilentOperationSink;
import org.waveprotocol.wave.model.schema.SchemaProvider;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.StringMap;
import org.waveprotocol.wave.model.wave.data.DocumentFactory;
import org.waveprotocol.wave.model.wave.data.DocumentOperationSink;
import org.waveprotocol.wave.model.wave.data.impl.ObservablePluggableMutableDocument;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;

/**
 * A document factory for creating content documents if the document is a blip.
 *
 */
public final class ContentDocumentSinkFactory implements DocumentFactory<DocumentOperationSink> {

  static {
    // HACK: this should not be here.
    IndexedDocumentImpl.performValidation = false;
  }

  /**
   * The minimal set of registries required for documents to function correctly
   * (e.g., line-container behaviour). Ideally this would be empty, but there
   * are some legacy dependencies.
   */
  private final Registries baseRegistries;

  /** The base document factory used to generates non-blip documents */
  private final DocumentFactory<? extends ObservablePluggableMutableDocument> dataDocFactory;

  /** All the documents, indexed by conversation id then blip id. */
  private final StringMap<StringMap<ContentDocumentSink>> documents = CollectionUtils.createStringMap();

  /** All the registered wavelets, indexed by wavelet id. */
  private final StringMap<OpBasedWavelet> wavelets = CollectionUtils.createStringMap();

  @VisibleForTesting
  ContentDocumentSinkFactory(
      DocumentFactory<? extends ObservablePluggableMutableDocument> dataDocFactory,
      Registries baseRegistries) {
    this.dataDocFactory = dataDocFactory;
    this.baseRegistries = baseRegistries;
  }

  /**
   * Creates a factory/registry.
   *
   * @param schemas schema definition for documents created by this factory
   * @param baseRegistries base set of behavioral doodads required for documents
   *        to function correctly
   * @return a new factory.
   */
  public static ContentDocumentSinkFactory create(
      SchemaProvider schemas, Registries baseRegistries) {
    DocumentFactory<? extends ObservablePluggableMutableDocument> dataDocFactory =
        ObservablePluggableMutableDocument.createFactory(schemas);
    return new ContentDocumentSinkFactory(dataDocFactory, baseRegistries);
  }

  @Override
  public DocumentOperationSink create(final WaveletId waveletId, final String blipId,
      final DocInitialization content) {

    String waveletIdStr = waveletId.serialise();
    DocumentOperationSink sink;
    if (IdUtil.isBlipId(blipId)) {
      ContentDocumentSink document = new ContentDocumentSink(baseRegistries, content);
      StringMap<ContentDocumentSink> convDocuments = getConversationDocuments(waveletIdStr);
      Preconditions.checkState(!convDocuments.containsKey(blipId));
      convDocuments.put(blipId, document);
      sink = document;
    } else {
      sink = dataDocFactory.create(waveletId, blipId, content);
    }

    sink.init(documentOutputSink(waveletIdStr, blipId));
    return sink;
  }

  /**
   * Gets the document map for a particular conversation.
   *
   *  @param id conversation id
   */
  private StringMap<ContentDocumentSink> getConversationDocuments(String id) {
    StringMap<ContentDocumentSink> convDocuments = documents.get(id);
    if (convDocuments == null) {
      convDocuments = CollectionUtils.createStringMap();
      documents.put(id, convDocuments);
    }
    return convDocuments;
  }

  /**
   * Gets the output sink for a document.
   */
  private SilentOperationSink<DocOp> documentOutputSink(final String waveletId,
      final String docId) {
    return new SilentOperationSink<DocOp>() {
      private SilentOperationSink<DocOp> sink;

      @Override
      public void consume(DocOp op) {
        if (sink == null) {
          sink = wavelets.get(waveletId).getDocumentOperationSink(docId);
          if (sink == null) {
            throw new RuntimeException("containing wavelet not registered");
          }
        }
        sink.consume(op);
      }
    };
  }

  /**
   * Registers a wavelet. The outgoing operation-sink for a conversation
   * document will be pulled from its registered wavelet when it starts being
   * used.
   *
   * @param wavelet  wavelet to which outoing document operations are to be sent
   */
  public void registerOpBasedWavelet(OpBasedWavelet wavelet) {
    String waveletId = wavelet.getId().serialise();
    Preconditions.checkArgument(!wavelets.containsKey(waveletId));
    wavelets.put(waveletId, wavelet);
  }

  /**
   * Gets the document implementation for a blip.
   *
   * @param blip blip
   * @return document implementation for {@code blip}.
   */
  public ContentDocumentSink get(ConversationBlip blip) {
    return getSpecial(blip.getConversation().getId(), blip.getId());
  }

  /**
   * Reveals access to the special document implementation for conversational
   * blips.
   *
   * @param waveletId
   * @param docId
   * @return the special document implementation (content-document) for the
   *         document, if it has one. This returns null either if the document
   *         does not exist, or it is a regular non-special (data) document.
   */
  public ContentDocumentSink getSpecial(String waveletId, String docId) {
    StringMap<ContentDocumentSink> convDocuments = documents.get(waveletId);
    return convDocuments != null ? convDocuments.get(docId) : null;
  }
}
