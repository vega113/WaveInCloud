/**
 * Copyright 2010 Google Inc.
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

package org.waveprotocol.wave.examples.fedone.robots.util;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ObservableConversationView;
import org.waveprotocol.wave.model.conversation.WaveBasedConversationView;
import org.waveprotocol.wave.model.conversation.WaveletBasedConversation;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.ReadOnlyWaveView;

/**
 * Utility class for {@link Conversation}s used by the Robot API.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class ConversationUtil {

  private final IdGenerator idGenerator;

  @Inject
  public ConversationUtil(IdGenerator idGenerator) {
    this.idGenerator = idGenerator;
  }

  /**
   * Returns an {@link ObservableConversationView} for the given wavelet.
   *
   * @param wavelet The wavelet to return the conversation for, must be a valid
   *        conversation wavelet.
   * @throws IllegalArgumentException if the wavelet is not a valid conversation
   *         wavelet.
   */
  public ObservableConversationView getConversation(ObservableWavelet wavelet) {
    Preconditions.checkArgument(IdUtil.isConversationalId(wavelet.getId()),
        "Expected conversational wavelet, got " + wavelet.getId());
    Preconditions.checkArgument(WaveletBasedConversation.waveletHasConversation(wavelet),
        "Conversation can't be build on a wavelet " + wavelet.getId()
            + " without conversation structure");

    ReadOnlyWaveView wv = new ReadOnlyWaveView(wavelet.getWaveId());
    wv.addWavelet(wavelet);

    return WaveBasedConversationView.create(wv, idGenerator);
  }
}
