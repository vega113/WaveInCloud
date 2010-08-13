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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.internal.Nullable;
import com.google.protobuf.ByteString;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;

import junit.framework.TestCase;

import org.waveprotocol.wave.examples.fedone.common.CoreWaveletOperationSerializer;
import org.waveprotocol.wave.examples.fedone.common.HashedVersion;
import org.waveprotocol.wave.examples.fedone.util.URLEncoderDecoderBasedPercentEncoderDecoder;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc.ProtocolOpenRequest;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc.ProtocolSubmitRequest;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc.ProtocolSubmitResponse;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc.ProtocolWaveletUpdate;
import org.waveprotocol.wave.federation.FederationErrors;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletOperation;
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.id.URIEncoderDecoder.EncodingException;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.waveserver.federation.SubmitResultListener;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tests for the {@link WaveClientRpcImpl}.
 */
public class WaveClientRpcImplTest extends TestCase {
  /**
   * Implementation of a ClientFrontend which only records requests and
   * will make callbacks when it receives wavelet listener events.
   */
  static class FakeClientFrontendImpl implements ClientFrontend {
    static class SubmitRecord {
      final SubmitResultListener listener;
      final int operations;
      SubmitRecord(int operations, SubmitResultListener listener) {
        this.operations = operations;
        this.listener = listener;
      }
    }

    private final Map<WaveId, OpenListener> openListeners = new HashMap<WaveId, OpenListener>();

    private final Map<WaveletName, SubmitRecord> submitRecords =
        new HashMap<WaveletName, SubmitRecord>();

    public void doSubmitFailed(WaveletName waveletName) {
      SubmitRecord record = submitRecords.remove(waveletName);
      if (record != null) {
        record.listener.onFailure(FederationErrors.badRequest(FAIL_MESSAGE));
      }
    }

    public void doSubmitSuccess(WaveletName waveletName) {
      SubmitRecord record = submitRecords.remove(waveletName);
      ProtocolHashedVersion fakeHashedVersion =
          ProtocolHashedVersion.newBuilder().setVersion(0).setHistoryHash(ByteString.EMPTY).build();
      if (record != null) {
        record.listener.onSuccess(record.operations, fakeHashedVersion, 0);
      }
    }

    public void doUpdateFailure(WaveId waveId, String errorMessage) {
      OpenListener listener = openListeners.get(waveId);
      if (listener != null) {
        listener.onFailure(errorMessage);
      }
    }

    @Override
    public void openRequest(ParticipantId participant, WaveId waveId,
        Set<String> waveletIdPrefixes, int maximumInitialWavelets, boolean snapshotsEnabled,
        final List<WaveClientRpc.WaveletVersion> knownWavelets, OpenListener openListener) {
      openListeners.put(waveId, openListener);
    }

    @Override
    public void submitRequest(WaveletName waveletName, ProtocolWaveletDelta delta,
        @Nullable String channelId,
        SubmitResultListener listener) {
      submitRecords.put(waveletName, new SubmitRecord(delta.getOperationCount(), listener));
    }

    @Override
    public void waveletCommitted(WaveletName waveletName, ProtocolHashedVersion version) {
      OpenListener listener = openListeners.get(waveletName.waveId);
      if (listener != null) {
        final List<ProtocolWaveletDelta> emptyList = Collections.emptyList();
        listener.onUpdate(waveletName, null, emptyList, null, HASHED_VERSION, false, null);
      }
    }

    @Override
    public void waveletUpdate(WaveletName waveletName, List<ProtocolWaveletDelta> newDeltas,
        ProtocolHashedVersion resultingVersion, Map<String, BufferedDocOp> documentState,
        @Nullable String channelId) {
      OpenListener listener = openListeners.get(waveletName.waveId);
      if (listener != null) {
        listener.onUpdate(waveletName, null, newDeltas, resultingVersion, null, false, null);
      }
    }
  }

  private static final String FAIL_MESSAGE = "Failed";

  private static final ProtocolHashedVersion HASHED_VERSION =
    CoreWaveletOperationSerializer.serialize(HashedVersion.unsigned(101L));

  private static final ParticipantId USER = new ParticipantId("user@host.com");

