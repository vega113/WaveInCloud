// Copyright 2010 Google Inc. All Rights Reserved.
package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.document.ReadableDocument;
import org.waveprotocol.wave.model.document.operation.DocInitializationCursor;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;

/**
 * Utilities for converting between DOM trees and operations.
 *
 * @author patcoleman@google.com (Pat Coleman)
 */
public final class DomOperationUtil {
  /** Static methods, no constructor. */
  private DomOperationUtil() {}

  /**
   * Writes an entire subtree worth of operations to an initialization cursor.
   * NOTE(patcoleman): does not include annotations.
   *
   * @param doc Document the node resides within.
   * @param node Root of the subtree.
   * @param cursor Cursor to write results out to.
   */
  public static <N, E extends N, T extends N> void buildDomInitializationFromSubtree(
      ReadableDocument<N, E, T> doc, N node, DocInitializationCursor cursor) {
    T text = doc.asText(node);
    if (text == null) {
      buildDomInitializationFromElement(doc, doc.asElement(node), cursor, true);
    } else {
      buildDomInitializationFromTextNode(doc, text, cursor);
    }
  }

  /**
   * Writes a text node's information out to an initialization cursor.
   * NOTE(patcoleman): does not include annotations.
   *
   * @param doc Document the node resides within.
   * @param textNode Text node containing information to be written
   * @param cursor Cursor to write results out to.
   */
  public static <N, E extends N, T extends N> void buildDomInitializationFromTextNode(
      ReadableDocument<N, E, T> doc, T textNode, DocInitializationCursor cursor) {
    cursor.characters(doc.getData(textNode));
  }


  /**
   * Writes an element's information out to an initialization cursor, optionally recursing
   * to do likewise for its children.
   *
   * @param doc Document the node resides within.
   * @param element Element containing information to be written.
   * @param cursor Cursor to write results out to.
   * @param recurse Whether or not to write children to the operation.
   */
  public static <N, E extends N, T extends N> void buildDomInitializationFromElement(
      ReadableDocument<N, E, T> doc, E element, DocInitializationCursor cursor, boolean recurse) {
    cursor.elementStart(doc.getTagName(element), new AttributesImpl(doc.getAttributes(element)));
    if (recurse) {
      for (N child = doc.getFirstChild(element); child != null; child = doc.getNextSibling(child)) {
        buildDomInitializationFromSubtree(doc, child, cursor);
      }
    }
    cursor.elementEnd();
  }
}
