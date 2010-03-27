package org.waveprotocol.wave.model.operation;

public interface DataFactory<D> {

  /**
   * @return empty piece of data.
   */
  D initialState();
}
