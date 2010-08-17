/**
 * Copyright 2009 Google Inc.
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

package org.waveprotocol.wave.examples.fedone.frontend;

import static org.waveprotocol.wave.examples.fedone.common.CommonConstants.INDEX_WAVE_ID;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import org.waveprotocol.wave.examples.fedone.common.CoreWaveletOperationSerializer;
import org.waveprotocol.wave.examples.fedone.common.DeltaSequence;
import org.waveprotocol.wave.examples.fedone.common.HashedVersion;
import org.waveprotocol.wave.examples.fedone.util.Log;
import org.waveprotocol.wave.examples.fedone.waveclient.common.ClientUtils;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveletProvider;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveletSnapshotBuilder;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc.DocumentSnapshot;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc.WaveletSnapshot;
import org.waveprotocol.wave.federation.FederationErrors;
import org.waveprotocol.wave.federation.FederationErrorProto.FederationError;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletOperation;
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.id.IdConstants;
import org.waveprotocol.wave.model.id.IdUtil;
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
import org.waveprotocol.wave.model.wave.data.core.CoreWaveletData;
import org.waveprotocol.wave.waveserver.federation.SubmitResultListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Implements the client front-end.
 *
 * This class maintains a list of wavelets accessible by local participants by
 * inspecting all updates it receives (there is no need to inspect historic
 * deltas as they would have been received as updates had there been an
 * addParticipant). Updates are aggregated in a special index Wave which is
 * stored with the WaveServer.
 *
 * When a wavelet is added and it's not at version 0, buffer updates until a
 * request for the wavelet's history has completed.
 */
public class ClientFrontendImpl implements ClientFrontend {
  private static final Log LOG = Log.get(ClientFrontendImpl.class);

  private final static AtomicInteger channel_counter = new AtomicInteger(0);

  @VisibleForTesting
  static final ParticipantId DIGEST_AUTHOR = new ParticipantId("digest-author");
  @VisibleForTesting
  static final String DIGEST_DOCUMENT_ID = "digest";

  /** Information we hold in memory for each wavelet, including index wavelets. */
  private static class PerWavelet {
    private final Set<ParticipantId> participants;
    final AtomicLong timestamp;  // last modified time
    private final ProtocolHashedVersion version0;
    private ProtocolHashedVersion currentVersion;
    private String digest;

    PerWavelet(WaveletName waveletName) {
      this.participants = Collections.synchronizedSet(Sets.<ParticipantId>newHashSet());
      this.timestamp = new AtomicLong(0);
      this.version0 = CoreWaveletOperationSerializer.serialize(HashedVersion.versionZero(waveletName));
      this.currentVersion = version0;
      this.digest = "";
    }

    synchronized ProtocolHashedVersion getCurrentVersion() {
      return currentVersion;
    }

    synchronized void setCurrentVersion(ProtocolHashedVersion version) {
      this.currentVersion = version;
    }
  }

  /**
   * Constructs the name of the index wave wavelet that refers to the
   * specified conversation root wavelet.
   *
   * @param waveletName to refer to
   * @return WaveletName of the index wave wavelet referring to waveId
   * @throws IllegalArgumentException if waveId is the WaveId of the index wave
   * @throws NullPointerException if waveId is null
   */
  @VisibleForTesting
  static WaveletName indexWaveletNameFor(WaveletName waveletName) {
    Preconditions.checkArgument(isConversationRootWavelet(waveletName),
        "Not a conversation root wavelet: %s", waveletName);
    WaveId waveId = waveletName.waveId;
    Preconditions.checkArgument(!waveId.equals(INDEX_WAVE_ID),
        "There is no index wave wavelet for the index wave itself: %s", waveId);
    return WaveletName.of(INDEX_WAVE_ID, WaveletId.deserialise(waveId.serialise()));
  }

  private static boolean isConversationRootWavelet(WaveletName waveletName) {
    return waveletName.waveId.getDomain().equals(waveletName.waveletId.getDomain())
    && IdUtil.isConversationRootWaveletId(waveletName.waveletId);
  }

