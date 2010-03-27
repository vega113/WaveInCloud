// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.util;

import java.util.Map;

/**
 * A document listener that is only interested in attribute changes.
 *
*
 * @param <E> document's element type
 */
public interface AttributeListener<E> {
  /**
   * Notifies this listener that attributes of an element within a document have
   * changed.
   *
   * @param element element that changed
   * @param oldValues attribute values that have been deleted
   * @param newValues attribute values that have been inserted
   */
  void onAttributesChanged(E element, Map<String, String> oldValues, Map<String, String> newValues);
}
