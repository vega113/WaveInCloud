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
package org.waveprotocol.box.client.webclient.client.events;

import com.google.gwt.event.shared.GwtEvent;

import org.waveprotocol.box.client.common.IndexEntry;

import java.util.List;

public class WaveIndexUpdatedEvent extends
    GwtEvent<WaveIndexUpdatedEventHandler> {
  public static final Type<WaveIndexUpdatedEventHandler> TYPE = new Type<WaveIndexUpdatedEventHandler>();

  private final List<IndexEntry> entries;

  public WaveIndexUpdatedEvent(List<IndexEntry> entries) {
    this.entries = entries;
  }

  @Override
  protected void dispatch(WaveIndexUpdatedEventHandler handler) {
    handler.onWaveIndexUpdate(this);
  }

  public List<IndexEntry> getEntries() {
    return entries;
  }

  @Override
  public Type<WaveIndexUpdatedEventHandler> getAssociatedType() {
    return TYPE;
  }

}
