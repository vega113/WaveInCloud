// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.util;

import org.waveprotocol.wave.model.document.MutableDocument;

/**
 * Bridge class to take care of some of the more annoying type conversion
 * related to documents.
 *
 * NOTE(user): This two-step unravelling of re-quantifying the
 * type-parameters into type variables is only required in order to coax
 * javac's dumb generic-inference to realise that the call is type-safe.
 * Eclipse's compiler is smarter, and does not require this extra work.
 *
*
 */
public final class DocumentUnwrapper {

  /** Helper interface that encapsulates an action */
  public interface Action<R> {

    /** Method to make use of the revised type */
    <N, E extends N, T extends N> R handle(MutableDocument<N, E, T> doc);
  }

  /**
   * Invoke the supplied action on an appropriately typed document
   *
   * @param action The action object to invoke
   * @param doc The document to apply the action to
   * @return The result of performing the action.
   */
  public static <R, N> R invoke(Action<R> action, MutableDocument<N, ?, ?> doc) {
    return action.handle(doc);
  }

  /** private constructor */
  private DocumentUnwrapper() {
  }
}
