// Copyright 2008 Google Inc. All Rights Reserved

package org.waveprotocol.wave.model.operation;


/**
 * An extension of the basic sink that takes hints about grouping operations together (perhaps
 * for batching, merging, etc).  If passing a group of operations to a sequencer, call
 * {@link #begin()} before the first operation, and {@link #end()} after the last operation.
 * Groups can be nested.
 *
 * @param <T> operation type
 * @author danilatos@google.com
 */
public interface OperationSequencer<T> {

  /**
   * Begin a set of executed operations.
   * Set up any state or other tasks as necessary.
   */
  void begin();

  /**
   * End a set of executed operations.
   * Perform any notifications or other tasks as necessary.
   */
  void end();

  /**
   * Consumes an operation
   * @param op
   */
  void consume(T op);
}
