// Copyright 2008 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.util;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.ReadableDocument;

/**
 * Test cases for the text extractor.
 *
*
 */
public class TextExtractorTest extends TestCase {

  public void testOnlyText() {
    ReadableDocument<?, ?, ?> doc = DocProviders.ROJO.parse("<x>Hello</x>");
    String text = extractAll(doc);
    assertEquals("Hello", text);
  }

  public void testTextSplitByAnElement() {
    ReadableDocument<?, ?, ?> doc = DocProviders.ROJO.parse("<x>Hel<y></y>lo</x>");
    String text = extractAll(doc);
    assertEquals("Hello", text);
  }

  public void testTextSplitByTwoElements() {
    ReadableDocument<?, ?, ?> doc = DocProviders.ROJO.parse("<x>Hel<y></y>lo Wor<z></z>ld</x>");
    String text = extractAll(doc);
    assertEquals("Hello World", text);
  }

  public void testTextNestedInAnElement() {
    ReadableDocument<?, ?, ?> doc = DocProviders.ROJO.parse("<x>Hel<y>lo</y> Wor<z>l</z>d</x>");
    String text = extractAll(doc);
    assertEquals("Hello World", text);
  }
  private static <N> String extractAll(ReadableDocument<N, ?, ?> doc) {
    return extractAll1(doc);
  }

  private static <N, E extends N, T extends N> String extractAll1(ReadableDocument<N, E, T> doc) {
    return TextExtractor.extractInnerText(doc, doc.getDocumentElement());
  }
}
