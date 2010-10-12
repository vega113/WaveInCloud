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

package org.waveprotocol.box.server.frontend.testing;

import com.google.inject.internal.Nullable;

import org.waveprotocol.box.server.common.VersionedWaveletDelta;
import org.waveprotocol.box.server.frontend.ClientFrontend;
import org.waveprotocol.box.server.waveserver.WaveBus;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.box.server.waveserver.WaveletProvider.SubmitRequestListener;
import org.waveprotocol.box.server.waveserver.WaveClientRpc;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.id.IdFilter;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
  * Implementation of a ClientFrontend which only records requests and will make callbacks when it
  * receives wavelet listener events.
  */
public class FakeClientFrontend implements ClientFrontend, WaveBus.Subscriber {
  private static class SubmitRecord {
    final SubmitRequestListener listener;
    final int operations;
    SubmitRecord(int operations, SubmitRequestListener listener) {
      this.operations = operations;
      this.listener = listener;
    }
  }

  private final Map<WaveId, OpenListener> openListeners = new HashMap<WaveId, OpenListener>();

  private final Map<WaveletName, SubmitRecord> submitRecords =
      new HashMap<WaveletName, SubmitRecord>();

  public void doSubmitFailed(WaveletName waveletName, String errorMessage) {
    SubmitRecord record = submitRecords.remove(waveletName);
    if (record != null) {
      record.listener.onFailure(errorMessage);
    }
  }

  /** Reports a submit success with resulting version 0 application timestamp 0 */
  public void doSubmitSuccess(WaveletName waveletName) {
    HashedVersion fakeHashedVersion = HashedVersion.of(0, new byte[0]);
    doSubmitSuccess(waveletName, fakeHashedVersion, 0);
  }

  /** Reports a submit success with the specified resulting version and application timestamp */
  public void doSubmitSuccess(WaveletName waveletName, HashedVersion resultingVersion,
      long applicationTimestamp) {
    SubmitRecord record = submitRecords.remove(waveletName);
    if (record != null) {
      record.listener.onSuccess(record.operations, resultingVersion, applicationTimestamp);
    }
  }

  public void doUpdateFailure(WaveId waveId, String errorMessage) {
    OpenListener listener = openListeners.get(waveId);
    if (listener != null) {
      listener.onFailure(errorMessage);
    }
  }

  @Override
  public void openRequest(ParticipantId participant, WaveId waveId, IdFilter waveletIdFilter,
      Collection<WaveClientRpc.WaveletVersion> knownWavelets, OpenListener openListener) {
    openListeners.put(waveId, openListener);
  }

  @Override
  public void submitRequest(WaveletName waveletName, ProtocolWaveletDelta delta,
      @Nullable String channelId, WaveletProvider.SubmitRequestListener listener) {
    submitRecords.put(waveletName, new SubmitRecord(delta.getOperationCount(), listener));
  }

  @Override
  public void waveletCommitted(WaveletName waveletName, HashedVersion version) {
    OpenListener listener = openListeners.get(waveletName.waveId);
    if (listener != null) {
      final List<VersionedWaveletDelta> emptyList = Collections.emptyList();
      listener.onUpdate(waveletName, null, emptyList, null, version, false, null);
    }
  }

  @Override
  public void waveletUpdate(ReadableWaveletData wavelet, HashedVersion resultingVersion,
      List<VersionedWaveletDelta> newDeltas) {
    OpenListener listener = openListeners.get(wavelet.getWaveId());
    if (listener != null) {
      WaveletName waveletName = WaveletName.of(wavelet.getWaveId(), wavelet.getWaveletId());
      listener.onUpdate(waveletName, null, newDeltas, resultingVersion, null, false, null);
    }
  }
}
