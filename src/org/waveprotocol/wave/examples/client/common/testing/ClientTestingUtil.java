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

package org.waveprotocol.wave.examples.client.common.testing;

import static junit.framework.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import static org.waveprotocol.wave.examples.fedone.common.DocumentConstants.MANIFEST_DOCUMENT_ID;
import static org.waveprotocol.wave.examples.fedone.util.testing.TestingConstants.TEST_TIMEOUT;
import static org.waveprotocol.wave.examples.fedone.util.testing.TestingConstants.WAVELET_NAME;

import com.google.common.collect.Lists;

import org.waveprotocol.wave.examples.client.common.ClientBackend;
import org.waveprotocol.wave.examples.client.common.ClientUtils;
import org.waveprotocol.wave.examples.client.common.ClientWaveView;
import org.waveprotocol.wave.examples.client.console.ConsoleClient;
import org.waveprotocol.wave.examples.fedone.util.BlockingSuccessFailCallback;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc.ProtocolSubmitResponse;
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.core.CoreWaveletDocumentOperation;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.core.CoreWaveletData;
import org.waveprotocol.wave.model.wave.data.core.impl.CoreWaveletDataImpl;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for tesing the client and related classes.
 *
 * @author mk.mateng@gmail.com (Michael Kuntzman)
 */
public class ClientTestingUtil {
  /**
   * ClientBackend factory that creates a spy object on the backend and injects a fake RPC
   * objects factory.
   */
  public static final ClientBackend.Factory backendSpyFactory = new ClientBackend.Factory() {
      @Override
      public ClientBackend create(String userAtDomain, String server, int port)
          throws IOException {
        return spy(new ClientBackend(userAtDomain, server, port, new FakeRpcObjectFactory()));
      }
    };

  /**
   * @return a new mock renderer for the console client. The renderer skips rendering to speed up
   * the tests and allows checking if the render method was called.
   */
  // TODO(Michael): Add checks to the ConsoleClientTest to check that render is being called when
  // appropriate.
  public static ConsoleClient.Renderer getMockConsoleRenderer() {
    return mock(ConsoleClient.Renderer.class);
  }


  /** The client backend on which this util instance acts. */
  private final ClientBackend backend;


  /**
   * Constructs a {@code ClientTestingUtil} that acts on the given client backend.
   *
   * @param backend to act on.
   */
  // TODO: Derive the console client and agents from a common base class, then have this util act
  // on that base class rather than on the backend.
  public ClientTestingUtil(ClientBackend backend) {
    this.backend = backend;
  }

  /**
   * Verifies that an operation completed without errors within the time set by the test timeout.
   *
   * @param callback the blocking callback that was used for the operation.
   */
  public void assertOperationComplete(
      BlockingSuccessFailCallback<ProtocolSubmitResponse, String> callback) {
    // Make sure the test times out if something is wrong.
    final long waitTimeout = TEST_TIMEOUT * 2;
    final TimeUnit waitUnit = TimeUnit.MILLISECONDS;

    Pair<ProtocolSubmitResponse, String> result = callback.await(waitTimeout, waitUnit);
    // Process any incoming events that may have been generated.
    backend.waitForAccumulatedEventsToProcess();

    assertNotNull(result);
    assertNotNull(result.getFirst());
  }

  /**
   * Creates a new empty wavelet with an empty manifest document using
   * {@code TestingConstants.WAVELET_NAME} for the wavelet's name. The wavelet is not part of a
   * {@code ClientWaveView} and not stored in the client backend.
   *
   * @return the new wavelet
   */
  public CoreWaveletData createWavelet() throws OperationException {
    return createWavelet(WAVELET_NAME);
  }

  /**
   * Creates a new empty wavelet with an empty manifest document and the specified wavelet name.
   * The wavelet is not part of a {@code ClientWaveView} and not stored in the client backend.
   *
   * @param waveletName of the new wavelet.
   * @return the new wavelet
   */
  public CoreWaveletData createWavelet(WaveletName waveletName) throws OperationException {
    CoreWaveletData wavelet = new CoreWaveletDataImpl(waveletName.waveId, waveletName.waveletId);
    wavelet.modifyDocument(MANIFEST_DOCUMENT_ID, ClientUtils.createManifest());
    return wavelet;
  }

  /**
   * Creates a valid wave (and wavelet) in the client backend.
   *
   * @return the new wave's conversation root wavelet.
   */
  public CoreWaveletData createWaveletInBackend() {
    BlockingSuccessFailCallback<ProtocolSubmitResponse, String> callback =
        BlockingSuccessFailCallback.create();
    CoreWaveletData wavelet = ClientUtils.getConversationRoot(backend.createConversationWave(
        callback));
    // Make sure the wavelet creation completes successfully before returning the wavelet.
    assertOperationComplete(callback);
    return wavelet;
  }

