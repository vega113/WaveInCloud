/**
 * Copyright 2009 Google Inc.
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

package org.waveprotocol.wave.examples.client.common;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import org.waveprotocol.wave.examples.fedone.common.DocumentConstants;
import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.document.operation.DocInitializationCursor;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.DocOpCursor;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.document.operation.impl.InitializationCursorAdapter;
import org.waveprotocol.wave.model.id.IdConstants;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.operation.core.CoreWaveletDelta;
import org.waveprotocol.wave.model.operation.core.CoreWaveletDocumentOperation;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.core.CoreWaveletData;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Static utility methods for common use throughout the client.
 *
 *
 */
public class ClientUtils {

  /**
   * Disallow construction.
   */
  private ClientUtils() {
  }

  /**
   * Concatenates all of the text of the specified documents into a single String in the order in
   * which they appear in documentStates.
   *
   * @param documentStates the documents to concatenate.
   * @return A String containing the characters from all documents, in order.
   */
  public static String collateText(Iterable<BufferedDocOp> documentStates) {
    final StringBuilder resultBuilder = new StringBuilder();
    for (BufferedDocOp documentState : documentStates) {
      documentState.apply(InitializationCursorAdapter.adapt(
          new DocOpCursor() {
            @Override
            public void characters(String s) {
              resultBuilder.append(s);
            }
            @Override public void annotationBoundary(AnnotationBoundaryMap map) {}
            @Override public void elementStart(String type, Attributes attrs) {}
            @Override public void elementEnd() {}
            @Override public void retain(int itemCount) {}
            @Override public void deleteCharacters(String chars) {}
            @Override public void deleteElementStart(String type, Attributes attrs) {}
            @Override public void deleteElementEnd() {}
            @Override public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {}
            @Override public void updateAttributes(AttributesUpdate attrUpdate) {}
          }
      ));
    }
    return resultBuilder.toString();
  }

  /**
   * Collect the text of all of the documents in a wave into a single String, in the order in which
   * the documents are listed by wavelet.getDocuments().
   *
   * TODO: move this to the console package (...console.ConsoleUtils)
   *
   * @param wave to collect the text from.
   * @return the collected text from the wave.
   */
  public static String collateText(ClientWaveView wave) {
    final StringBuilder doc = new StringBuilder();
    for (CoreWaveletData wavelet : wave.getWavelets()) {
      doc.append(ClientUtils.collateText(wavelet.getDocuments().values()));
    }
    return doc.toString();
  }

  /**
   * Create a document operation for the insertion of text, inserting at a given index.
   *
   * @param text text to insert
   * @param index index to insert at
   * @return document operation which inserts text at a given index
   */
  public static BufferedDocOp createTextInsertion(String text,
      int index, int previousTotalLength) {
    DocOpBuilder builder = new DocOpBuilder();

    if (index > 0) {
      builder.retain(index);
    }

    if (!text.isEmpty()) {
      builder.characters(text);
    }

    if (previousTotalLength > index) {
      builder.retain(previousTotalLength - index);
    }

    return builder.build();
  }


  /**
   * Create a mutation that creates a new blip containing the given text, places it in a new
   * blip, then adds a reference to the blip in the document manifest.
   *
   * @param manifestDocument document containing the manifest
   * @param author of the delta
   * @param newBlipId
   * @param text to place in the new blip
   */
  public static CoreWaveletDelta createAppendBlipDelta(BufferedDocOp manifestDocument,
      ParticipantId author, String newBlipId, String text) {
    if (text.length() == 0) {
      throw new IllegalArgumentException("Cannot append a empty String");
    } else {
      // Create new blip to insert at end of document.
      BufferedDocOp newBlipOp = new DocOpBuilder()
          .elementStart(DocumentConstants.BODY, Attributes.EMPTY_MAP)
          .elementStart(DocumentConstants.LINE, Attributes.EMPTY_MAP)
          .elementEnd() // </line>
          .characters(text)
          .elementEnd() // </body>
          .build();

      // An empty doc op to indicate a new blip is being created.
      BufferedDocOp emptyNewBlipOp = new DocOpBuilder().build();

      // Send the operation.
      ImmutableList<CoreWaveletDocumentOperation> operations = ImmutableList.of(
          new CoreWaveletDocumentOperation(newBlipId, emptyNewBlipOp),
          new CoreWaveletDocumentOperation(newBlipId, newBlipOp),
          appendToManifest(manifestDocument, newBlipId));
      return new CoreWaveletDelta(author, operations);
    }
  }

  /**
   * Add record of a blip to the end of the manifest.
   */
  public static CoreWaveletDocumentOperation appendToManifest(BufferedDocOp manifestDocument,
      String blipId) {
    BufferedDocOp manifestUpdateOp = new DocOpBuilder()
        .retain(findDocumentSize(manifestDocument) - 1)
        .elementStart(DocumentConstants.BLIP,
            new AttributesImpl(ImmutableMap.of(DocumentConstants.BLIP_ID, blipId)))
        .elementEnd() // </blip>
        .retain(1) // retain </conversation>
        .build();
    return new CoreWaveletDocumentOperation(DocumentConstants.MANIFEST_DOCUMENT_ID,
        manifestUpdateOp);
  }

