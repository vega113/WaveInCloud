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

import static org.waveprotocol.wave.examples.common.CommonConstants.INDEX_WAVE_ID;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.waveprotocol.wave.examples.client.common.IndexEntry;
import org.waveprotocol.wave.examples.common.Snippets;
import org.waveprotocol.wave.examples.fedone.common.DeltaSequence;
import org.waveprotocol.wave.examples.fedone.common.VersionedWaveletDelta;
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.id.IdConstants;
import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.core.CoreAddParticipant;
import org.waveprotocol.wave.model.operation.core.CoreNoOp;
import org.waveprotocol.wave.model.operation.core.CoreRemoveParticipant;
import org.waveprotocol.wave.model.operation.core.CoreWaveletDelta;
import org.waveprotocol.wave.model.operation.core.CoreWaveletDocumentOperation;
import org.waveprotocol.wave.model.operation.core.CoreWaveletOperation;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.WaveletData;

import java.util.Iterator;
import java.util.List;

/**
 * Utilities for mutating the index wave, a list of waves visible to a user. The
 * index wave has a wavelet for each wave in the index. That wavelet has a
 * digest document containing a snippet of text.
 *
 * TODO(anorth): replace this with a more canonical use of the wave model.
 *
 * @author anorth@google.com (Alex North)
 * @author mk.mateng@gmail.com (Michael Kuntzman)
 */
public final class IndexWave {

  @VisibleForTesting
  public static final ParticipantId DIGEST_AUTHOR = new ParticipantId("digest-author");
  @VisibleForTesting
  public static final String DIGEST_DOCUMENT_ID = "digest";

  /**
   * @return true if the specified wave can be indexed in an index wave.
   */
  public static boolean canBeIndexed(WaveId waveId) {
    return !isIndexWave(waveId);
  }

  /**
   * @return true if the specified wavelet name can be encoded into an index
   *         wave.
   */
  public static boolean canBeIndexed(WaveletName waveletName) {
    WaveId waveId = waveletName.waveId;
    WaveletId waveletId = waveletName.waveletId;

    return canBeIndexed(waveId) && waveId.getDomain().equals(waveletId.getDomain())
        && IdUtil.isConversationRootWaveletId(waveletId);
  }

  /**
   * Constructs the deltas for an index wave wavelet in response to deltas on an
   * original conversation wavelet.
   *
   * The created delta sequence will have the same effect on the participants as
   * the original deltas and will update the digest as a function of oldDigest
   * and newDigest. The sequence may contain no-ops to pad it to the same length
   * as the source deltas.
   *
   * @param sourceDeltas conversational deltas to process
   * @param oldDigest the current index digest
   * @param newDigest the new digest
   * @return deltas to apply to the index wavelet to achieve the same change in
   *         participants, and the specified change in digest text
   */
  public static DeltaSequence createIndexDeltas(long targetVersion, DeltaSequence sourceDeltas,
      String oldDigest, String newDigest) {
    long deltaTargetVersion = targetVersion; // Target for the next delta.
    long numSourceOps = sourceDeltas.getEndVersion().getVersion() - targetVersion;
    List<CoreWaveletDelta> indexDeltas = createParticipantDeltas(sourceDeltas, deltaTargetVersion);
    long numIndexOps = numOpsInDeltas(indexDeltas);
    deltaTargetVersion += numIndexOps;

    if (numIndexOps < numSourceOps) {
      indexDeltas.add(createDigestDelta(deltaTargetVersion, oldDigest, newDigest));
      numIndexOps += 1;
      deltaTargetVersion += 1;
    }

    if (numIndexOps < numSourceOps) {
      // Append no-ops.
      long numNoOps = numSourceOps - numIndexOps;
      List<CoreWaveletOperation> noOps = Lists.newArrayList();
      for (long i = 0; i < numNoOps; ++i) {
        noOps.add(CoreNoOp.INSTANCE);
      }
      CoreWaveletDelta noOpDelta =
          new CoreWaveletDelta(DIGEST_AUTHOR, HashedVersion.unsigned(deltaTargetVersion), noOps);
      indexDeltas.add(noOpDelta);
    }

    return new DeltaSequence(asVersionedDeltas(indexDeltas, targetVersion),
        HashedVersion.unsigned(sourceDeltas.getEndVersion().getVersion()));
  }

  /**
   * Retrieve a list of index entries from an index wave.
   *
   * @param wavelets wavelets to retrieve the index from.
   * @return list of index entries.
   */
  public static List<IndexEntry> getIndexEntries(Iterable<? extends WaveletData> wavelets) {
    List<IndexEntry> indexEntries = Lists.newArrayList();

    for (WaveletData wavelet : wavelets) {
      WaveId waveId = waveIdFromIndexWavelet(wavelet);
      String digest = Snippets.collateTextForWavelet(wavelet);
      indexEntries.add(new IndexEntry(waveId, digest));
    }
    return indexEntries;
  }

