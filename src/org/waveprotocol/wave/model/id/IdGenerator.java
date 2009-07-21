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

package org.waveprotocol.wave.model.id;

/**
 * This class is used to generate Wave and Wavelet ids.
 *
 * The id field is structured as a sequence of '+'-separated tokens.
 * The id field is case sensitive.
 *
 * A wavelet is hosted by a single wave service provider, which may differ from
 * the service provider which allocated the wave id. Common examples are private
 * replies and user-data wavelets for users from a federated domain. Thus, the
 * service provider specified in a wavelet id may differ from the service
 * provider of the wave to which it belongs.
 *
 *
 */
public interface IdGenerator {

  /**
   * Creates a new unique wave id.
   *
   * Conversational waves (all the waves we have today) are specified by a leading
   * token 'w' followed by a pseudo-random string, e.g. w+3dKS9cD.
   */
  WaveId newWaveId();

  /**
   * Creates a new unique wavelet id.
   *
   * Conversational wavelets (those expected to render in a wave client) are
   * specified by a leading token "conv" followed by a psuedorandom string,
   * e.g. conv+3sG7. The distinguished root conversation wavelet is
   * identified by conv+root.
   */
  WaveletId newConversationWaveletId();

  /**
   * Create a new conversation root wavelet id.
   *
   * The conversation root wavelet has id "conv+root".
   */
  WaveletId newConversationRootWaveletId();
}
