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
package org.waveprotocol.wave.examples.webclient.waveclient.common;

import org.waveprotocol.wave.concurrencycontrol.channel.WaveViewService;
import org.waveprotocol.wave.concurrencycontrol.common.Delta;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.version.DistinctVersion;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;

import java.util.List;

/**
 * A simple implementation of WaveViewServiceUpdate.
 *  TODO(arb): enforce constraint that only one of deltaList, snapshot, channelId is set.
 */
class WebClientWaveViewUpdate implements WaveViewService.WaveViewServiceUpdate {

  String channelId = null;
  WaveletId waveletId = null;
  DistinctVersion lastCommittedVersion = null;
  DistinctVersion currentVersion = null;
  ObservableWaveletData waveletSnapshot = null;
  List<Delta> deltaList = null;
  public boolean marker = false;

  public WebClientWaveViewUpdate() {
  }

  @Override
  public boolean hasChannelId() {
    return channelId != null;
  }

  @Override
  public String getChannelId() {
    return channelId;
  }

  public WebClientWaveViewUpdate setChannelId(final String channelId) {
    this.channelId = channelId;
    return this;
  }

  @Override
  public boolean hasWaveletId() {
    return waveletId != null;
  }

  @Override
  public WaveletId getWaveletId() {
    return waveletId;
  }

  public WebClientWaveViewUpdate setWaveletId(final WaveletId waveletId) {
    this.waveletId = waveletId;
    return this;
  }

  @Override
  public boolean hasLastCommittedVersion() {
    return lastCommittedVersion != null;
  }

  @Override
  public DistinctVersion getLastCommittedVersion() {
    return lastCommittedVersion;
  }

  public WebClientWaveViewUpdate setLastCommittedVersion(
      final DistinctVersion lastCommittedVersion) {
    this.lastCommittedVersion = lastCommittedVersion;
    return this;
  }

  @Override
  public boolean hasCurrentVersion() {
    return currentVersion != null;
  }

  @Override
  public DistinctVersion getCurrentVersion() {
    return currentVersion;
  }

  public WebClientWaveViewUpdate setCurrentVersion(final DistinctVersion currentVersion) {
    this.currentVersion = currentVersion;
    return this;
  }

  @Override
  public boolean hasWaveletSnapshot() {
    return waveletSnapshot != null;
  }

  @Override
  public ObservableWaveletData getWaveletSnapshot() {
    return waveletSnapshot;
  }

  public WebClientWaveViewUpdate setWaveletSnapshot(final ObservableWaveletData waveletSnapshot) {
    this.waveletSnapshot = waveletSnapshot;
    return this;
  }

  @Override
  public boolean hasDeltas() {
    return deltaList != null;
  }

  @Override
  public List<Delta> getDeltaList() {
    return deltaList;
  }

  public WebClientWaveViewUpdate setDeltaList(final List<Delta> deltaList) {
    this.deltaList = deltaList;
    return this;
  }

  @Override
  public boolean hasMarker() {
    return marker;
  }

  public WebClientWaveViewUpdate setMarker(boolean marker) {
    this.marker = marker;
    return this;
  }
}
