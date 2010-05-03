package org.waveprotocol.wave.model.document.operation;

import org.waveprotocol.wave.model.operation.SilentOperationSink;


/**
 * A document that can reverse whatever modifications are performed on it.
 *
*
 */
public interface DocumentOperationReverser extends ModifiableDocument {

  /**
   * Registers a sink into which reverse operations should be sunk.
   *
   * @param reverseSink The sink into which reverse operations should be sunk.
   */
  void registerReverseSink(SilentOperationSink<BufferedDocOp> reverseSink);

  /**
   * Unregisters the sink.
   */
  void unregisterReverseSink();

}
