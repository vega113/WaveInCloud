// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.util;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.raw.impl.Element;
import org.waveprotocol.wave.model.document.raw.impl.Node;
import org.waveprotocol.wave.model.document.raw.impl.Text;

/**
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class DocIterateTest extends TestCase {

  /***/
  public void testIteration() {
    MutableDocument<Node, Element, Text> doc = ContextProviders.createTestPojoContext(
        "<x>hello</x><y><yy>blah</yy>yeah</y><z>final</z>", null, null, null,
        DocumentSchema.NO_SCHEMA_CONSTRAINTS).document();

    int i;
    Element root = doc.getDocumentElement();
    Node x = root.getFirstChild();
    Node z = root.getLastChild();
    Node y = z.getPreviousSibling();

    for (Node end : new Node[]{z, root, null}) {
      i = 0;
      for (Node n : DocIterate.deep(doc, z.getFirstChild(), end)) {
        assertSame(z.getFirstChild(), n);
        i++;
      }
      assertEquals(1, i);

      i = 0;
      for (Node n : DocIterate.deep(doc, z, end)) {
        assertSame(i == 0 ? z : z.getFirstChild(), n);
        i++;
      }
      assertEquals(2, i);

      i = 0;
      for (Node n : DocIterate.deep(doc, root, end)) {
        switch (i) {
          case 0: assertSame(root, n); break;
          case 3: assertSame(y, n); break;
          case 4: assertSame(y.getFirstChild(), n); break;
          case 5: assertSame(y.getFirstChild().getFirstChild(), n); break;
          case 6: assertSame(y.getFirstChild().getNextSibling(), n); break;
          default: assertNotNull(n); break;
        }
        i++;
      }
      assertEquals(end == z ? 7 : 9, i);
    }

    i = 0;
    for (Element e : DocIterate.deepElements(doc, root, root)) {
      i++;
    }
    assertEquals(5, i);

    i = 0;
    for (Element e : DocIterate.deepElementsWithTagName(doc, "x")) {
      i++;
    }
    assertEquals(1, i);
  }

  /***/
  public void testElementsByTagNameIteration() {
    MutableDocument<Node, Element, Text> doc = ContextProviders.createTestPojoContext(
        "<foo id=\"0\"/><x>hello</x><y><foo id=\"1\">hello</foo>yeah</y><z>sup</z><foo id=\"2\"/>",
        null, null, null, DocumentSchema.NO_SCHEMA_CONSTRAINTS).document();

    int i = 0;
    for (Element e : DocIterate.deepElementsWithTagName(doc, "foo")) {
      assertEquals(String.valueOf(i), e.getAttribute("id"));
      i++;
    }
    assertEquals(3, i);

    i = 0;
    for (Element e : DocIterate.deepElementsWithTagName(doc, "missing")) {
      i++;
    }
    assertEquals(0, i);
  }
}