  /**
   * Constructs the name of the index wave wavelet that refers to the specified
   * wave.
   *
   * @param waveId referent wave id
   * @return WaveletName of the index wave wavelet referring to waveId
   * @throws IllegalArgumentException if the wave cannot be indexed
   */
  public static WaveletName indexWaveletNameFor(WaveId waveId) {
    Preconditions.checkArgument(canBeIndexed(waveId), "Wave %s cannot be indexed", waveId);
    return WaveletName.of(INDEX_WAVE_ID, WaveletId.deserialise(waveId.serialise()));
  }

  /**
   * @return true if the specified wave ID is an index wave ID.
   */
  public static boolean isIndexWave(WaveId waveId) {
    return waveId.equals(INDEX_WAVE_ID);
  }

  /**
   * Extracts the wave id referred to by an index wavelet's wavelet name.
   *
   * @param indexWavelet the index wavelet.
   * @return the wave id.
   * @throws IllegalArgumentException if the wavelet is not from an index wave.
   */
  public static WaveId waveIdFromIndexWavelet(WaveletData indexWavelet) {
    return waveIdFromIndexWavelet(
        WaveletName.of(indexWavelet.getWaveId(), indexWavelet.getWaveletId()));
  }

  /**
   * Extracts the wave id referred to by an index wavelet name.
   *
   * @param indexWaveletName of the index wavelet.
   * @return the wave id.
   * @throws IllegalArgumentException if the wavelet is not from an index wave.
   */
  public static WaveId waveIdFromIndexWavelet(WaveletName indexWaveletName) {
    WaveId waveId = indexWaveletName.waveId;
    Preconditions.checkArgument(isIndexWave(waveId), waveId + " is not an index wave");
    return WaveId.deserialise(indexWaveletName.waveletId.serialise());
  }

  /**
   * Extracts a wavelet name referring to the conversational root wavelet in the
   * wave referred to by an index wavelet name.
   */
  public static WaveletName waveletNameFromIndexWavelet(WaveletName indexWaveletName) {
    return WaveletName.of(IndexWave.waveIdFromIndexWavelet(indexWaveletName), new WaveletId(
        indexWaveletName.waveletId.getDomain(), IdConstants.CONVERSATION_ROOT_WAVELET));
  }

  /**
   * Counts the ops in a sequence of deltas
   */
  private static long numOpsInDeltas(Iterable<CoreWaveletDelta> deltas) {
    long sum = 0;
    for (CoreWaveletDelta d : deltas) {
      sum += d.getOperations().size();
    }
    return sum;
  }

  /**
   * Constructs a delta with one op which transforms the digest document from
   * one digest string to another.
   */
  private static CoreWaveletDelta createDigestDelta(long targetVersion, String oldDigest,
      String newDigest) {
    CoreWaveletOperation op =
        new CoreWaveletDocumentOperation(DIGEST_DOCUMENT_ID, createEditOp(oldDigest, newDigest));
    CoreWaveletDelta delta = new CoreWaveletDelta(DIGEST_AUTHOR,
        HashedVersion.unsigned(targetVersion), ImmutableList.of(op));
    return delta;
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
  private static List<CoreWaveletDelta> createParticipantDeltas(
      Iterable<VersionedWaveletDelta> deltas, long targetVersion) {
    List<CoreWaveletDelta> participantDeltas = Lists.newArrayList();
    for (VersionedWaveletDelta delta : deltas) {
      List<CoreWaveletOperation> participantOps = Lists.newArrayList();
      for (CoreWaveletOperation op : delta.delta.getOperations()) {
        if (op instanceof CoreAddParticipant || op instanceof CoreRemoveParticipant) {
          participantOps.add(op);
        }
      }
      if (!participantOps.isEmpty()) {
        HashedVersion hashedVersion = HashedVersion.unsigned(targetVersion);
        targetVersion += participantOps.size();
        participantDeltas.add(new CoreWaveletDelta(delta.delta.getAuthor(), hashedVersion,
            participantOps));
      }
    }
    return participantDeltas;
  }

  /**
   * Determines the length (in number of characters) of the longest common
   * prefix of the specified two CharSequences. E.g. ("", "foo") -> 0. ("foo",
   * "bar) -> 0. ("foo", "foobar") -> 3. ("bar", "baz") -> 2.
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

  /**
   * Adapts CoreWaveletDeltas as ProtocolWaveletDeltas beginning at some target
   * version.
   */
  private static Iterable<VersionedWaveletDelta> asVersionedDeltas(
      final Iterable<CoreWaveletDelta> deltas, final long targetVersion) {
    return new Iterable<VersionedWaveletDelta>() {
      @Override
      public Iterator<VersionedWaveletDelta> iterator() {
        return new Iterator<VersionedWaveletDelta>() {

          Iterator<CoreWaveletDelta> inner = deltas.iterator();
          HashedVersion nextTargetVersion = HashedVersion.unsigned(targetVersion);

          @Override
          public boolean hasNext() {
            return inner.hasNext();
          }

          @Override
          public VersionedWaveletDelta next() {
            CoreWaveletDelta nextCore = inner.next();
            try {
              return new VersionedWaveletDelta(nextCore, nextTargetVersion);
            } finally {
              nextTargetVersion = HashedVersion.unsigned(nextTargetVersion.getVersion()
                      + nextCore.getOperations().size());
            }
          }

          @Override
          public void remove() {
            throw new UnsupportedOperationException();
          }
        };
      }
    };
  }
}
