// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.indexed;

import org.waveprotocol.wave.model.document.indexed.DocumentHandler.EventBundle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Holds a composite document event.
 */
public final class EventBundleImpl<N, E extends N, T extends N> implements EventBundle<N, E, T> {

  final static class Builder<N, E extends N, T extends N> {
    private final List<DocumentEvent<N, E, T>> components = new ArrayList<DocumentEvent<N, E, T>>();
    private Collection<E> inserted;
    private Collection<E> deleted;

    Builder() {
    }

    void addComponent(DocumentEvent<N, E, T> event) {
      components.add(event);
    }

    void addDeletedElement(E e) {
      if (deleted == null) {
        deleted = new LinkedHashSet<E>();
      }
      deleted.add(e);
    }

    void addInsertedElement(E e) {
      if (inserted == null) {
        inserted = new ArrayList<E>();
      }
      inserted.add(e);
    }

    EventBundleImpl<N, E, T> build() {
      if (inserted == null) {
        inserted = Collections.emptySet();
      }
      if (deleted == null) {
        deleted = Collections.emptySet();
      }
      return new EventBundleImpl<N, E, T>(components, deleted, inserted);
    }
  }

  private final List<DocumentEvent<N, E, T>> components;
  private final Collection<E> deleted;
  private final Collection<E> inserted;

  EventBundleImpl(List<DocumentEvent<N, E, T>> components, Collection<E> deleted,
      Collection<E> inserted) {
    this.components = components;
    this.deleted = deleted;
    this.inserted = inserted;
  }

  @Override
  public Iterable<DocumentEvent<N, E, T>> getEventComponents() {
    return components;
  }

  @Override
  public Collection<E> getDeletedElements() {
    return deleted;
  }

  @Override
  public Collection<E> getInsertedElements() {
    return inserted;
  }

  public boolean wasDeleted(E element) {
    return deleted.contains(element);
  }
}
