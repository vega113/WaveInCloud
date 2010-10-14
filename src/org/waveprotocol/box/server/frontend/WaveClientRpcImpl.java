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

package org.waveprotocol.box.server.frontend;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;

import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.box.server.util.Log;
import org.waveprotocol.box.server.util.URLEncoderDecoderBasedPercentEncoderDecoder;
import org.waveprotocol.box.server.waveserver.WaveClientRpc.ProtocolOpenRequest;
import org.waveprotocol.box.server.waveserver.WaveClientRpc.ProtocolSubmitRequest;
import org.waveprotocol.box.server.waveserver.WaveClientRpc.ProtocolSubmitResponse;
import org.waveprotocol.box.server.waveserver.WaveClientRpc.ProtocolWaveClientRpc;
import org.waveprotocol.box.server.waveserver.WaveClientRpc.ProtocolWaveletUpdate;
import org.waveprotocol.box.server.waveserver.WaveletProvider.SubmitRequestListener;
import org.waveprotocol.wave.model.id.IdFilter;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.id.URIEncoderDecoder.EncodingException;
import org.waveprotocol.wave.model.operation.core.CoreWaveletDelta;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

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
    IdFilter waveletIdFilter = IdFilter.of(Collections.<WaveletId>emptySet(), request.getWaveletIdPrefixList());

    frontend.openRequest(id, waveId, waveletIdFilter, request.getKnownWaveletList(),
        new ClientFrontend.OpenListener() {
          @Override
          public void onFailure(String errorMessage) {
            LOG.warning("openRequest failure: " + errorMessage);
            controller.setFailed(errorMessage);
          }

          @Override
          public void onUpdate(WaveletName waveletName,
              @Nullable WaveletSnapshotAndVersion snapshot,
              List<CoreWaveletDelta> deltas, @Nullable HashedVersion endVersion,
              @Nullable HashedVersion committedVersion, final boolean hasMarker,
              final String channel_id) {
            ProtocolWaveletUpdate.Builder builder = ProtocolWaveletUpdate.newBuilder();
            builder.setMarker(hasMarker);
            if (channel_id != null) {
              builder.setChannelId(channel_id);
            }
            try {
              builder.setWaveletName(uriCodec.waveletNameToURI(waveletName));
              for (CoreWaveletDelta d : deltas) {
                builder.addAppliedDelta(CoreWaveletOperationSerializer.serialize(d));
              }
              if (snapshot != null) {
                Preconditions.checkState(committedVersion.equals(
                    CoreWaveletOperationSerializer.deserialize(snapshot.committedVersion)),
                    "Mismatched commit versions, snapshot: " + snapshot.committedVersion
                    + " expected: " + committedVersion);
                builder.setSnapshot(snapshot.snapshot);
                builder.setResultingVersion(snapshot.snapshot.getVersion());
                builder.setCommitNotice(snapshot.committedVersion);
              } else {
                if (endVersion != null) {
                  builder.setResultingVersion(
                      CoreWaveletOperationSerializer.serialize(endVersion));
                }
                if (committedVersion != null) {
                  builder.setCommitNotice(
                      CoreWaveletOperationSerializer.serialize(committedVersion));
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
          new SubmitRequestListener() {
            @Override
            public void onFailure(String error) {
              done.run(ProtocolSubmitResponse.newBuilder()
                  .setOperationsApplied(0).setErrorMessage(error).build());
            }

            @Override
            public void onSuccess(int operationsApplied,
                HashedVersion hashedVersionAfterApplication, long applicationTimestamp) {
              done.run(ProtocolSubmitResponse.newBuilder()
                  .setOperationsApplied(operationsApplied)
                  .setHashedVersionAfterApplication(
                      CoreWaveletOperationSerializer.serialize(hashedVersionAfterApplication))
                  .build());
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
