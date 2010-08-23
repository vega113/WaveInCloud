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
package org.waveprotocol.wave.examples.client.webclient.client.events;

import com.google.gwt.event.shared.GwtEvent;

import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.wave.Blip;

public class WaveletBlipEvent extends GwtEvent<WaveletBlipEventHandler> {
  public static final Type<WaveletBlipEventHandler> TYPE = new Type<WaveletBlipEventHandler>();

  private final Blip blip;
  private final WaveletId waveletId;

  public WaveletBlipEvent(WaveletId waveletId, Blip blip) {
    this.waveletId = waveletId;
    this.blip = blip;
  }

  @Override
  protected void dispatch(WaveletBlipEventHandler handler) {
    handler.onBlipAdded(waveletId, blip);
  }

  @Override
  public com.google.gwt.event.shared.GwtEvent.Type<WaveletBlipEventHandler> getAssociatedType() {
    return TYPE;
  }
}
