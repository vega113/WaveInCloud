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

package org.waveprotocol.box.server.persistence.memory;

import com.google.gxp.com.google.common.base.Preconditions;

import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.box.server.waveserver.ByteStringMessage;
import org.waveprotocol.box.server.waveserver.DeltaStore.DeltasAccess;
import org.waveprotocol.box.server.waveserver.WaveletDeltaRecord;
import org.waveprotocol.wave.federation.Proto.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.core.TransformedWaveletDelta;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.version.HashedVersion;

import java.util.ArrayList;
import java.util.Collection;

/**
 * An in-memory implementation of DeltasAccess
 *
 * @author josephg@google.com (Joseph Gentle)
 */
public class MemoryDeltaCollection implements DeltasAccess {
  final ArrayList<WaveletDeltaRecord> records = CollectionUtils.newArrayList();
  final WaveletName waveletName;

  public MemoryDeltaCollection(WaveletName waveletName) {
    Preconditions.checkNotNull(waveletName);
    this.waveletName = waveletName;
  }

  private boolean hasVersion(long version) {
    return version >= 0 && version < records.size();
  }

  /** @return true if the collection is empty */
  public boolean isEmpty() {
    return records.isEmpty();
  }

  @Override
  public WaveletName getWaveletName() {
    return waveletName;
  }

  @Override
  public HashedVersion getEndVersion() {
    return getResultingVersion(records.size() - 1);
  }

  @Override
  public WaveletDeltaRecord getDelta(long version) {
    if (hasVersion(version)) {
      return records.get((int) version);
    } else {
      return null;
    }
  }

  @Override
  public HashedVersion getAppliedAtVersion(long version) {
    if (hasVersion(version)) {
      ByteStringMessage<ProtocolAppliedWaveletDelta> delta =
        records.get((int) version).applied;
      ProtocolHashedVersion protoVersion =
        delta.getMessage().getHashedVersionAppliedAt();
      return CoreWaveletOperationSerializer.deserialize(protoVersion);
    } else {
      return null;
    }
  }

  @Override
  public HashedVersion getResultingVersion(long version) {
    if (hasVersion(version)) {
      return records.get((int) version).transformed.getResultingVersion();
    } else {
      return null;
    }
  }

  @Override
  public ByteStringMessage<ProtocolAppliedWaveletDelta> getAppliedDelta(long version) {
    WaveletDeltaRecord delta = getDelta(version);
    return (delta != null) ? delta.applied : null;
  }

  @Override
  public TransformedWaveletDelta getTransformedDelta(long version) {
    WaveletDeltaRecord delta = getDelta(version);
    return (delta != null) ? delta.transformed : null;
  }

  @Override
  public void close() {
    // Does nothing.
  }

  @Override
  public void append(
      Collection<org.waveprotocol.box.server.waveserver.WaveletDeltaRecord> deltas) {
    records.addAll(deltas);
  }
}