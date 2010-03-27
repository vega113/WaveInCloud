// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document;


/**
 * Handler for mutable document events.
 *
*
 */
public interface MutableDocumentListener {
  MutableDocumentListener VOID = new MutableDocumentListener() {
    public void onDocumentEvents(MutableDocumentEvent events) {
      // Do nothing.
    }
  };

  /**
   * Triggered on changes to the document. The events should be in index order.
   *
   * @param events list of events
   */
  void onDocumentEvents(MutableDocumentEvent events);
}