  /**
   * @return an empty document
   */
  public static BufferedDocOp createEmptyDocument() {
    return new DocOpBuilder().build();
  }

  /**
   * Creates an empty manifest document.
   *
   * @return the manifest document.
   */
  public static BufferedDocOp createManifest() {
    return new DocOpBuilder().elementStart(DocumentConstants.CONVERSATION, Attributes.EMPTY_MAP)
        .elementEnd().build();
  }

  /**
   * Find the size of a document in number of characters and tags.
   *
   * @param doc document mutation to find the size
   * @return size of the document in number of characters and tags
   */
  public static int findDocumentSize(DocOp doc) {
    final AtomicInteger size = new AtomicInteger(0);

    doc.apply(InitializationCursorAdapter.adapt(new DocOpCursor() {
      @Override public void characters(String s) {
        size.getAndAdd(s.length());
      }

      @Override public void elementStart(String key, Attributes attrs) {
        size.incrementAndGet();
      }

      @Override public void elementEnd() {
        size.incrementAndGet();
      }

      @Override public void annotationBoundary(AnnotationBoundaryMap map) {}
      @Override public void retain(int itemCount) {}
      @Override public void deleteCharacters(String chars) {}
      @Override public void deleteElementStart(String type, Attributes attrs) {}
      @Override public void deleteElementEnd() {}
      @Override public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {}
      @Override public void updateAttributes(AttributesUpdate attrUpdate) {}
    }));

    return size.get();
  }

  /**
   * Returns all documents in the wave, aggregated from all the wavelets.
   *
   * @param wave to get the documents from.
   * @return map of all documents in the wave, aggregated from all the wavelets, and keyed by their
   * IDs.
   */
  public static Map<String, BufferedDocOp> getAllDocuments(ClientWaveView wave) {
    final Map<String, BufferedDocOp> documents = Maps.newHashMap();
    for (CoreWaveletData wavelet : wave.getWavelets()) {
      documents.putAll(wavelet.getDocuments());
    }
    return documents;
  }

  /**
   * Returns all participants in the wave, aggregated from all the wavelets.
   *
   * @param wave to get the participants from.
   * @return all participants in the wave, aggregated from all the wavelets.
   */
  public static Set<ParticipantId> getAllParticipants(ClientWaveView wave) {
    final Set<ParticipantId> participants = new HashSet<ParticipantId>();
    for (CoreWaveletData wavelet : wave.getWavelets()) {
      participants.addAll(wavelet.getParticipants());
    }
    return participants;
  }

  /**
   * Get the conversation root wavelet of a wave.
   *
   * @param wave to get conversation root of
   * @return conversation root wavelet of the wave
   */
  public static CoreWaveletData getConversationRoot(ClientWaveView wave) {
    return wave.getWavelet(getConversationRootId(wave));
  }

  /**
   * @return the conversation root wavelet id of a wave.
   */
  public static WaveletId getConversationRootId(ClientWaveView wave) {
    return getConversationRootId(wave.getWaveId());
  }

  /**
   * @return the conversation root wavelet id corresponding to a wave id.
   */
  public static WaveletId getConversationRootId(WaveId waveId) {
    return new WaveletId(waveId.getDomain(), IdConstants.CONVERSATION_ROOT_WAVELET);
  }

  /**
   * Returns a snippet or null.
   */
  public static String renderSnippet(final CoreWaveletData wavelet, final int maxSnippetLength) {
    BufferedDocOp bufferedDocOp = wavelet.getDocuments().get(DocumentConstants.CONVERSATION);
    if (bufferedDocOp == null) {
      // Render whatever data we have and hope its good enough
      return ClientUtils.collateText(wavelet.getDocuments().values());
    }

    final StringBuilder sb = new StringBuilder();
    bufferedDocOp.apply(InitializationCursorAdapter.adapt(
        new DocInitializationCursor() {
          @Override
          public void annotationBoundary(AnnotationBoundaryMap map) {
          }

          @Override
          public void characters(String chars) {
            // No chars in the conversation manifest
          }

          @Override
          public void elementEnd() {
          }

          @Override
          public void elementStart(String type, Attributes attrs) {
            if (sb.length() >= maxSnippetLength) {
              return;
            }

            if (DocumentConstants.BLIP.equals(type)) {
              String blipId = attrs.get(DocumentConstants.BLIP_ID);
              if (blipId != null) {
                BufferedDocOp document = wavelet.getDocuments().get(blipId);
                if (document == null) {
                  // We see this when a blip has been deleted
                  return;
                }
                sb.append(ClientUtils.collateText(Arrays.asList(document)));
                sb.append(" ");
              }
            }
          }
        }));
    return sb.toString();
  }
}
