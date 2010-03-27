// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.util;


import java.util.List;

/**
 * A base class for in-memory element lists. Extending classes should at minimum provide
 * the implementation of the {@link #createInitialized(Object)} method, which is to
 * return a fully initialized element to be stored in the list.
 *
 * <T> The type of the element stored in the list.
 * <I> The type of the state used to initialize newly inserted elements.
 *
*
 */
public abstract class InMemoryElementList<T, I> implements ElementList<T, I> {
  private final List<T> delegate = CollectionUtils.newArrayList();

  /**
   * Create and return a new element to be inserted in the list. The element is to be
   * initialized with the provided {@code initialState}.
   *
   * @param initialState The state to initialize the new element with.
   * @return A new element, initialized with the provided {@code initialState}.
   */
  protected abstract T createInitialized(I initialState);

  @Override
  public T add(I initialState) {
    return add(delegate.size(), initialState);
  }

  @Override
  public T add(int index, I initialState) {
    T element = createInitialized(initialState);
    delegate.add(index, element);
    return element;
  }

  @Override
  public void clear() {
    delegate.clear();
  }

  @Override
  public T get(int index) {
    return delegate.get(index);
  }

  @Override
  public Iterable<T> getValues() {
    return delegate;
  }

  @Override
  public int indexOf(T element) {
    return delegate.indexOf(element);
  }

  @Override
  public boolean remove(T element) {
    return delegate.remove(element);
  }

  @Override
  public int size() {
    return delegate.size();
  }
}