  /**
   * Returns all documents in the wave, aggregated from all the wavelets.
   *
   * @param wave to get the documents from.
   * @return map of all documents in the wave, aggregated from all the wavelets, and keyed by their
   * IDs.
   */
  public Map<String, BufferedDocOp> getAllDocuments(ClientWaveView wave) {
    return ClientUtils.getAllDocuments(wave);
  }

  /**
   * Returns all documents in the wave, aggregated from all the wavelets. The wave is retrieved
   * from the client backend using the given wave ID.
   *
   * @param waveId of the wave to get the documents from.
   * @return map of all documents in the wave, aggregated from all the wavelets, and keyed by their
   * IDs.
   */
  public Map<String, BufferedDocOp> getAllDocuments(WaveId waveId) {
    return getAllDocuments(backend.getWave(waveId));
  }

  /**
   * Returns all participants in the wave, aggregated from all the wavelets.
   *
   * @param wave to get the participants from.
   * @return all participants in the wave, aggregated from all the wavelets.
   */
  public Set<ParticipantId> getAllParticipants(ClientWaveView wave) {
    return ClientUtils.getAllParticipants(wave);
  }

  /**
   * Returns all participants in the wave, aggregated from all the wavelets. The wave is retrieved
   * from the client backend using the given wave ID.
   *
   * @param waveId of the wave to get the participants from.
   * @return all participants in the wave, aggregated from all the wavelets.
   */
  public Set<ParticipantId> getAllParticipants(WaveId waveId) {
    return getAllParticipants(backend.getWave(waveId));
  }

  /**
   * @return the first open wave in the client backend, not counting the index wave.
   */
  public ClientWaveView getFirstWave() {
    return backend.getWave(getFirstWaveId());
  }

  /**
   * @return the WaveId of the first open wave in the client backend, not counting the index wave.
   */
  public WaveId getFirstWaveId() {
    return getOpenWaveId(0, false);
  }

  /**
   * Returns the WaveId of the n-th open wave in the client backend.
   *
   * @param index of the open wave whose id to retreive (zero based).
   * @param includingIndexWave should the index wave be included in the count of open waves?
   * @return the WaveId, or null if not found.
   */
  public WaveId getOpenWaveId(int index, boolean includingIndexWave) {
    for (WaveId waveId : getOpenWaveIds(includingIndexWave)) {
      if (index == 0) {
        return waveId;
      }
      --index;
    }

    // Wave not found (index is out of range).
    return null;
  }

  /**
   * Returns the set of wave IDs of the waves that are currently open in the client backend,
   * optionally including the index wave.
   *
   * @param includeIndexWave should the index wave be included in the returned set?
   * @return the set of wave Ids of the open waves.
   */
  public Set<WaveId> getOpenWaveIds(boolean includeIndexWave) {
    return backend.getOpenWaveIds(includeIndexWave);
  }

  /**
   * Counts the waves curretly open in the client backend.
   *
   * @param includeIndexWave should the index wave be included in the count?
   * @return the number of waves currently open in the client backend.
   */
  public int getOpenWavesCount(boolean includeIndexWave) {
    return getOpenWaveIds(includeIndexWave).size();
  }

  /**
   * Collects the text from the specified blip document.
   *
   * @param blip document to collect the text from.
   * @return A string containing the characters from the blip.
   */
  public static String getText(BufferedDocOp blip) {
    return ClientUtils.collateText(Lists.newArrayList(blip));
  }

  /**
   * Collates the specified document operations into a string equivalent to the resulting wavelet
   * content.
   *
   * @param ops to collate.
   * @return the resulting text content.
   */
  public String getText(List<CoreWaveletDocumentOperation> ops) {
    List<BufferedDocOp> docs = Lists.newArrayList();
    for (CoreWaveletDocumentOperation op : ops) {
      // Skip changes to the manifest document since they may contain "retain" and other components
      // that can't be collated by ClientUtils, and we don't really care about the manifest anyway.
      if (!op.getDocumentId().equals(MANIFEST_DOCUMENT_ID)) {
        docs.add(op.getOperation());
      }
    }
    return ClientUtils.collateText(docs);
  }

  /**
   * Collects the text of all of the documents in a wave into a single String.
   *
   * @param wave wave to collect the text from.
   * @return the collected text from the wave.
   */
  public String getText(ClientWaveView wave) {
    return ClientUtils.collateText(wave);
  }

  /**
   * Collects the text of all of the documents in a wave into a single String. The wave is
   * retrieved from the client backend using the given wave ID.
   *
   * @param waveId of the wave to collect the text from.
   * @return the collected text from the wave.
   */
  public String getText(WaveId waveId) {
    return getText(backend.getWave(waveId));
  }
}