  private static final WaveId WAVE_ID = new WaveId("waveId", "1");

  private static final WaveletId WAVELET_ID = new WaveletId("waveletId", "A");

  private static final WaveletName WAVELET_NAME = WaveletName.of(WAVE_ID, WAVELET_ID);

  private static final ProtocolWaveletDelta DELTA = ProtocolWaveletDelta.newBuilder()
    .setAuthor(USER.getAddress())
    .setHashedVersion(HASHED_VERSION)
    .addOperation(ProtocolWaveletOperation.newBuilder().build()).build();

  private static final ImmutableList<ProtocolWaveletDelta> DELTAS = ImmutableList.of(DELTA);

  private static final ProtocolHashedVersion RESULTING_VERSION =
    CoreWaveletOperationSerializer.serialize(HashedVersion.unsigned(102L));

  /** RpcController that just handles error text and failure condition. */
  private final RpcController controller = new RpcController() {

    private boolean failed = false;
    private String errorText = null;

    @Override
    public String errorText() {
      return errorText;
    }

    @Override
    public boolean failed() {
      return failed;
    }

    @Override
    public boolean isCanceled() {
      return false;
    }

    @Override
    public void notifyOnCancel(RpcCallback<Object> arg) {
    }

    @Override
    public void reset() {
      failed = false;
      errorText = null;
    }

    @Override
    public void setFailed(String error) {
      failed = true;
      errorText = error;
    }

    @Override
    public void startCancel() {
    }
  };

  private int counter = 0;

  private FakeClientFrontendImpl frontend;

  private WaveClientRpcImpl rpcImpl;

  private final IdURIEncoderDecoder uriCodec = new IdURIEncoderDecoder(
      new URLEncoderDecoderBasedPercentEncoderDecoder());

  private WaveletName getWaveletName(String waveletName) {
    try {
      return uriCodec.uriToWaveletName(waveletName);
    } catch (EncodingException e) {
      return null;
    }
  }

  private String getWaveletUri(WaveletName waveletName) {
    try {
      return uriCodec.waveletNameToURI(waveletName);
    } catch (EncodingException e) {
      return null;
    }
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    counter = 0;
    controller.reset();
    frontend = new FakeClientFrontendImpl();
    rpcImpl = new WaveClientRpcImpl(frontend);
  }

  // TODO(arb): test with channelIds.
  
  /**
   * Tests that an open results in a proper wavelet commit update.
   */
  public void testOpenCommit() {
    ProtocolOpenRequest request = ProtocolOpenRequest.newBuilder()
        .setParticipantId(USER.getAddress())
        .setWaveId(WAVE_ID.serialise()).build();
    counter = 0;
    rpcImpl.open(controller, request, new RpcCallback<ProtocolWaveletUpdate>() {
      @Override
      public void run(ProtocolWaveletUpdate update) {
        ++counter;
        assertEquals(WAVELET_NAME, getWaveletName(update.getWaveletName()));
        assertTrue(update.hasCommitNotice());
        assertEquals(HASHED_VERSION, update.getCommitNotice());
      }
    });
    frontend.waveletCommitted(WAVELET_NAME, HASHED_VERSION);
    assertEquals(1, counter);
    assertFalse(controller.failed());
  }

  /**
   * Tests that an open failure results in a proper wavelet failure update.
   */
  public void testOpenFailure() {
    ProtocolOpenRequest request = ProtocolOpenRequest.newBuilder()
        .setParticipantId(USER.getAddress())
        .setWaveId(WAVE_ID.serialise()).build();
    counter = 0;
    rpcImpl.open(controller, request, new RpcCallback<ProtocolWaveletUpdate>() {
      @Override
      public void run(ProtocolWaveletUpdate update) {
        ++counter;
      }
    });
    frontend.doUpdateFailure(WAVE_ID, FAIL_MESSAGE);
    assertEquals(0, counter);
    assertTrue(controller.failed());
    assertEquals(FAIL_MESSAGE, controller.errorText());
  }

