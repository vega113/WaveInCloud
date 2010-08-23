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
 */

package org.waveprotocol.wave.examples.fedone.frontend;

import static org.waveprotocol.wave.examples.fedone.common.CommonConstants.INDEX_WAVE_ID;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.waveprotocol.wave.examples.fedone.common.CoreWaveletOperationSerializer;
import org.waveprotocol.wave.examples.fedone.common.DeltaSequence;
import org.waveprotocol.wave.examples.fedone.common.HashedVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.core.CoreAddParticipant;
import org.waveprotocol.wave.model.operation.core.CoreRemoveParticipant;
import org.waveprotocol.wave.model.operation.core.CoreWaveletDelta;
import org.waveprotocol.wave.model.operation.core.CoreWaveletDocumentOperation;
import org.waveprotocol.wave.model.operation.core.CoreWaveletOperation;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.List;

/**
 * Utilities for mutating the index wave, a list of waves visible to a user. The
 * index wave has a wavelet for each wave in the index. That wavelet has a
 * digest document containing a snippet of text.
 *
 * TODO(anorth): replace this with a more canonical use of the wave model.
 *
 * @author anorth@google.com (Alex North)
 */
final class IndexWave {

  @VisibleForTesting
  static final ParticipantId DIGEST_AUTHOR = new ParticipantId("digest-author");
  @VisibleForTesting
  static final String DIGEST_DOCUMENT_ID = "digest";

  /**
   * Constructs the name of the index wave wavelet that refers to the specified
   * wave.
   *
   * @param waveId referent wave id
   * @return WaveletName of the index wave wavelet referring to waveId
   * @throws IllegalArgumentException if waveId is the WaveId of the index wave
   */
  static WaveletName indexWaveletNameFor(WaveId waveId) {
    Preconditions.checkArgument(!waveId.equals(INDEX_WAVE_ID),
        "There is no index wave wavelet for the index wave itself: %s", waveId);
    return WaveletName.of(INDEX_WAVE_ID, WaveletId.deserialise(waveId.serialise()));
  }

  /**
   * Extracts the wave id referred to by an index wavelet name.
   */
  static WaveId indexWaveletWaveId(WaveletName indexWaveletName) {
    Preconditions.checkArgument(indexWaveletName.waveId.equals(INDEX_WAVE_ID), "Expected wave id "
        + INDEX_WAVE_ID + ", got " + indexWaveletName.waveId);
    return WaveId.deserialise(indexWaveletName.waveletId.serialise());
  }

  /**
   * Constructs the deltas that should be applied to and index wave wavelet when
   * the corresponding original wavelet receives deltas digest changes.
   *
   * The returned deltas will have the same effect on the participants as the
   * original deltas. The effect of the returned deltas on the document's digest
   * are purely a function of oldDigest and newDigest, which should represent
   * the change implied by deltas.
   *
   * @param targetVersion version the deltas should start at
   * @param deltas The deltas whose effect on the participants to determine
   * @return deltas to apply to the index wavelet to achieve the same change in
   *         participants, and the specified change in digest text
   */
  static DeltaSequence createIndexDeltas(long targetVersion,
      DeltaSequence deltas, String oldDigest, String newDigest) {
    ProtocolWaveletDelta digestDelta =
      createDigestDelta(targetVersion, oldDigest, newDigest);
    if (digestDelta != null) {
      targetVersion += digestDelta.getOperationCount();
    }
    DeltaSequence participantDeltas = createParticipantDeltas(targetVersion, deltas);
    if (digestDelta == null) {
      return participantDeltas;
    } else {
      return participantDeltas.prepend(ImmutableList.of(digestDelta));
    }
  }

  /**
   * Constructs a delta with one op which transforms the digest document from one digest
   * string to another.
   *
   * @return a delta, or null if no op is required
   */
  private static ProtocolWaveletDelta createDigestDelta(long targetVersion, String oldDigest,
      String newDigest) {
    if (oldDigest.equals(newDigest)) {
      return null;
    } else {
      CoreWaveletOperation op = new CoreWaveletDocumentOperation(DIGEST_DOCUMENT_ID,
          createEditOp(oldDigest, newDigest));
      CoreWaveletDelta indexDelta = new CoreWaveletDelta(DIGEST_AUTHOR, ImmutableList.of(op));
      return CoreWaveletOperationSerializer.serialize(indexDelta,
          HashedVersion.unsigned(targetVersion), HashedVersion.unsigned(targetVersion + 1));
    }
  }

  /** Constructs a BufferedDocOp that transforms source into target. */
  private static BufferedDocOp createEditOp(String source, String target) {
    int commonPrefixLength = lengthOfCommonPrefix(source, target);
    DocOpBuilder builder = new DocOpBuilder();
    if (commonPrefixLength > 0) {
      builder.retain(commonPrefixLength);
    }
    if (source.length() > commonPrefixLength) {
      builder.deleteCharacters(source.substring(commonPrefixLength));
    }
    if (target.length() > commonPrefixLength) {
      builder.characters(target.substring(commonPrefixLength));
    }
    return builder.build();
  }

  /** Extracts participant change operations from a delta sequence. */
  private static DeltaSequence createParticipantDeltas(long version,
      Iterable<ProtocolWaveletDelta> deltas) {
    List<ProtocolWaveletDelta> participantDeltas = Lists.newArrayList();
    for (ProtocolWaveletDelta protoDelta : deltas) {
      Pair<CoreWaveletDelta, HashedVersion> deltaAndVersion =
        CoreWaveletOperationSerializer.deserialize(protoDelta);
      CoreWaveletDelta delta = deltaAndVersion.first;
      List<CoreWaveletOperation> participantOps = Lists.newArrayList();
      for (CoreWaveletOperation op : delta.getOperations()) {
        if (op instanceof CoreAddParticipant || op instanceof CoreRemoveParticipant) {
          participantOps.add(op);
        }
      }
      if (!participantOps.isEmpty()) {
        CoreWaveletDelta indexDelta = new CoreWaveletDelta(delta.getAuthor(), participantOps);
        participantDeltas.add(CoreWaveletOperationSerializer.serialize(indexDelta,
            HashedVersion.unsigned(version),
            HashedVersion.unsigned(version + indexDelta.getOperations().size())));
        version += indexDelta.getOperations().size();
      }
    }
    return new DeltaSequence(participantDeltas,
        CoreWaveletOperationSerializer.serialize(HashedVersion.unsigned(version)));
  }

  /**
   * Determines the length (in number of characters) of the longest common
   * prefix of the specified two CharSequences. E.g. ("", "foo") -> 0.
   * ("foo", "bar) -> 0. ("foo", "foobar") -> 3. ("bar", "baz") -> 2.
   *
   * (Does this utility method already exist anywhere?)
   *
   * @throws NullPointerException if a or b is null
   */
  private static int lengthOfCommonPrefix(CharSequence a, CharSequence b) {
    int result = 0;
    int minLength = Math.min(a.length(), b.length());
    while (result < minLength && a.charAt(result) == b.charAt(result)) {
      result++;
    }
    return result;
  }
}
