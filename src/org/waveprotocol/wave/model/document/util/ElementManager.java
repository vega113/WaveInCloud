// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.util;

/**
 * Useful extra functionality for dealing with elements, that do not require a
 * document context (so this interface may be implemented by singletons, etc).
 *
 * NOTE(danilatos): This is a temporary interface and is likely to be merged
 * into others soon
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface ElementManager<E> {

  /**
   * Get a transient property for a given element
   *
   * @see Property
   */
  <T> T getProperty(Property<T> property, E element);

  /**
   * Set a transient property on a given element
   *
   * @see Property
   */
  <T> void setProperty(Property<T> property, E element, T value);

  /**
   * Find out if an element is still usable or not. Useful for assisting with
   * other cleanup work.
   *
   * @param element An element reference, possibly no longer valid
   * @return true if the element is invalid, for example if it has been removed
   *         from the document
   */
  boolean isDestroyed(E element);
}
