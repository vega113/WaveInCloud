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

package org.waveprotocol.wave.examples.fedone.agents.agent;

import org.waveprotocol.wave.model.operation.core.CoreWaveletDocumentOperation;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.core.CoreWaveletData;

/**
 * Handles agent events.
 */
interface AgentEventListener {
  /**
   * Invoked when the wavelet document changes.
   *
   * @param wavelet
   * @param documentOperation
   */
  void onDocumentChanged(CoreWaveletData wavelet, CoreWaveletDocumentOperation documentOperation);

  /**
   * Invoked when a participant is added to the wavelet.
   *
   * @param wavelet
   * @param participant
   */
  void onParticipantAdded(CoreWaveletData wavelet, ParticipantId participant);

  /**
   * Invoked when a participant is removed from the wavelet.
   *
   * @param wavelet
   * @param participant
   */
  void onParticipantRemoved(CoreWaveletData wavelet, ParticipantId participant);

  /**
   * Invoked when this agent is added.
   *
   * @param wavelet
   */
  void onSelfAdded(CoreWaveletData wavelet);

  /**
   * Invoked when this agent is removed.
   *
   * @param wavelet
   */
  void onSelfRemoved(CoreWaveletData wavelet);
}
