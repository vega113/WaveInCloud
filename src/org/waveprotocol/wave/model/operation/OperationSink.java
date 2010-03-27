// Copyright 2008 Google Inc. All Rights Reserved

package org.waveprotocol.wave.model.operation;


/**
 * An operation sink is an opaque operation applier.  It has a single method through which
 * operations are passed, and must obey the contract that the intent of that operation gets
 * executed.  Note this is a weaker contract than actually executing the {@link
 * Operation#apply(Object)} method on that operation (for example, the operation may be transformed
 * into another operation instance, so that the new operation's {@code apply} method would be called
 * instead).
 *
 * A sink also abstracts away the locating of the appropriate object to which the operation is
 * applied.   Usually, a sink will either pass the operation to another sink, or be a manager for
 * instances of the target type to which the operation applies, and will identify the appropriate
 * instance and then execute the operation's {@code apply} method on that instance.
 *
*
 *
 * @param <T>
 */
public interface OperationSink<T extends Operation<?>> {
  /**
   * An operation sink which does nothing with consumed operations.
   */
  static final OperationSink<Operation<?>> VOID =
    new OperationSink<Operation<?>>() {
      @Override
      public void consume(Operation<?> op) {
      }
    };

  /** Builds operation sinks which simply apply sunk operations to a target. */
  final class Executor {
    /**
     * Creates a new operation sink which applies all received ops to a target.
     *
     * @param target target to which to apply ops.
     * @param <O> type of operations sunk
     * @param <T> type of the operation target
     */
    public static <O extends Operation<? super T>, T> OperationSink<O> build(final T target) {
      return new OperationSink<O>() {
        public void consume(O op) throws OperationException {
          op.apply(target);
        }
      };
    }
  }

  /**
   * Consumes an operation.  Usually, this will involve finding an appropriate target for the
   * operation, then calling {@link Operation#apply(Object)} on that target.  However, this is not
   * a strong guarantee.  The only contract for a sink is to ensure that the intent of the given
   * operation is effected.
   *
   * @param op  operation to apply
   */
  public void consume(T op) throws OperationException;
}
