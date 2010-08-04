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

package org.waveprotocol.wave.examples.fedone.waveserver;

import com.google.inject.Inject;
import com.google.inject.internal.Nullable;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;

import org.waveprotocol.wave.examples.fedone.util.Log;
import org.waveprotocol.wave.examples.fedone.util.URLEncoderDecoderBasedPercentEncoderDecoder;
import org.waveprotocol.wave.examples.fedone.waveserver.ClientFrontend.OpenListener;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc.ProtocolOpenRequest;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc.ProtocolSubmitRequest;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc.ProtocolSubmitResponse;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc.ProtocolWaveClientRpc;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc.ProtocolWaveletUpdate;
import org.waveprotocol.wave.federation.FederationErrorProto.FederationError;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.URIEncoderDecoder.EncodingException;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.waveserver.federation.SubmitResultListener;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * RPC interface implementation for the wave server. Adapts incoming and
 * outgoing RPCs to the client frontend interface.
 *
 *
 */
public class WaveClientRpcImpl implements ProtocolWaveClientRpc.Interface {

  private static final Log LOG = Log.get(WaveClientRpcImpl.class);

  private final ClientFrontend frontend;

  private final IdURIEncoderDecoder uriCodec = new IdURIEncoderDecoder(
      new URLEncoderDecoderBasedPercentEncoderDecoder());

  /**
   * Constructor.
   *
   * @param frontend ClientFrontend that consumes the operations.
   */
  @Inject
  public WaveClientRpcImpl(ClientFrontend frontend) {
    this.frontend = frontend;
  }

  @Override
  public void open(final RpcController controller, ProtocolOpenRequest request,
      final RpcCallback<ProtocolWaveletUpdate> done) {
    ParticipantId id = new ParticipantId(request.getParticipantId());
    WaveId waveId;
    try {
      waveId = WaveId.deserialise(request.getWaveId());
    } catch (IllegalArgumentException e) {
      LOG.warning(e.getMessage());
      controller.setFailed(e.getMessage());
      return;
    }
    Set<String> prefixes = new HashSet<String>(request.getWaveletIdPrefixCount());
    for (int i = 0; i < request.getWaveletIdPrefixCount(); ++i) {
      prefixes.add(request.getWaveletIdPrefix(0));
    }

    frontend.openRequest(id, waveId, prefixes, request.getMaximumWavelets(),
        request.getSnapshots(),
        request.getKnownWaveletsCount() > 0 ? request.getKnownWaveletsList() : null,
        new OpenListener() {

          @Override
          public void onFailure(String errorMessage) {
            LOG.warning("openRequest failure: " + errorMessage);
            controller.setFailed(errorMessage);
          }

          @Override
          public void onUpdate(WaveletName waveletName,
              @Nullable WaveletSnapshotAndVersions snapshot,
              List<ProtocolWaveletDelta> deltas, @Nullable ProtocolHashedVersion endVersion,
              @Nullable ProtocolHashedVersion committedVersion, final boolean hasMarker,
              final String channel_id) {
            ProtocolWaveletUpdate.Builder builder = ProtocolWaveletUpdate.newBuilder();
            builder.setMarker(hasMarker);
            if (channel_id != null) {
              builder.setChannelId(channel_id);
            }
            try {
              builder.setWaveletName(uriCodec.waveletNameToURI(waveletName));
              builder.addAllAppliedDelta(deltas);
              if (snapshot != null) {
                builder.setSnapshot(snapshot.snapshot);
                if (snapshot.currentVersion != null) {
                  builder.setResultingVersion(snapshot.currentVersion);
                }
                if (snapshot.committedVersion != null) {
                  builder.setCommitNotice(snapshot.committedVersion);
                } else {
                  // TODO(arb): HACK. lastCommittedVersion isn't ever set in the waveserver!?
                  builder.setCommitNotice(snapshot.currentVersion);
                }
              } else {
                if (endVersion != null) {
                  builder.setResultingVersion(endVersion);
                }
                if (committedVersion != null) {
                  builder.setCommitNotice(committedVersion);
                }
              }

              done.run(builder.build());
            } catch (EncodingException e) {
              LOG.warning(e.getMessage());
              controller.setFailed(e.getMessage());
            }
          }
        });
  }

  @Override
  public void submit(RpcController controller, ProtocolSubmitRequest request,
      final RpcCallback<ProtocolSubmitResponse> done) {
    WaveletName waveletName;
    String errorMessage = null;
    try {
      waveletName = uriCodec.uriToWaveletName(request.getWaveletName());
      String channelId;
      if (request.hasChannelId()) {
        channelId = request.getChannelId();
      } else {
        channelId = null;
      }
      frontend.submitRequest(waveletName, request.getDelta(), channelId,
          new SubmitResultListener() {
            @Override
            public void onFailure(FederationError error) {
              done.run(ProtocolSubmitResponse.newBuilder()
                  .setOperationsApplied(0).setErrorMessage(error.getErrorMessage()).build());
            }

            @Override
            public void onSuccess(int operationsApplied,
                ProtocolHashedVersion hashedVersionAfterApplication,
                long applicationTimestamp) {
              done.run(ProtocolSubmitResponse.newBuilder()
                  .setOperationsApplied(operationsApplied)
                  .setHashedVersionAfterApplication(hashedVersionAfterApplication).build());
              // TODO(arb): applicationTimestamp??
            }
          });
    } catch (EncodingException e) {
      errorMessage = e.getMessage();
    }

    if (errorMessage != null) {
      LOG.warning(errorMessage);
      done.run(ProtocolSubmitResponse.newBuilder()
          .setOperationsApplied(0).setErrorMessage(errorMessage).build());
    }
  }
}
