// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.operation.debug;

import org.waveprotocol.wave.model.document.operation.debug.RandomDocOpGenerator.RandomProvider;

import java.util.Random;

/**
 * Implementation of RandomProvider based on java.util.Random.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class RandomProviderImpl implements RandomProvider {

  public static RandomProviderImpl ofSeed(int seed) {
    return new RandomProviderImpl(seed);
  }

  private final Random random;

  public RandomProviderImpl(Random random) {
    this.random = random;
  }

  public RandomProviderImpl(int seed) {
    this.random = new Random(seed);
  }

  @Override
  public boolean nextBoolean() {
    return random.nextBoolean();
  }

  @Override
  public int nextInt(int upperBound) {
    return random.nextInt(upperBound);
  }
}
