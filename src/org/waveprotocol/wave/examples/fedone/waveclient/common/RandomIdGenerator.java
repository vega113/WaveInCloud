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
 *
 */

package org.waveprotocol.wave.examples.fedone.waveclient.common;

import org.waveprotocol.wave.model.id.IdConstants;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.SimplePrefixEscaper;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.examples.fedone.util.RandomBase64Generator;

/**
 * This class is used to generate Wave and Wavelet ids.
 *
 *
 */
public final class RandomIdGenerator implements IdGenerator, IdConstants {

  private final RandomBase64Generator base64Generator = new RandomBase64Generator();
  private final String domain;

  /**
   *
   * @param domain the wave  provider domain name (e.g. acmewave.com) to
   *        associate with generated ids
   */
  public RandomIdGenerator(String domain) {
    this.domain = domain;
  }

  @Override
  public WaveId newWaveId() {
    // 72 random bits allow billions of waves per domain without probable id collisions
    return new WaveId(domain, newId(WAVE_PREFIX, 72));
  }

  @Override
  public WaveletId newConversationWaveletId() {
    // 48 random bits allow millions of wavelets per domain per wave without probable id collisions
    return new WaveletId(domain, newId(CONVERSATION_WAVELET_PREFIX, 48));
  }

  @Override
  public WaveletId newConversationRootWaveletId() {
    return new WaveletId(domain, CONVERSATION_ROOT_WAVELET);
  }

  /**
   * Creates a unique string id with an initial namespacing token. Generated ids
   * take the form "[namespace]+[random-token]".
   *
   * @param namespace namespacing token
   * @param randomBits the amount of randomness in the random token
   */
  private String newId(String namespace, int randomBits) {
    return build(namespace, newUniqueToken(randomBits));
  }

  /**
   * @param bits minimum quantity of randomness in the token
   * @return random token
   */
  private String newUniqueToken(int bits) {
    int characters = (bits + 5) / 6;  // 6 bits per character, rounded up
    return base64Generator.next(characters);
  }

  /**
   * @return a string from a list of tokens by using
   *         {@link IdConstants#TOKEN_SEPARATOR} as delimiters.
   */
  private static String build(String... tokens) {
    return SimplePrefixEscaper.DEFAULT_ESCAPER.join(TOKEN_SEPARATOR, tokens);
  }
}
