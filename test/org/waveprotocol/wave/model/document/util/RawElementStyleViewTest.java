// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.util;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.raw.RawDocumentProviderImpl;
import org.waveprotocol.wave.model.document.raw.impl.RawDocumentImpl;

/**
 * Tests logic of the rich text mutation builder.
 *
*
 */
public class RawElementStyleViewTest extends TestCase {
  public void testNoStyle() {
    RawElementStyleView view = parse("<p/>");
    assertEquals(null, view.getStylePropertyValue(view.getDocumentElement(), "foo"));
  }

  public void testEmptyStyle() {
    RawElementStyleView view = parse("<p style=\"\"/>");
    assertEquals(null, view.getStylePropertyValue(view.getDocumentElement(), "foo"));
  }

  public void testSingleStyle() {
    RawElementStyleView view = parse("<p style=\"foo: value1\"/>");
    assertEquals("value1", view.getStylePropertyValue(view.getDocumentElement(), "foo"));
    assertEquals(null, view.getStylePropertyValue(view.getDocumentElement(), "bar"));
  }

  public void testMultipleStyles() {
    RawElementStyleView view = parse("<p style=\"foo: value1; bar: value2\"/>");
    view.getDocumentElement();
    assertEquals("value1", view.getStylePropertyValue(view.getDocumentElement(), "foo"));
    assertEquals("value2", view.getStylePropertyValue(view.getDocumentElement(), "bar"));
    assertEquals(null   , view.getStylePropertyValue(view.getDocumentElement(), "baz"));
  }

  private RawElementStyleView parse(String xmlString) {
    return new RawElementStyleView(
        RawDocumentProviderImpl.create(RawDocumentImpl.BUILDER).parse(xmlString));
  }
}
