// Copyright 2008 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document;

import org.waveprotocol.wave.model.document.indexed.DocumentHandler;
import org.waveprotocol.wave.model.wave.SourcesEvents;

/**
 * A mutable document to which event handlers can be attached.
 *
*
 */
public interface ObservableMutableDocument<N, E extends N, T extends N> extends
    MutableDocument<N, E, T>, SourcesEvents<DocumentHandler<N, E, T>> {

  public interface Action {
    /**
     * Runs the action with the given document.
     *
     * The document may only be assumed to be valid only during the method's
     * execution.
     */
    <N, E extends N, T extends N> void exec(ObservableMutableDocument<N, E, T> doc);
  }

  public interface Method<V> {
    /**
     * Runs the method with the given document.
     *
     * The document may only be assumed to be valid only during the method's
     * execution.
     */
    <N, E extends N, T extends N> V exec(ObservableMutableDocument<N, E, T> doc);
  }

  // Overload with() to perform actions on observable documents.

  public void with(Action actionToRunWithObservableMutableDocument);
  public <V> V with(Method<V> methodToRunWithMutableDocument);
}
