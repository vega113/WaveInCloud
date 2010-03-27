// Copyright 2010 Google Inc. All Rights Reserved.
package org.waveprotocol.wave.model.document.indexed;


import junit.framework.TestCase;

import java.util.Arrays;
import java.util.List;

/**
 * Test of xml parsing.
 *
*
 */
public class SimpleXmlParserTest extends TestCase {

  private static void assertSplitMatch(List<String> expected, String text, String sep) {
    assertEquals(expected, Arrays.asList(SimpleXmlParser.split(text, sep)));
  }

  public void testSplit() {
    assertSplitMatch(Arrays.asList("a=", "b", " c=", "d"), "a='b' c='d'", "'");
    assertSplitMatch(Arrays.asList("a=", "b", " c=", "d"), "a=''b'' c=''d''", "''");
    assertSplitMatch(Arrays.<String>asList(), "", "'");
    assertSplitMatch(Arrays.asList("a=", "b", " c=", ""), "a='b' c=''", "'");
  }

}
