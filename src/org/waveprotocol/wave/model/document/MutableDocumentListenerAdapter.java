// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document;

/**
 * Injects a document listener into an observable document, dispatching
 * events to a (document-free) document listener.
 *
*
 */
public final class MutableDocumentListenerAdapter implements DocHandler {
  private final MutableDocumentListener listener;
  private final ObservableDocument doc;

  public MutableDocumentListenerAdapter(MutableDocumentListener listener,
      ObservableDocument doc) {
    this.doc = doc;
    this.listener = listener;
  }

  public static <N> void observe(MutableDocumentListener listener,
      ObservableDocument doc) {
    doc.addListener(new MutableDocumentListenerAdapter(listener, doc));
  }

  @Override
  public void onDocumentEvents(DocHandler.EventBundle<Doc.N, Doc.E, Doc.T> event) {
    listener.onDocumentEvents(new MutableDocumentEvent(doc, event));
  }
}
