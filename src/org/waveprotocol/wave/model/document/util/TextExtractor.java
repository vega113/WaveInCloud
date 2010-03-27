// Copyright 2008 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.document.ReadableDocument;

/**
 * Extracts the text content of subtrees in a readable document.
 *
*
 *
 * @param <N>
 * @param <E>
 * @param <T>
 */
public class TextExtractor<N, E extends N, T extends N> {
  private final ReadableDocument<N, E, T> document;

  public static <N, E extends N, T extends N> String
      extractInnerText(ReadableDocument<N, E, T> doc, E element) {
    return new TextExtractor<N, E, T>(doc).getInnerText(element);
  }

  public TextExtractor(ReadableDocument<N, E, T> document) {
    this.document = document;
  }

  public String getInnerText(E element) {
    StringBuilder builder = new StringBuilder();
    getInnerTextOfElement(element, builder);
    return builder.toString();
  }

  private void getInnerTextOfNode(N node, StringBuilder builder) {
    T maybeText = document.asText(node);
    if (maybeText != null) {
      getInnerTextOfText(maybeText, builder);
    } else {
      getInnerTextOfElement(document.asElement(node), builder);
    }
  }

  private void getInnerTextOfText(T text, StringBuilder builder) {
    builder.append(document.getData(text));
  }

  private void getInnerTextOfElement(E element, StringBuilder builder) {
    N child = document.getFirstChild(element);
    while (child != null) {
      getInnerTextOfNode(child, builder);
      child = document.getNextSibling(child);
    }
  }
}
