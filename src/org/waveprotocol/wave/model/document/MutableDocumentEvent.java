// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document;

/**
 * Data object that wraps a mutableDocument and its events in order to allow
 * users to match the same generic types across both objects.
 *
*
 */
public class MutableDocumentEvent {

  private final Document document;
  private final DocHandler.EventBundle<Doc.N, Doc.E, Doc.T> events;

  public MutableDocumentEvent(Document document,
      DocHandler.EventBundle<Doc.N, Doc.E, Doc.T> events) {
    this.document = document;
    this.events = events;
  }

  public Document getDocument() {
    return document;
  }

  public DocHandler.EventBundle<Doc.N, Doc.E, Doc.T> getEvents() {
    return events;
  }
}
