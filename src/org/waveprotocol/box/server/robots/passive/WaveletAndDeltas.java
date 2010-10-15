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

package org.waveprotocol.box.server.robots.passive;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import org.waveprotocol.box.server.util.WaveletDataUtil;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.core.CoreWaveletDelta;
import org.waveprotocol.wave.model.operation.core.CoreWaveletOperation;
import org.waveprotocol.wave.model.operation.wave.ConversionUtil;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.model.wave.data.WaveletData;

import java.util.Collections;
import java.util.List;

/**
 * A wavelet snapshot and a sequence of deltas applying to that snapshot.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class WaveletAndDeltas {

  /**
   * Snapshot of the wavelet before any deltas are applied.
   */
  private final ReadableWaveletData snapshotBeforeDeltas;

  /**
   * Contiguous sequence of deltas applying to snapshotBeforeDeltas.
   */
  private final List<CoreWaveletDelta> deltas;

  /**
   * Cached result of applying all deltas to the first snapshot.
   */
  private ObservableWaveletData snapshotAfterDeltas;

  /**
   * The version of the snapshot with all deltas applied.
   */
  private HashedVersion currentVersion;

  /**
   * The name of the wavelet in this container.
   */
  private final WaveletName waveletName;

  /**
   * Constructs a {@link WaveletAndDeltas} from the given {@link WaveletData}
   * and {@link CoreWaveletDelta}s. It will construct a copy of the
   * WaveletData so that operations can happily applied to it.
   *
   * @param snapshot the state of the wavelet after the deltas have been
   *        applied.
   * @param deltas the deltas in the order they have been applied to the
   *        wavelet.
   * @param resultingVersion version after application of the deltas, must
   *        therefore match the version of the snapshot.
   * @throws OperationException if the operations can not be rolled back to
   *         create a snapshot before the deltas have been applied.
   */
  public static WaveletAndDeltas create(ReadableWaveletData snapshot,
      List<CoreWaveletDelta> deltas, HashedVersion resultingVersion)
      throws OperationException {
    Preconditions.checkArgument(snapshot.getVersion() == resultingVersion.getVersion(),
        String.format("Version of snapshot %s doesn't match the HashedVersion %s",
            snapshot.getVersion(), resultingVersion));
    Preconditions.checkArgument(
        areContiguousDeltas(deltas), "Deltas are not contiguous: " + deltas);

    ObservableWaveletData preDeltaWavelet = WaveletDataUtil.copyWavelet(snapshot);
    rollback(preDeltaWavelet, deltas);
    ObservableWaveletData postDeltaWavelet = WaveletDataUtil.copyWavelet(snapshot);
    return new WaveletAndDeltas(preDeltaWavelet, postDeltaWavelet, deltas, resultingVersion);
  }

  /**
   * Reverses the operations detailed in the list of deltas on the given
   * wavelet.
   *
   * @param wavelet {@link ObservableWaveletData} to apply operations to
   * @param deltas the {@link CoreWaveletDelta} containing the operations
   *        which we should revert on the given wavelet.
   * @throws OperationException if the operations can not be rolled back.
   */
  private static void rollback(ObservableWaveletData wavelet, List<CoreWaveletDelta> deltas)
      throws OperationException {
    List<WaveletOperation> inverseOps = Lists.newArrayList();

    // Go through everything in reverse order
    for (int i = deltas.size() - 1; i >= 0; i--) {
      CoreWaveletDelta delta = deltas.get(i);
      List<? extends CoreWaveletOperation> coreOps = delta.getOperations();
      // Metadata such as the last modified ts will change due to the rollback
      // of operations.
      long timestamp = 0L;
      WaveletOperationContext context =
          new WaveletOperationContext(delta.getAuthor(), timestamp, -1);
      for (int j = coreOps.size() - 1; j >= 0; j--) {
        CoreWaveletOperation coreOp = coreOps.get(j);
        WaveletOperation inverseOp =
            ConversionUtil.fromCoreWaveletOperation(context, coreOp.getInverse());
        inverseOps.add(inverseOp);
      }
    }

    long startVersion = wavelet.getVersion();
    int opCount = 0;
    for (WaveletOperation inverseOp : inverseOps) {
      inverseOp.apply(wavelet);
      opCount++;
    }
    if (wavelet.getVersion() != startVersion - opCount) {
      throw new OperationException("Expected end version " + (startVersion - opCount)
          + " doesn't match the version of the wavelet " + wavelet.getVersion());
    }
  }

  /**
   * Checks whether the deltas are contiguous. Meaning that their version
   * numbers are in proper ascending order.
   *
   * @param deltas the deltas to check to be contiguous.
   * @return true if the deltas are contiguous, false otherwise.
   */
  private static boolean areContiguousDeltas(List<CoreWaveletDelta> deltas) {
    if (deltas.size() <= 1) {
      return true;
    }

    CoreWaveletDelta first = deltas.get(0);
    long nextVersion = first.getTargetVersion().getVersion() + first.getOperations().size();

    for (int i = 1; i < deltas.size(); i++) {
      CoreWaveletDelta delta = deltas.get(i);
      long version = delta.getTargetVersion().getVersion();
      if (version != nextVersion) {
        return false;
      }
      nextVersion = version + delta.getOperations().size();
    }
    return true;
  }

  /**
   * Constructs a {@link WaveletAndDeltas} from the given {@link WaveletData}
   * and {@link CoreWaveletDelta}s.
   *
   * @param preDeltasSnapshot the state of the wavelet before the deltas have
   *        been applied.
   * @param postDeltasSnapshot the state of the wavelet after the deltas have
   *        been applied.
   * @param deltas deltas in the order they have beenapplied to the wavelet.
   * @param resultingVersion version after application of the deltas.
   */
  private WaveletAndDeltas(ObservableWaveletData preDeltasSnapshot,
      ObservableWaveletData postDeltasSnapshot, List<CoreWaveletDelta> deltas,
      HashedVersion resultingVersion) {
    this.snapshotBeforeDeltas = preDeltasSnapshot;
    this.deltas = Lists.newArrayList(deltas);
    this.snapshotAfterDeltas = postDeltasSnapshot;
    this.currentVersion = resultingVersion;
    this.waveletName = WaveletDataUtil.waveletNameOf(preDeltasSnapshot);
  }

  /**
   * Returns the wavelet before any deltas have been applied.
   */
  public ReadableWaveletData getSnapshotBeforeDeltas() {
    return snapshotBeforeDeltas;
  }

  /**
   * Returns an unmodifiable view of all deltas collected.
   */
  public List<CoreWaveletDelta> getDeltas() {
    return Collections.unmodifiableList(deltas);
  }

  /**
   * Returns the latest snapshot with all deltas applied.
   */
  public ReadableWaveletData getSnapshotAfterDeltas() {
    return snapshotAfterDeltas;
  }

  /**
   * Returns the {@link HashedVersion} of the wavelet after all deltas have been
   * applied.
   */
  public HashedVersion getVersionAfterDeltas() {
    return currentVersion;
  }

  /**
   * Appends the given deltas to the deltas already stored. Updates the latest
   * snapshot and latest version as well. This method will make a copy of the
   * snapshot.
   *
   * @param updatedSnapshot the snapshot after deltas have been applied
   * @param newDeltas the deltas that have been applied since the last call to
   *        appendDeltas.
   * @param newVersion version after application of the deltas.
   */
  public void appendDeltas(ReadableWaveletData updatedSnapshot,
      List<CoreWaveletDelta> newDeltas, HashedVersion newVersion) {
    Preconditions.checkArgument(
        !newDeltas.isEmpty(), "There were no new deltas passed to appendDeltas");
    Preconditions.checkArgument(updatedSnapshot.getVersion() == newVersion.getVersion(),
        String.format("Version of snapshot %s doesn't match the HashedVersion %s",
            updatedSnapshot.getVersion(), newVersion));
    Preconditions.checkArgument(areContiguousToCurrentVersion(newDeltas), String.format(
        "Deltas are not contiguous to the current version(%s) %s", currentVersion, deltas));
    WaveletName updatedWaveletName = WaveletDataUtil.waveletNameOf(updatedSnapshot);
    Preconditions.checkArgument(updatedWaveletName.equals(waveletName),
        String.format(
            "Updated wavelet doesn't have the same name as with which this class has been "
                + "instantiated. %s != %s", updatedWaveletName, waveletName));

    // TODO(ljvderijk): This should actually be applying the deltas, however
    // they do not contain a timestamp at this time.
    snapshotAfterDeltas = WaveletDataUtil.copyWavelet(updatedSnapshot);
    deltas.addAll(newDeltas);
    currentVersion = newVersion;
  }

  /**
   * Returns true if the given deltas apply to the current version of this
   * wavelet and they are contiguous.
   *
   * @param deltas the list of deltas to check.
   */
  public boolean areContiguousToCurrentVersion(List<CoreWaveletDelta> deltas) {
    return deltas.get(0).getTargetVersion().equals(currentVersion) && areContiguousDeltas(deltas);
  }
}
