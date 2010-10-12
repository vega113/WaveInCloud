/**
 * Copyright 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.waveprotocol.box.client.webclient.waveclient.common;

import org.waveprotocol.box.client.common.IndexEntry;
import org.waveprotocol.box.common.CommonConstants;
import org.waveprotocol.box.common.DocumentConstants;
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
import org.waveprotocol.wave.model.operation.core.CoreWaveletDocumentOperation;
import org.waveprotocol.wave.model.wave.data.core.CoreWaveletData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Static utility methods for common use throughout the client.
 *
 *
 */
public class WebClientUtils {

  private static class CountingDocOpCursor implements DocOpCursor {

    int size = 0;

    @Override
    public void characters(String s) {
      size += s.length();
    }

    @Override
    public void elementStart(String key, Attributes attrs) {
      size += 1;
    }

    @Override
    public void elementEnd() {
      size += 1;
    }

    @Override
    public void annotationBoundary(AnnotationBoundaryMap map) {
    }

    @Override
    public void retain(int itemCount) {
    }

    @Override
    public void deleteCharacters(String chars) {
    }

    @Override
    public void deleteElementStart(String type, Attributes attrs) {
    }

    @Override
    public void deleteElementEnd() {
    }

    @Override
    public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
    }

    @Override
    public void updateAttributes(AttributesUpdate attrUpdate) {
    }
  }

  /**
   * Disallow construction.
   */
  private WebClientUtils() {
  }

  /**
   * Renders and concatenates all of the specified documents into a single
   * String in the order in which they appear in documentStates.
   *
   * @param documentStates the documents to render
   * @return A String containing the characters from all documents, in order
   */
  public static String render(Iterable<BufferedDocOp> documentStates) {
    final StringBuilder resultBuilder = new StringBuilder();
    for (BufferedDocOp documentState : documentStates) {
      documentState.apply(InitializationCursorAdapter.adapt(new DocOpCursor() {
        @Override
        public void characters(String s) {
          resultBuilder.append(s);
        }

        @Override
        public void annotationBoundary(AnnotationBoundaryMap map) {
        }

        @Override
        public void elementStart(String type, Attributes attrs) {
        }

        @Override
        public void elementEnd() {
        }

        @Override
        public void retain(int itemCount) {
        }

        @Override
        public void deleteCharacters(String chars) {
        }

        @Override
        public void deleteElementStart(String type, Attributes attrs) {
        }

        @Override
        public void deleteElementEnd() {
        }

        @Override
        public void replaceAttributes(Attributes oldAttrs, Attributes newAttrs) {
        }

        @Override
        public void updateAttributes(AttributesUpdate attrUpdate) {
        }
      }));
    }
    return resultBuilder.toString();
  }

  /**
   * Render all of the documents as a single String, in the order in
   * which they are listed by wavelet.getDocumentIds().
   *
   * TODO: move this to the console package (...console.ConsoleUtils)
   *
   * @param wave wave to render
   * @return rendered wave
   */
  public static String renderDocuments(WaveViewServiceImpl wave) {
    final StringBuilder doc = new StringBuilder();
    for (CoreWaveletData wavelet : wave.getWavelets()) {
      doc.append(render(wavelet.getDocuments().values()));
    }
    return doc.toString();
  }

  /**
   * Create a document operation for the insertion of text, inserting at a given
   * index.
   *
   * @param text text to insert
   * @param index index to insert at
   * @return document operation which inserts text at a given index
   */
  public static BufferedDocOp createTextInsertion(String text, int index,
      int previousTotalLength) {
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
   * Add record of a blip to the end of the manifest.
   */
  public static CoreWaveletDocumentOperation appendToManifest(
      BufferedDocOp manifestDocument, String blipId) {
    Map<String, String> newBlipAttributes = new HashMap<String, String>();
    newBlipAttributes.put(DocumentConstants.BLIP_ID, blipId);
    BufferedDocOp manifestUpdateOp = new DocOpBuilder().retain(
        findDocumentSize(manifestDocument) - 1).elementStart(
        DocumentConstants.BLIP, new AttributesImpl(newBlipAttributes)).elementEnd() // </blip>
    .retain(1) // retain </conversation>
    .build();
    return new CoreWaveletDocumentOperation(DocumentConstants.MANIFEST_DOCUMENT_ID,
        manifestUpdateOp);
  }

  public static CoreWaveletDocumentOperation insertReplyAfter(
      BufferedDocOp manifestDocument, final String parentBlipId,
      String childBlipId, boolean startThread) {
    // Fist, find the parent blip's offset
    class SearchCounter extends CountingDocOpCursor {
      int searchLocation = -1;

      @Override
      public void elementStart(String key, Attributes attrs) {
        super.elementStart(key, attrs);
        String blipId = attrs.get(DocumentConstants.BLIP_ID);
        if (parentBlipId.equals(blipId)) {
          searchLocation = size;
        }
      }
    }
    SearchCounter counter = new SearchCounter();
    manifestDocument.apply(InitializationCursorAdapter.adapt(counter));
    if (counter.searchLocation == -1) {
      throw new RuntimeException("Could not find parent blip " + parentBlipId);
    }

    // Set up attrs
    Map<String, String> newThreadAttributes = new HashMap<String, String>();
    newThreadAttributes.put(DocumentConstants.BLIP_ID, childBlipId);
    Map<String, String> newBlipAttributes = new HashMap<String, String>();
    newBlipAttributes.put(DocumentConstants.BLIP_ID, childBlipId);

    DocOpBuilder builder = new DocOpBuilder();
    int skipCount = counter.searchLocation; // Found parent's open

    // Create ops
    if (startThread) {
      builder.retain(skipCount); // through parent's open tag
      builder.elementStart(DocumentConstants.THREAD, new AttributesImpl(
          newThreadAttributes)); // <thread>
    } else {
      skipCount++; // through parent's end tag
      builder.retain(skipCount);
    }
    builder.elementStart(DocumentConstants.BLIP, new AttributesImpl(
        newBlipAttributes)); // <blip>
    builder.elementEnd(); // </blip>
    if (startThread) {
      builder.elementEnd(); // </thread>
    }
    builder.retain(counter.size - skipCount); // rest of document
    BufferedDocOp manifestUpdateOp = builder.build();

    return new CoreWaveletDocumentOperation(DocumentConstants.MANIFEST_DOCUMENT_ID,
        manifestUpdateOp);
  }

  /**
   * Find the size of a document in number of characters and tags.
   *
   * @param doc document mutation to find the size
   * @return size of the document in number of characters and tags
   */
  public static int findDocumentSize(DocOp doc) {
    final CountingDocOpCursor countingDocOpCursor = new CountingDocOpCursor();
    doc.apply(InitializationCursorAdapter.adapt(countingDocOpCursor));
    return countingDocOpCursor.size;
  }

  /**
   * @return a empty document
   */
  public static BufferedDocOp createEmptyDocument() {
    return new DocOpBuilder().build();
  }

  /**
   * Retrieve a list of index entries from an index wave.
   *
   * @param indexWave the wave to retrieve the index from
   * @return list of index entries
   */
  public static List<IndexEntry> getIndexEntries(WaveViewServiceImpl indexWave) {
    if (!indexWave.getWaveId().equals(CommonConstants.INDEX_WAVE_ID)) {
      throw new IllegalArgumentException(indexWave + " is not the index wave");
    }

    List<IndexEntry> indexEntries = new ArrayList<IndexEntry>();

    for (CoreWaveletData wavelet : indexWave.getWavelets()) {
      // The wave id is encoded as the wavelet id
      WaveId waveId = WaveId.deserialise(wavelet.getWaveletName().waveletId.serialise());
      String digest = WebClientUtils.renderSnippet(wavelet, Integer.MAX_VALUE);
      indexEntries.add(new IndexEntry(waveId, digest));
    }

    return indexEntries;
  }

  /**
   * Get the conversation root wavelet of a wave.
   *
   * @param wave to get conversation root of
   * @return conversation root wavelet of the wave
   */
  public static CoreWaveletData getConversationRoot(WaveViewServiceImpl wave) {
    return wave.getWavelet(getConversationRootId(wave));
  }

  /**
   * @return the conversation root wavelet id of a wave.
   */
  public static WaveletId getConversationRootId(WaveViewServiceImpl wave) {
    return getConversationRootId(wave.getWaveId());
  }

  /**
   * @return the conversation root wavelet id corresponding to a wave id.
   */
  public static WaveletId getConversationRootId(WaveId waveId) {
    return new WaveletId(waveId.getDomain(),
        IdConstants.CONVERSATION_ROOT_WAVELET);
  }

  /**
   * Returns a snippet or null.
   */
  public static String renderSnippet(CoreWaveletData conversationRoot,
      final int maxSnippetLength) {
    if (conversationRoot == null) {
      return null;
    }
    final Map<String, BufferedDocOp> documents = conversationRoot.getDocuments();
    if (documents == null) {
      return null;
    }
    BufferedDocOp bufferedDocOp = documents.get(DocumentConstants.CONVERSATION);
    if (bufferedDocOp == null) {
      // Render whatever data we have and hope its good enough
      return render(documents.values());
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
                BufferedDocOp document = documents.get(blipId);
                if (document == null) {
                  // We see this when a blip has been deleted
                  return;
                }
                sb.append(render(Arrays.asList(document)));
                sb.append(" ");
              }
            }
          }
        }));
    return sb.toString();
  }
}
