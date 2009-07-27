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

import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.WaveletData;

/**
 * Notification interface for wavelet operations, with an additional two methods defined with
 * efficient rendering in mind.
 *
 *
 */
public interface WaveletOperationListener {
  /**
   * Invoked when an operation is applied to a document of a wavelet.
   *
   * @param wavelet the wavelet operated on
   * @param documentId the document id of the document
   */
  public void waveletDocumentUpdated(WaveletData wavelet, String documentId);

  /**
   * Invoked when a participant has been added to a wavelet.
   *
   * @param wavelet the wavelet operated on
   * @param participantId the id of the participant added
   */
  public void participantAdded(WaveletData wavelet, ParticipantId participantId);

  /**
   * Invoked when a participant has been removed from a wavelet.
   *
   * @param wavelet the wavelet operated on
   * @param participantId the id of the participant removed
   */
  public void participantRemoved(WaveletData wavelet, ParticipantId participantId);

  /**
   * Invoked on a wavelet NoOp.
   *
   * @param wavelet the wavelet (not) operated on
   */
  public void noOp(WaveletData wavelet);

  /**
   * Invoked before a sequence of deltas is applied.
   */
  public void onDeltaSequenceStart(WaveletData wavelet);

  /**
   * Invoked after a sequence of deltas has been applied.  It will probably be most appropriate to
   * do any rendering for wavelet changes in this method.
   */
  public void onDeltaSequenceEnd(WaveletData wavelet);
}