  @VisibleForTesting
  static WaveletName waveletNameForIndexWavelet(WaveletName indexWaveletName) {
    WaveId waveId = indexWaveletName.waveId;
    Preconditions.checkArgument(
        waveId.equals(INDEX_WAVE_ID), "Expected " + INDEX_WAVE_ID + ", got " + waveId);
    WaveletId waveletId = indexWaveletName.waveletId;
    return WaveletName.of(WaveId.deserialise(waveletId.serialise()),
        new WaveletId(waveletId.getDomain(), IdConstants.CONVERSATION_ROOT_WAVELET));
  }

  /** Maps wavelets to the participants currently on that wavelet */
  @VisibleForTesting final Map<ParticipantId, UserManager> perUser;
  private final Map<WaveletName, PerWavelet> perWavelet;
  private final WaveletProvider waveletProvider;

  @Inject
  public ClientFrontendImpl(WaveletProvider waveletProvider) {
    this.waveletProvider = waveletProvider;
    waveletProvider.setListener(this);
    MapMaker mapMaker = new MapMaker();
    perWavelet = mapMaker.makeComputingMap(new Function<WaveletName, PerWavelet>() {
      @Override public PerWavelet apply(WaveletName wn) { return new PerWavelet(wn); }
    });

    perUser = mapMaker.makeComputingMap(new Function<ParticipantId, UserManager>() {
      @Override public UserManager apply(ParticipantId from) { return new UserManager(); }
    });
  }

