// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.document.MutableDocumentEvent;
import org.waveprotocol.wave.model.document.MutableDocumentListener;
import org.waveprotocol.wave.model.document.MutableDocumentListenerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A mutable document listener factory which delegates to a set of other
 * factories. The factory creates listeners which forward messages to all
 * listeners provided by any of the delegate factories.
 *
 * Only used in wally.
 *
 * @author anorth@google.com (Alex North)
 */
public class DocumentListenerRegistry implements MutableDocumentListenerFactory {

  private final Set<MutableDocumentListenerFactory> factories;

  /**
   * Constructs a new registry an initial series of delegate factories.
   *
   * @param factories factories to delegate to
   */
  public DocumentListenerRegistry(MutableDocumentListenerFactory... factories) {
    this.factories = new HashSet<MutableDocumentListenerFactory>(Arrays.asList(factories));
  }

  /**
   * {@inheritDoc}.
   *
   * Creates a listener which forwards messages to all listeners for the
   * document provided by registered factories.
   *
   * @return a forwarding listener, or null if no factories provided listeners.
   */
  @Override
  public MutableDocumentListener createDocumentListener(String documentId) {
    final Set<MutableDocumentListener> listeners = new HashSet<MutableDocumentListener>();
    for (MutableDocumentListenerFactory factory : factories) {
      MutableDocumentListener interestedListener = factory.createDocumentListener(documentId);
      if (interestedListener != null) {
        listeners.add(interestedListener);
      }
    }
    if (!listeners.isEmpty()) {
      return forwardingListener(listeners);
    }
    return null;
  }

  /**
   * Adds a factory to the set of delegate factories.
   */
  public void addFactory(MutableDocumentListenerFactory factory) {
    factories.add(factory);
  }

  /**
   * Creates a listener which forwards messages to a set of delegate listeners.
   */
  private MutableDocumentListener forwardingListener(final Set<MutableDocumentListener> delegates) {
    return new MutableDocumentListener() {
      @Override
      public void onDocumentEvents(MutableDocumentEvent events) {
        for (MutableDocumentListener listener : delegates) {
          listener.onDocumentEvents(events);
        }
      }
    };
  }
}
