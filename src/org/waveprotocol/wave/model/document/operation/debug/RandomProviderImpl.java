/**
 * Copyright 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.waveprotocol.wave.model.document.operation.debug;

import org.waveprotocol.wave.model.document.operation.debug.RandomDocOpGenerator.RandomProvider;

import java.util.Random;

/**
 * Implementation of RandomProvider based on java.util.Random.
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