  @Override
  public void openRequest(ParticipantId participant, WaveId waveId, Set<String> waveletIdPrefixes,
      int maximumInitialWavelets, boolean snapshotsEnabled,
      final List<WaveClientRpc.WaveletVersion> knownWavelets, OpenListener openListener) {
    String channel_id = generateChannelID();

    LOG.info("received openRequest from " + participant + " for waveId " + waveId + " snapshots: "
        + snapshotsEnabled);
    if (waveletIdPrefixes == null || waveletIdPrefixes.isEmpty()) {
      waveletIdPrefixes = ImmutableSet.of("");
    }
    final boolean isIndexWave = waveId.equals(INDEX_WAVE_ID);
    UserManager userManager = perUser.get(participant);
    synchronized (userManager) {
      Set<WaveletId> waveletIds = userManager.subscribe(waveId, waveletIdPrefixes, channel_id,
          openListener);
      // Send this listener all deltas on relevant wavelets that we've already
      // sent out to other listeners, so that the listener can catch up with
      // those.
      // TODO: Because of the way we create the fake digest edit ops for the
      // index wave, if the listener is subscribing to the index wave then it
      // may be that this requires fewer ops than the existing listeners have
      // seen, leaving this listener at a different end version than other
      // listeners on the same wavelet. Fix!
      Map<WaveletName, ProtocolHashedVersion> knownWaveletVersions = Maps.newHashMap();
      if (knownWavelets != null) {
        for (WaveClientRpc.WaveletVersion waveletAndVersion : knownWavelets) {
          knownWaveletVersions.put(
              WaveletName.of(waveId, WaveletId.deserialise(waveletAndVersion.getWaveletId())),
              waveletAndVersion.getHashedVersion());
        }
      }
      for (WaveletId waveletId : waveletIds) {
        WaveletName waveletName = WaveletName.of(waveId, waveletId);

        // The WaveletName by which the waveletProvider knows the relevant deltas
        WaveletName sourceWaveletName =
          (isIndexWave ? waveletNameForIndexWavelet(waveletName) : waveletName);
        ProtocolHashedVersion startVersion;
        if (knownWaveletVersions.containsKey(waveletName)) { // Commented out for now.
          startVersion = knownWaveletVersions.get(waveletName);
          // TODO(arb): needs to be startVersion++
          // Known version is current version. We want current version + 1 to now.

        } else {
          startVersion = perWavelet.get(sourceWaveletName).version0;
        }
        ProtocolHashedVersion endVersion = userManager.getWaveletVersion(sourceWaveletName);

        List<ProtocolWaveletDelta> deltaList;
        WaveletSnapshotAndVersions snapshot;

        // TODO(arb): can I snapshot the indexWave?
        if (isIndexWave || !snapshotsEnabled) {
          DeltaSequence deltaSequence = new DeltaSequence(
              waveletProvider.getHistory(sourceWaveletName, startVersion, endVersion),
              endVersion);
          if (isIndexWave) { // Construct fake index wave deltas from the deltas
            String newDigest = perWavelet.get(sourceWaveletName).digest;
            // Nuke the request, and just return new deltas from v0.
            if (startVersion.getVersion() != 0) {
              LOG.warning("resetting index wave version from: " + startVersion);
            }
            startVersion = perWavelet.get(sourceWaveletName).version0;
            deltaSequence = createIndexDeltas(startVersion, deltaSequence, "", newDigest);
          }
          deltaList = deltaSequence;
          endVersion = deltaSequence.getEndVersion();
          snapshot = null;
        } else {
          // TODO(arb): when we have uncommitted deltas, look them up here.
          deltaList = Collections.emptyList();
          WaveletSnapshotBuilder<WaveletSnapshotAndVersions> snapshotBuilder =
            new WaveletSnapshotBuilder<WaveletSnapshotAndVersions>() {
            @Override
            public WaveletSnapshotAndVersions build(CoreWaveletData waveletData,
                HashedVersion currentVersion, ProtocolHashedVersion committedVersion) {
              return new WaveletSnapshotAndVersions(serializeSnapshot(waveletData),
                  currentVersion, committedVersion);
            }
          };
          snapshot = waveletProvider.getSnapshot(sourceWaveletName, snapshotBuilder);
        }

        // TODO: Once we've made sure that all listeners have received the
        // same number of ops for the index wavelet, enable the following check:
        //if (!deltaList.getEndVersion().equals(userManager.getWaveletVersion(waveletName))) {
        //  throw new IllegalStateException(..)
        // }

        LOG.info("snapshot in response is: " + (snapshot == null));
        if (snapshot == null) {
          // TODO(arb): get the LCV - maybe add to waveletProvider?
          openListener.onUpdate(waveletName, snapshot, deltaList, endVersion,
              null, false, channel_id);
        } else {
          openListener.onUpdate(waveletName, snapshot, deltaList, endVersion,
              snapshot.committedVersion, false, channel_id);
        }
      }

      final WaveletName dummyWaveletName = createDummyWaveletName(waveId);

      if (waveletIds.size() == 0) {
        // there were no wavelets, send just a channelid
        LOG.info("sending just a channel id for " + dummyWaveletName);
        openListener.onUpdate(dummyWaveletName,
            null, new ArrayList<ProtocolWaveletDelta>(), null, null, false, channel_id);
      }

      LOG.info("sending marker for " + dummyWaveletName);
      openListener.onUpdate(dummyWaveletName,
          null, new ArrayList<ProtocolWaveletDelta>(), null, null, true, null);
    }
  }

  private String generateChannelID() {
    return "ch" + channel_counter.addAndGet(1);
  }

  /**
   * Serializes a WaveletData into a WaveletSnapshot protobuffer.
   *
   * @param snapshot the snapshot
   * @return the new protobuffer
   */
  private WaveletSnapshot serializeSnapshot(CoreWaveletData snapshot) {
    WaveletSnapshot.Builder snapshotBuilder = WaveletSnapshot.newBuilder();
    Map<String, BufferedDocOp> documentMap = snapshot.getDocuments();
    for (Entry<String,BufferedDocOp> document : documentMap.entrySet()) {
      DocumentSnapshot.Builder documentBuilder = DocumentSnapshot.newBuilder();
      documentBuilder.setDocumentId(document.getKey());
      documentBuilder.setDocumentOperation(
          CoreWaveletOperationSerializer.serialize(document.getValue()));
      snapshotBuilder.addDocument(documentBuilder.build());
    }
    for (ParticipantId participant : snapshot.getParticipants()) {
      snapshotBuilder.addParticipantId(participant.toString());
    }
    return snapshotBuilder.build();
  }

