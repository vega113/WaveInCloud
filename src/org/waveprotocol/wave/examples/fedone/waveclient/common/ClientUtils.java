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

package org.waveprotocol.wave.examples.fedone.waveclient.common;

import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.document.operation.DocInitializationCursor;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.impl.InitializationCursorAdapter;
import org.waveprotocol.wave.model.document.operation.impl.BufferedDocOpImpl.DocOpBuilder;
import org.waveprotocol.wave.model.wave.data.WaveViewData;
import org.waveprotocol.wave.model.wave.data.WaveletData;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Static utility methods for common use throughout the client.
 *
 *
 */
public class ClientUtils {
  /** The default ("root") document id.  */
  public static final String DEFAULT_DOCUMENT_ID = "default"; // TODO

  /**
   * Disallow construction.
   */
  private ClientUtils() {
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
      documentState.apply(new InitializationCursorAdapter(
          new DocInitializationCursor() {
            @Override
            public void characters(String s) {
              resultBuilder.append(s);
            }
            @Override public void annotationBoundary(AnnotationBoundaryMap map) {}
            @Override public void elementStart(String type, Attributes attrs) {}
            @Override public void elementEnd() {}
          }
      ));
    }
    return resultBuilder.toString();
  }

  /**
   * Render all of the documents in a wave as a single String, in the order
   * in which they are listed by wavelet.getDocumentIds().
   *
   * TODO: move this to the console package (...console.ConsoleUtils)
   *
   * @param wave wave to render
   * @return rendered wave
   */
  public static String renderDocuments(WaveViewData wave) {
    final StringBuilder doc = new StringBuilder();
    for (WaveletData wavelet : wave.getWavelets()) {
      doc.append(render(wavelet.getDocuments().values()));
    }
    return doc.toString();
  }

  /**
   * Get the digest (summary) of a wave as a String, by extracting some sort of relevant
   * information from the default document of the conversation root.
   *
   * @param wave to get digest of
   * @return digest of wave
   */
  public static String getWaveDigest(WaveViewData wave) {
    // Currently, just extract the first line of the rendered document
    return renderDocuments(wave).split("\n")[0];
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

    return builder.finish();
  }

  /**
   * Find the size of a document in number of characters and tags.
   *
   * @param doc document mutation to find the size
   * @return size of the document in number of characters and tags
   */
  public static int findDocumentSize(DocOp doc) {
    final AtomicInteger size = new AtomicInteger(0);

    doc.apply(new InitializationCursorAdapter(new DocInitializationCursor() {
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
    }));

    return size.get();
  }

  /**
   * @return a empty document
   */
  public static BufferedDocOp createEmptyDocument() {
    return new DocOpBuilder().finish();
  }

}