  /**
   * Tests that an open results in a proper wavelet update.
   */
  public void testOpenUpdate() {
    ProtocolOpenRequest request = ProtocolOpenRequest.newBuilder()
        .setParticipantId(USER.getAddress())
        .setWaveId(WAVE_ID.serialise()).build();
    counter = 0;
    rpcImpl.open(controller, request, new RpcCallback<ProtocolWaveletUpdate>() {
      @Override
      public void run(ProtocolWaveletUpdate update) {
        ++counter;
        assertEquals(WAVELET_NAME, getWaveletName(update.getWaveletName()));
        assertEquals(DELTAS.size(), update.getAppliedDeltaCount());
        for (int i = 0; i < update.getAppliedDeltaCount(); ++i) {
          assertEquals(DELTAS.get(i), update.getAppliedDelta(i));
        }
        assertFalse(update.hasCommitNotice());
      }
    });
    Map<String, BufferedDocOp> documentState = ImmutableMap.of();
    frontend.waveletUpdate(WAVELET_NAME, DELTAS, RESULTING_VERSION, documentState,
                           null /* channelId */);
    assertEquals(1, counter);
    assertFalse(controller.failed());
  }

  /**
   * Tests that a failed submit results in the proper submit failure response.
   */
  public void testSubmitFailed() {
    ProtocolSubmitRequest request = ProtocolSubmitRequest.newBuilder()
      .setDelta(DELTA)
      .setWaveletName(getWaveletUri(WAVELET_NAME)).build();
    counter = 0;
    rpcImpl.submit(controller, request, new RpcCallback<ProtocolSubmitResponse>() {
      @Override
      public void run(ProtocolSubmitResponse response) {
        ++counter;
        assertEquals(0, response.getOperationsApplied());
        assertEquals(FAIL_MESSAGE, response.getErrorMessage());
      }
    });
    frontend.doSubmitFailed(WAVELET_NAME);
    assertEquals(1, counter);
    assertFalse(controller.failed());
  }

  /**
   * Tests that a successful submit results in the proper submit response.
   */
  public void testSubmitSuccess() {
    ProtocolSubmitRequest request = ProtocolSubmitRequest.newBuilder()
      .setDelta(DELTA)
      .setWaveletName(getWaveletUri(WAVELET_NAME)).build();
    counter = 0;
    rpcImpl.submit(controller, request, new RpcCallback<ProtocolSubmitResponse>() {
      @Override
      public void run(ProtocolSubmitResponse response) {
        ++counter;
        assertEquals(1, response.getOperationsApplied());
        assertFalse(response.hasErrorMessage());
      }
    });
    frontend.doSubmitSuccess(WAVELET_NAME);
    assertEquals(1, counter);
    assertFalse(controller.failed());
  }

  /**
   * Tests that a bad wave id request is gracefully handled.
   */
  public void testOpenEncodingError() {
    ProtocolOpenRequest request = ProtocolOpenRequest.newBuilder()
        .setParticipantId(USER.getAddress())
        .setWaveId("badwaveid").build();
    counter = 0;
    try {
      rpcImpl.open(controller, request, new RpcCallback<ProtocolWaveletUpdate>() {
        @Override
        public void run(ProtocolWaveletUpdate update) {
          ++counter;
        }
      });
    } catch (IllegalArgumentException e) {
      controller.setFailed(FAIL_MESSAGE);
    }
    assertEquals(0, counter);
    assertTrue(controller.failed());
    assertFalse(controller.errorText().isEmpty());
  }

  /**
   * Tests that a bad wavelet name submit is gracefully handled.
   */
  public void testSubmitEncodingError() {
    ProtocolSubmitRequest request = ProtocolSubmitRequest.newBuilder()
      .setDelta(DELTA)
      .setWaveletName("badwaveletname").build();
    counter = 0;
    try {
      rpcImpl.submit(controller, request, new RpcCallback<ProtocolSubmitResponse>() {
        @Override
        public void run(ProtocolSubmitResponse response) {
          ++counter;
          assertTrue(response.hasErrorMessage());
        }
      });
    } catch (IllegalArgumentException e) {
      controller.setFailed(FAIL_MESSAGE);
    }
    assertEquals(0, counter);
    assertTrue(controller.failed());
    assertFalse(controller.errorText().isEmpty());
  }
}
