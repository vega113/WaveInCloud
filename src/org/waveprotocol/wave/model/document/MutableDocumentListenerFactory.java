// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document;

/**
 * Creates mutableDocumentListeners based on Document IDs.
 *
*
 */
public interface MutableDocumentListenerFactory {

  /**
   * Returns a MutableDocumentListener for the provided documentId.
   */
  MutableDocumentListener createDocumentListener(String documentId);
}