  private static class SubmitResultListenerAdapter implements SubmitResultListener {
    private final SubmitResultListener listener;

    public SubmitResultListenerAdapter(SubmitResultListener listener) {
      this.listener = listener;
    }

    @Override
    public void onFailure(FederationError error) {
      listener.onFailure(error);
    }

    @Override
    public void onSuccess(int operationsApplied,
        ProtocolHashedVersion hashedVersionAfterApplication,
        long applicationTimestamp) {
      listener.onSuccess(operationsApplied, hashedVersionAfterApplication, applicationTimestamp);
    }
  }

  private boolean isWaveletWritable(WaveletName waveletName) {
    return !waveletName.waveId.equals(INDEX_WAVE_ID);
  }

  @Override
  public void submitRequest(final WaveletName waveletName, final ProtocolWaveletDelta delta,
      final String channelId, final SubmitResultListener listener) {
    final ParticipantId author = new ParticipantId(delta.getAuthor());
    if (!isWaveletWritable(waveletName)) {
      listener.onFailure(FederationErrors.badRequest("Wavelet " + waveletName + " is readonly"));
    } else {
      perUser.get(author).submitRequest(channelId, waveletName);
      waveletProvider.submitRequest(waveletName, delta, channelId,
          new SubmitResultListenerAdapter(listener) {
        @Override
        public void onSuccess(int operationsApplied,
            ProtocolHashedVersion hashedVersionAfterApplication, long applicationTimestamp) {
          super.onSuccess(operationsApplied, hashedVersionAfterApplication,
              applicationTimestamp);
          perWavelet.get(waveletName).timestamp.set(applicationTimestamp);
          perUser.get(author).submitResponse(channelId, waveletName,
              hashedVersionAfterApplication);
        }

        @Override
        public void onFailure(FederationError error) {
          super.onFailure(error);
          perUser.get(author).submitResponse(channelId, waveletName, null);
        }
      });
    }
  }

  @Override
  public void waveletCommitted(WaveletName waveletName, ProtocolHashedVersion version) {
    for (ParticipantId participant : perWavelet.get(waveletName).participants) {
      // TODO(arb): commits? channelId
      perUser.get(participant).onCommit(waveletName, version, null);
    }
  }

  private void onAdd(WaveletName waveletName, ParticipantId participant) {
    perWavelet.get(waveletName).participants.add(participant);
    perUser.get(participant).addWavelet(waveletName);
  }

  private void onRemove(WaveletName waveletName, ParticipantId participant) {
    perWavelet.get(waveletName).participants.remove(participant);
    perUser.get(participant).removeWavelet(waveletName);
  }

  /**
   * Sends new deltas to a particular user on a particular wavelet, and also
   * generates fake deltas for the index wavelet. If the user was added,
   * requests missing deltas from the waveletProvider. Updates the participants
   * of the specified wavelet if the participant was added or removed.
   *
   * @param waveletName which the deltas belong to
   * @param participant on the wavelet
   * @param newDeltas newly arrived deltas of relevance for participant.
   *        Must not be empty.
   * @param add whether the participant is added by the first delta
   * @param remove whether the participant is removed by the last delta
   * @param oldDigest The digest text of the wavelet before the deltas are
   *                  applied (but including all changes from preceding deltas)
   * @param newDigest The digest text of the wavelet after the deltas are applied
   * @param channelId The channel ID of this client (or null if not supported by the client)
   */
  @VisibleForTesting
  void participantUpdate(WaveletName waveletName, ParticipantId participant,
      DeltaSequence newDeltas, boolean add, boolean remove, String oldDigest, String newDigest,
      String channelId) {
    final DeltaSequence deltasToSend;
    if (add && newDeltas.getStartVersion().getVersion() > 0) {
      ProtocolHashedVersion version0 = perWavelet.get(waveletName).version0;
      ProtocolHashedVersion firstKnownDelta = newDeltas.getStartVersion();
      deltasToSend = newDeltas.prepend(
          waveletProvider.getHistory(waveletName, version0, firstKnownDelta));
      oldDigest = "";
    } else {
      deltasToSend = newDeltas;
    }
    if (add) {
      onAdd(waveletName, participant);
    }
    perUser.get(participant).onUpdate(waveletName, deltasToSend, channelId);
    if (remove) {
      onRemove(waveletName, participant);
    }

    // Construct and publish fake index wave deltas
    if (isConversationRootWavelet(waveletName)) {
      WaveletName indexWaveletName = indexWaveletNameFor(waveletName);
      if (add) {
        onAdd(indexWaveletName, participant);
      }
      ProtocolHashedVersion indexVersion =
        perUser.get(participant).getWaveletVersion(indexWaveletName);

      DeltaSequence indexDeltas = createIndexDeltas(indexVersion, deltasToSend,
          oldDigest, newDigest);
      if (!indexDeltas.isEmpty()) {
        perUser.get(participant).onUpdate(indexWaveletName, indexDeltas, channelId);
      }
      if (remove) {
        onRemove(indexWaveletName, participant);
      }
    }
  }

