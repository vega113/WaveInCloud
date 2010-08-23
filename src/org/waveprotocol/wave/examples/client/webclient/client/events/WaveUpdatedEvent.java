/**
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
 * 
 */

package org.waveprotocol.wave.examples.client.webclient.client.events;

import com.google.gwt.event.shared.GwtEvent;

import org.waveprotocol.wave.examples.client.webclient.waveclient.common.WaveViewServiceImpl;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;

public class WaveUpdatedEvent extends GwtEvent<WaveUpdatedEventHandler> {
  public static final Type<WaveUpdatedEventHandler> TYPE = new Type<WaveUpdatedEventHandler>();
  private final String channelId;
  private final WaveViewServiceImpl waveView;
  private final WaveletId wavelet;

  public WaveUpdatedEvent(WaveViewServiceImpl waveView) {
    this(waveView, null, null);
  }

  public WaveUpdatedEvent(WaveViewServiceImpl waveView, String channelId,
      WaveletId wavelet) {
    this.waveView = waveView;
    this.channelId = channelId;
    this.wavelet = wavelet;
  }

  @Override
  public Type<WaveUpdatedEventHandler> getAssociatedType() {
    return TYPE;
  }

  public String getChannelId() {
    return channelId;
  }

  public WaveId getId() {
    return waveView.getWaveId();
  }

  public WaveletId getWavelet() {
    return wavelet;
  }

  public WaveViewServiceImpl getWaveViewService() {
    return waveView;
  }

  @Override
  protected void dispatch(WaveUpdatedEventHandler handler) {
    handler.onWaveUpdate(this);
  }
}
