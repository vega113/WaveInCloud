// Copyright 2009 Google Inc. All Rights Reserved.
package org.waveprotocol.wave.model.document.util;

import org.waveprotocol.wave.model.document.AnnotationBehaviour;
import org.waveprotocol.wave.model.document.AnnotationMutationHandler;

import java.util.Iterator;

/**
 * Tentative registry for annotation change handlers and behaviour specifications.
 *
 * This interface will most likely be merged with something similar for documents,
 * and hopefully generalised.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface AnnotationRegistry {
  // TODO(patcoleman): come up with a good way of merging the properties registered here.
  // (currenty handler and behaviour, though there may be more later).

  /**
   * Register a handler to be notified on all changes to annotations matching the given prefix.
   *
   * @param prefix
   * @param handler
   */
  void registerHandler(String prefix, AnnotationMutationHandler handler);

  /**
   * Get handlers for given key, in order from most general prefix to most specific prefix.
   *
   * @param key
   * @return iterator over the handlers for a given key
   */
  Iterator<AnnotationMutationHandler> getHandlers(String key);

  /**
   * Register a behaviour definition for all annotations matching the given prefix.
   *
   * @param prefix
   * @param behaviour
   */
  void registerBehaviour(String prefix, AnnotationBehaviour behaviour);

  /**
   * Get all behaviours for the given key, in order from most general prefix to most
   * specific prefix.
   *
   * @param key
   * @return iterator over the behaviours for a given key
   */
  Iterator<AnnotationBehaviour> getBehaviours(String key);

  /**
   * Helper methods for implementations.
   */
  public static final class Util {

    /**
     * Validates a prefix. Validation logic forms part of the definition of this
     * interface. Does nothing if the prefix is valid.
     *
     * @param prefix prefix to validate
     * @throws IllegalArgumentException if the prefix is invalid
     */
    public static void validatePrefix(String prefix) {
      if (prefix == null) {
        throw new IllegalArgumentException("Prefix cannot be null");
      }

      if ("".equals(prefix)) {
        throw new IllegalArgumentException("Cannot register handler on root prefix");
      }

      if (prefix.startsWith("/") || prefix.startsWith("" + Annotations.TRANSIENT)) {
        throw new IllegalArgumentException("Illegal initial character: " + prefix.charAt(0));
      }

      if (prefix.endsWith("/")) {
        throw new IllegalArgumentException("Illegal final character: " + prefix.charAt(0));
      }
    }

    private Util() {}
  }

  /**
   * Creates an extension of this registry
   */
  AnnotationRegistry createExtension();
}