  /**
   * Based on deltas we receive from the wave server, pass the appropriate
   * membership changes and deltas from both the affected wavelets and the
   * corresponding index wave wavelets on to the UserManagers.
   */
  @Override
  public void waveletUpdate(WaveletName waveletName, List<ProtocolWaveletDelta> newDeltas,
      ProtocolHashedVersion endVersion, Map<String, BufferedDocOp> documentState,
      String channelId) {
    if (newDeltas.isEmpty()) {
      return;
    }

    final PerWavelet waveletInfo = perWavelet.get(waveletName);
    final ProtocolHashedVersion expectedVersion;
    final String oldDigest;
    final Set<ParticipantId> remainingParticipants;

    synchronized (waveletInfo) {
      expectedVersion = waveletInfo.getCurrentVersion();
      oldDigest = waveletInfo.digest;
      remainingParticipants = Sets.newHashSet(waveletInfo.participants);
    }

    DeltaSequence deltaSequence = new DeltaSequence(newDeltas, endVersion);

    Preconditions.checkState(expectedVersion.equals(deltaSequence.getStartVersion()),
        "Expected deltas starting at version %s, got %s",
        expectedVersion, deltaSequence.getStartVersion().getVersion());

    String newDigest = digest(ClientUtils.renderSnippet(documentState, 80));

    // Participants added during the course of newDeltas
    Set<ParticipantId> newParticipants = Sets.newHashSet();

    for (int i = 0; i < newDeltas.size(); i++) {
      ProtocolWaveletDelta delta = newDeltas.get(i);
      // Participants added or removed in this delta get the whole delta
      for (ProtocolWaveletOperation op : delta.getOperationList()) {
        if (op.hasAddParticipant()) {
          ParticipantId p = new ParticipantId(op.getAddParticipant());
          remainingParticipants.add(p);
          newParticipants.add(p);
        }
        if (op.hasRemoveParticipant()) {
          ParticipantId p = new ParticipantId(op.getRemoveParticipant());
          remainingParticipants.remove(p);
          participantUpdate(waveletName, p,
              deltaSequence.subList(0, i + 1), newParticipants.remove(p), true, oldDigest, "",
              channelId);
        }
      }
    }

    // Send out deltas to those who end up being participants at the end
    // (either because they already were, or because they were added).
    for (ParticipantId p : remainingParticipants) {
      boolean isNew = newParticipants.contains(p);
      participantUpdate(waveletName, p, deltaSequence, isNew, false, oldDigest, newDigest,
          channelId);
    }

    synchronized (waveletInfo) {
      waveletInfo.setCurrentVersion(deltaSequence.getEndVersion());
      waveletInfo.digest = newDigest;
    }
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

  /** Constructs a digest of the specified String. */
  private static String digest(String text) {
    int digestEndPos = text.indexOf('\n');
    if (digestEndPos < 0) {
      return text;
    } else {
      return text.substring(0, Math.min(80, digestEndPos));
    }
  }

  @VisibleForTesting
  static DeltaSequence createUnsignedDeltas(List<ProtocolWaveletDelta> deltas) {
    Preconditions.checkArgument(!deltas.isEmpty(), "No deltas specified");
    ProtocolWaveletDelta lastDelta = Iterables.getLast(deltas);
    long endVersion = lastDelta.getHashedVersion().getVersion() + lastDelta.getOperationCount();
    return new DeltaSequence(deltas,
        CoreWaveletOperationSerializer.serialize(HashedVersion.unsigned(endVersion)));
  }

  private static DeltaSequence participantDeltasOnly(long version,
      Iterable<ProtocolWaveletDelta> deltas) {
    List<ProtocolWaveletDelta> result = Lists.newArrayList();

    // Filter out operations that are of interest to the index wave wavelet
    // (add/remove participant operations):
    for (ProtocolWaveletDelta protoDelta : deltas) {
      Pair<CoreWaveletDelta, HashedVersion> deltaAndVersion =
        CoreWaveletOperationSerializer.deserialize(protoDelta);
      CoreWaveletDelta delta = deltaAndVersion.first;
      List<CoreWaveletOperation> indexOps = Lists.newArrayList();
      for (CoreWaveletOperation op : delta.getOperations()) {
        if (op instanceof CoreAddParticipant || op instanceof CoreRemoveParticipant) {
          indexOps.add(op);
        }
      }
      if (!indexOps.isEmpty()) {
        CoreWaveletDelta indexDelta = new CoreWaveletDelta(delta.getAuthor(), indexOps);
        result.add(CoreWaveletOperationSerializer.serialize(indexDelta,
            HashedVersion.unsigned(version),
            HashedVersion.unsigned(version + indexDelta.getOperations().size())));
        version += indexDelta.getOperations().size();
      }
    }
    return new DeltaSequence(result,
        CoreWaveletOperationSerializer.serialize(HashedVersion.unsigned(version)));
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

  @VisibleForTesting
  static ProtocolWaveletDelta createDigestDelta(HashedVersion version,
      String oldDigest, String newDigest) {
    if (oldDigest.equals(newDigest)) {
      return null;
    } else {
      CoreWaveletOperation op = new CoreWaveletDocumentOperation(DIGEST_DOCUMENT_ID,
          createEditOp(oldDigest, newDigest));
      CoreWaveletDelta indexDelta = new CoreWaveletDelta(DIGEST_AUTHOR, ImmutableList.of(op));
      return CoreWaveletOperationSerializer.serialize(indexDelta, version,
          HashedVersion.unsigned(version.getVersion() + 1));
    }
  }

  /**
   * Constructs the deltas that should be passed on to the index wave wavelet,
   * when the corresponding target wavelet receives the specified deltas
   * and the original wave's digest has changed as specified.
   *
   * The returned deltas will have the same effect on the participants as
   * the original deltas. The effect of the returned deltas on the document's
   * digest are purely a function of oldDigest and newDigest, which should
   * represent the change implied by deltas.
   *
   * @param indexVersion the returned deltas should start at
   * @param deltas The deltas whose effect on the participants to determine
   * @return deltas to apply to the index wavelet to achieve the same change
   *         in participants, and the specified change in digest text
   */
  private static DeltaSequence createIndexDeltas(ProtocolHashedVersion indexVersion,
      DeltaSequence deltas, String oldDigest, String newDigest) {
    long version = indexVersion.getVersion();
    ProtocolWaveletDelta digestDelta =
      createDigestDelta(HashedVersion.unsigned(version), oldDigest, newDigest);
    if (digestDelta != null) {
      version += digestDelta.getOperationCount();
    }
    DeltaSequence participantDeltas = participantDeltasOnly(version, deltas);
    if (digestDelta == null) {
      return participantDeltas;
    } else {
      return participantDeltas.prepend(ImmutableList.of(digestDelta));
    }
  }

  @VisibleForTesting
  static WaveletName createDummyWaveletName(WaveId waveId) {
    final WaveletName dummyWaveletName =
      WaveletName.of(waveId, new WaveletId(waveId.getDomain(), "dummy+root"));
    return dummyWaveletName;
  }
}
