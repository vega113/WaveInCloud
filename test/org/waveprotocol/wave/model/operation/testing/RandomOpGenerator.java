package org.waveprotocol.wave.model.operation.testing;

import java.util.Random;

public interface RandomOpGenerator<D, O> {

  /**
   * Validity
   *
   * @param state
   * @param random
   * @return A random operation for the given state
   */
  O randomOperation(D state, Random random);
}
