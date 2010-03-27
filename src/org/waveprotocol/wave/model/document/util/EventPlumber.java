// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.document.ObservableMutableDocument;
import org.waveprotocol.wave.model.document.indexed.DocumentEvent;
import org.waveprotocol.wave.model.document.indexed.DocumentEvent.AttributesModified;
import org.waveprotocol.wave.model.document.indexed.DocumentHandler;
import org.waveprotocol.wave.model.util.AttributeListener;
import org.waveprotocol.wave.model.util.DeletionListener;
import org.waveprotocol.wave.model.util.ElementListener;

/**
 * Event munging boilerplate
 *
*
 */
public final class EventPlumber {

  /**
   * Represents the binding of an event listener in a document.
   */
  public interface ListenerRegistration {
    /**
     * Detaches the bound listener from the document. This method is idempotent.
     */
    void detach();
  }

  /**
   * Base class for the different event adapters. This class controls the logic
   * of attaching/removing the document listener.
   */
  private static abstract class EventAdapter<N, E extends N, T extends N> // \u2620
      implements DocumentHandler<N, E, T>, ListenerRegistration {
    private final ObservableMutableDocument<N, E, T> doc;

    public EventAdapter(ObservableMutableDocument<N, E, T> doc) {
      this.doc = doc;
    }

    final ListenerRegistration attach() {
      doc.addListener(this);
      return this;
    }

    @Override
    public final void detach() {
      doc.removeListener(this);
    }
  }

  /**
   * Unfurls an {@link DocumentHandler.EventBundle} to an
   * {@link AttributeListener}. Note that attributes gained or lost due to
   * element insertions or deletions are not broadcast to the listener.
   */
  private static final class AttributeListenerAdapter<N, E extends N, T extends N> // \u2620
      extends EventAdapter<N, E, T> {
    private final AttributeListener<E> listener;

    private AttributeListenerAdapter(ObservableMutableDocument<N, E, T> doc,
        AttributeListener<E> listener) {
      super(doc);
      this.listener = listener;
    }

    public static <N, E extends N, T extends N> ListenerRegistration dispatch(
        ObservableMutableDocument<N, E, T> doc, final AttributeListener<E> listener) {
      return new AttributeListenerAdapter<N, E, T>(doc, listener).attach();
    }

    @Override
    public void onDocumentEvents(EventBundle<N, E, T> bundle) {
      for (DocumentEvent<N, E, T> event : bundle.getEventComponents()) {
        if (event.getType() == DocumentEvent.Type.ATTRIBUTES) {
          AttributesModified<N, E, T> am = (AttributesModified<N, E, T>) event;
          E target = am.getElement();
          listener.onAttributesChanged(target, am.getOldValues(), am.getNewValues());
        }
      }
    }
  }

  /**
   * Unfurls an {@link DocumentHandler.EventBundle} to an
   * {@link ElementListener}.
   */
  private static final class ElementListenerAdapter<N, E extends N, T extends N> // \u2620
      extends EventAdapter<N, E, T> {
    private final ElementListener<E> listener;

    private ElementListenerAdapter(ObservableMutableDocument<N, E, T> doc,
        ElementListener<E> listener) {
      super(doc);
      this.listener = listener;
    }

    public static <N, E extends N, T extends N> ListenerRegistration dispatch(
        ObservableMutableDocument<N, E, T> doc, ElementListener<E> listener) {
      return new ElementListenerAdapter<N, E, T>(doc, listener).attach();
    }

    @Override
    public void onDocumentEvents(EventBundle<N, E, T> bundle) {
      // NOTE(koz/hearnden): We call onElementRemoved() before
      // onElementAdded() on purpose so that listeners can refer to stored
      // references to elements in onElementAdded() without fear that they may
      // have been removed. See the note in {@link ElementListener} for more
      // details.
      for (E deleted : bundle.getDeletedElements()) {
        listener.onElementRemoved(deleted);
      }
      for (E inserted : bundle.getInsertedElements()) {
        listener.onElementAdded(inserted);
      }
    }
  }

  private static final class DeletionListenerAdapter<N, E extends N, T extends N> // \u2620
      extends EventAdapter<N, E, T> {
    private final DeletionListener listener;
    private final E target;

    private DeletionListenerAdapter(ObservableMutableDocument<N, E, T> doc, E target,
        DeletionListener listener) {
      super(doc);
      this.target = target;
      this.listener = listener;
    }

    public static <N, E extends N, T extends N> ListenerRegistration dispatch(
        ObservableMutableDocument<N, E, T> doc, E target, DeletionListener listener) {
      return new DeletionListenerAdapter<N, E, T>(doc, target, listener).attach();
    }

    @Override
    public void onDocumentEvents(EventBundle<N, E, T> event) {
      if (event.wasDeleted(target)) {
        detach();
        listener.onDeleted();
      }
    }
  }

  private EventPlumber() {
  }

  /**
   * Attaches an element listener to a document.
   */
  public static <N, E extends N> ListenerRegistration dispatchElementEvents(
      ObservableMutableDocument<N, E, ?> doc, ElementListener<E> listener) {
    return ElementListenerAdapter.dispatch(doc, listener);
  }

  /**
   * Attaches an attribute listener to a document.
   */
  public static <N, E extends N> ListenerRegistration dispatchAttributeEvents(
      ObservableMutableDocument<N, E, ?> doc, AttributeListener<E> listener) {
    return AttributeListenerAdapter.dispatch(doc, listener);
  }

  /**
   * Attaches a deletion listener to a document element. The document listener
   * is detached before the deletion listener is notified.
   */
  public static <N, E extends N> ListenerRegistration dispatchDeletionEvent(
      ObservableMutableDocument<N, E, ?> doc, E target, DeletionListener listener) {
    return DeletionListenerAdapter.dispatch(doc, target, listener);
  }
}
