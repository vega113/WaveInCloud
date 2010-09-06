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

import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.WaveletData;

/**
 * Handles agent events.
 */
interface AgentEventListener {
  /**
   * Invoked when the wavelet document changes.
   *
   * @param wavelet The wavelet that changes.
   * @param docId the id of the document in the wavelet that changed.
   * @param docOp the operation that caused the change.
   */
  void onDocumentChanged(WaveletData wavelet, String docId, BufferedDocOp docOp);

  /**
   * Invoked when a participant is added to the wavelet.
   *
   * @param wavelet the wavelet to which a participant is added.
   * @param participant the participant that has been added.
   */
  void onParticipantAdded(WaveletData wavelet, ParticipantId participant);

  /**
   * Invoked when a participant is removed from the wavelet.
   *
   * @param wavelet the wavelet for which a participant is removed.
   * @param participant the participant that has been removed.
   */
  void onParticipantRemoved(WaveletData wavelet, ParticipantId participant);

  /**
   * Invoked when this agent is added.
   *
   * @param wavelet the wavelet to which we have been added.
   */
  void onSelfAdded(WaveletData wavelet);

  /**
   * Invoked when this agent is removed.
   *
   * @param wavelet the wavelet from which we have been removed.
   */
  void onSelfRemoved(WaveletData wavelet);
}
