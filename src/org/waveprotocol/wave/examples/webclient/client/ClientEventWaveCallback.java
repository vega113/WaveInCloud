/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.waveprotocol.wave.examples.webclient.client;

import org.waveprotocol.wave.concurrencycontrol.channel.WaveViewService.WaveViewServiceUpdate;
import org.waveprotocol.wave.concurrencycontrol.common.ChannelException;
import org.waveprotocol.wave.examples.webclient.client.events.WaveUpdatedEvent;
import org.waveprotocol.wave.examples.webclient.common.communication.callback.ExceptionHandlingStreamingRemoteCallback;
import org.waveprotocol.wave.examples.webclient.common.communication.interchange.StatusCode;
import org.waveprotocol.wave.examples.webclient.util.Log;
import org.waveprotocol.wave.examples.webclient.waveclient.common.WaveViewServiceImpl;

/**
 * Fires WaveUpdateEvents in response to a wave .
 */
public class ClientEventWaveCallback
    implements
    ExceptionHandlingStreamingRemoteCallback<String, WaveViewServiceUpdate, ChannelException> {

  private static final Log LOG = Log.get(ClientEventWaveCallback.class);

  private final WaveViewServiceImpl waveView;

  public ClientEventWaveCallback(WaveViewServiceImpl waveView) {
    this.waveView = waveView;
  }

  @Override
  public void onException(ChannelException e) {
    LOG.severe("Wave exception: " + waveView.getWaveId().getId(), e);
  }

  @Override
  public void onFailure(StatusCode reason) {
    LOG.info("Operation failure: " + waveView.getWaveId().getId() + " "
        + reason.getDescription());
  }

  @Override
  public void onSuccess(String response) {
    LOG.info("Subscribed to wave: " + waveView.getWaveId().getId());
  }

  @Override
  public void onUpdate(WaveViewServiceUpdate update) {
    LOG.info("Received update for wave: " + waveView.getWaveId().getId());
    ClientEvents.get().fireEvent(
        new WaveUpdatedEvent(waveView, update.getChannelId(),
            update.getWaveletId()));
  }
}
