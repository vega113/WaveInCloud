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

package org.waveprotocol.box.server.robots;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.JsonRpcResponse;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.data.converter.EventDataConverter;
import com.google.wave.api.event.Event;
import com.google.wave.api.event.EventSerializationException;
import com.google.wave.api.event.EventSerializer;
import com.google.wave.api.event.EventType;
import com.google.wave.api.event.OperationErrorEvent;

import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.box.server.common.SnapshotSerializer;
import org.waveprotocol.box.server.frontend.WaveletSnapshotAndVersion;
import org.waveprotocol.box.server.robots.util.ConversationUtil;
import org.waveprotocol.box.server.robots.util.OperationUtil;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.box.server.waveserver.WaveServerException;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ObservableConversationView;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;
import org.waveprotocol.wave.util.logging.Log;

import java.util.Collections;
import java.util.Map;

/**
 * Class which provides context for robot operations and gives access to the
 * results.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class OperationContextImpl implements OperationContext, OperationResults {

  private static final Log LOG = Log.get(OperationContextImpl.class);

  /**
   * Maps operation ID's to responses.
   */
  private final Map<String, JsonRpcResponse> responses = Maps.newHashMap();

  /**
   * {@link WaveletProvider} that gives us access to wavelets.
   */
  private final WaveletProvider waveletProvider;

  /**
   * {@link EventDataConverter} that can convert to
   * {@link com.google.wave.api.impl.WaveletData} and such.
   */
  private final EventDataConverter converter;

  /**
   * The wavelet to which this context is bound, null if unbound.
   */
  private final RobotWaveletData boundWavelet;

  /**
   * The wavelets that have been opened in the lifespan of this context.
   */
  private final Map<WaveletName, RobotWaveletData> openedWavelets = Maps.newHashMap();

  /** Stores temporary blip ids -> real blip ids */
  private final Map<String, String> tempBlipIdMap = Maps.newHashMap();
  /** Stores temporary wavelet names -> real wavelet names */
  private final Map<WaveletName, WaveletName> tempWaveletNameMap = Maps.newHashMap();
  /** Caches {@link ObservableConversationView}s */
  private final Map<WaveletName, Map<ParticipantId, ObservableConversationView>>
      openedConversations;

  /** Used to create conversations. */
  private final ConversationUtil conversationUtil;

  /**
   * Constructs an operation context not bound to any wavelet.
   *
   * @param waveletProvider the waveletprovider to use for querying wavelet.
   * @param converter {@link EventDataConverter} for converting from server side
   *        objects.
   * @param conversationUtil used to create conversations.
   */
  public OperationContextImpl(WaveletProvider waveletProvider, EventDataConverter converter,
      ConversationUtil conversationUtil) {
    this(waveletProvider, converter, conversationUtil, null);
  }

  /**
   * Constructs a bound operation context. The bound wavelet is added to the
   * list of open wavelets.
   *
   * @param waveletProvider the waveletprovider to use for querying wavelet.
   * @param converter {@link EventDataConverter} for converting from server side
   *        objects.
   * @param boundWavelet the wavelet to bind this context to, null for an
   *        unbound context.
   * @param conversationUtil used to create conversations.
   */
  public OperationContextImpl(WaveletProvider waveletProvider, EventDataConverter converter,
      ConversationUtil conversationUtil, RobotWaveletData boundWavelet) {
    this.waveletProvider = waveletProvider;
    this.converter = converter;
    this.conversationUtil = conversationUtil;
    this.boundWavelet = boundWavelet;
    this.openedConversations = Maps.newHashMap();

    if (boundWavelet != null) {
      openedWavelets.put(boundWavelet.getWaveletName(), boundWavelet);
    }
  }

  // OperationContext implementation begins here

  @Override
  public EventDataConverter getConverter() {
    return converter;
  }

  @Override
  public boolean isBound() {
    return boundWavelet != null;
  }

  @Override
  public Map<WaveletName, RobotWaveletData> getOpenWavelets() {
    return Collections.unmodifiableMap(openedWavelets);
  }

  @Override
  public void constructResponse(OperationRequest operation, Map<ParamsProperty, Object> data) {
    setResponse(operation.getId(), JsonRpcResponse.result(operation.getId(), data));
  }

  @Override
  public void constructErrorResponse(OperationRequest operation, String errorMessage) {
    setResponse(operation.getId(), JsonRpcResponse.error(operation.getId(), errorMessage));
  }

  @Override
  public void processEvent(OperationRequest operation, Event event) throws InvalidRequestException {
    // Create JSON-RPC error response.
    if (event.getType() == EventType.OPERATION_ERROR) {
      constructErrorResponse(operation, OperationErrorEvent.as(event).getMessage());
      return;
    }
    // Create JSON-RPC success response.
    try {
      constructResponse(operation, EventSerializer.extractPropertiesToParamsPropertyMap(event));
    } catch (EventSerializationException e) {
      LOG.severe("Internal Error occurred, when serializing events", e);
      throw new InvalidRequestException("Unable to serialize events", operation);
    }
  }

  @Override
  public void putWavelet(String waveId, String waveletId, RobotWaveletData newWavelet) {
    WaveletName waveletName = newWavelet.getWaveletName();
    Preconditions.checkArgument(!openedWavelets.containsKey(waveletName),
        "Not allowed to put an already open wavelet in as a new wavelet");

    // New wavelets are indicated by the temporary marker in their waveId.
    if (waveId.contains("!" + TEMP_ID_MARKER)) {
      tempWaveletNameMap.put(WaveletName.of(waveId, waveletId), waveletName);
    }
    openedWavelets.put(waveletName, newWavelet);
  }

  @Override
  public OpBasedWavelet openWavelet(String waveId, String waveletId, ParticipantId participant)
      throws InvalidRequestException {
    WaveletName waveletName;
    if (waveId.contains("!" + TEMP_ID_MARKER)) {
      WaveletName tempWaveletName = WaveletName.of(waveId, waveletId);
      waveletName = tempWaveletNameMap.get(tempWaveletName);
    } else {
      waveletName = WaveletName.of(waveId, waveletId);
    }

    RobotWaveletData wavelet = openedWavelets.get(waveletName);
    if (wavelet == null) {
      // Open a wavelet from the server
      WaveletSnapshotAndVersion snapshot;
      try {
        snapshot = waveletProvider.getSnapshot(waveletName);
      } catch (WaveServerException e) {
        LOG.severe("Wave server failure retrieving snapshot for wavelet " + waveletName);
        throw new InvalidRequestException("Wavelet " + waveletName + " couldn't be retrieved");
      }

      if (snapshot == null) {
        throw new InvalidRequestException("Wavelet " + waveletName + " couldn't be retrieved");
      }

      // Note: The error messages relayed to the client don't give away that the
      // wavelet actually exists.
      ObservableWaveletData obsWavelet;
      try {
        obsWavelet = SnapshotSerializer.deserializeWavelet(snapshot.snapshot, waveletName.waveId);
      } catch (OperationException e) {
        LOG.severe("Unable to parse retrieved snapshot", e);
        throw new InvalidRequestException("Wavelet " + waveletName + " couldn't be retrieved");
      } catch (InvalidParticipantAddress e) {
        LOG.severe("Found invalid participant when parsing a snapshot", e);
        throw new InvalidRequestException("Wavelet " + waveletName + " couldn't be retrieved");
      }
      HashedVersion committedVersion =
          CoreWaveletOperationSerializer.deserialize(snapshot.committedVersion);

      if (!obsWavelet.getParticipants().contains(participant)) {
        LOG.severe(
            participant + " tried to open " + waveletName + " which it isn't participating in");
        throw new InvalidRequestException("Wavelet " + waveletName + " couldn't be retrieved");
      }
      wavelet = new RobotWaveletData(obsWavelet, committedVersion);
      openedWavelets.put(waveletName, wavelet);
    }
    return wavelet.getOpBasedWavelet(participant);
  }

  @Override
  public OpBasedWavelet openWavelet(OperationRequest operation, ParticipantId participant)
      throws InvalidRequestException {
    String waveId = OperationUtil.getRequiredParameter(operation, ParamsProperty.WAVE_ID);
    String waveletId = OperationUtil.getRequiredParameter(operation, ParamsProperty.WAVELET_ID);
    return openWavelet(waveId, waveletId, participant);
  }

  @Override
  public ObservableConversationView openConversation(
      String waveId, String waveletId, ParticipantId participant) throws InvalidRequestException {
    WaveletName waveletName = WaveletName.of(waveId, waveletId);

    if (!openedConversations.containsKey(waveletName)) {
      openedConversations.put(
          waveletName, Maps.<ParticipantId, ObservableConversationView> newHashMap());
    }

    Map<ParticipantId, ObservableConversationView> conversations =
        openedConversations.get(waveletName);

    if (!conversations.containsKey(participant)) {
      OpBasedWavelet wavelet = openWavelet(waveId, waveletId, participant);
      conversations.put(participant, conversationUtil.buildConversation(wavelet));
    }
    return conversations.get(participant);
  }

  @Override
  public ObservableConversationView openConversation(
      OperationRequest operation, ParticipantId participant) throws InvalidRequestException {
    String waveId = OperationUtil.getRequiredParameter(operation, ParamsProperty.WAVE_ID);
    String waveletId = OperationUtil.getRequiredParameter(operation, ParamsProperty.WAVELET_ID);
    return openConversation(waveId, waveletId, participant);
  }

  // OperationResults implementation begins here

  @Override
  public void putBlip(String blipId, ConversationBlip newBlip) {
    if (blipId.startsWith(TEMP_ID_MARKER)) {
      tempBlipIdMap.put(blipId, newBlip.getId());
    }
  }

  @Override
  public ConversationBlip getBlip(Conversation conversation, String blipId)
      throws InvalidRequestException {
    // We might need to look up the blip id for new blips.
    String actualBlipId = blipId.startsWith(TEMP_ID_MARKER) ? tempBlipIdMap.get(blipId) : blipId;

    ConversationBlip blip = conversation.getBlip(actualBlipId);
    if (blip == null || blip.isDeleted()) {
      throw new InvalidRequestException(
          "Blip with id " + blipId + " does not exist or has been deleted");
    }

    return blip;
  }

  // OperationResults implementation begins here

  @Override
  public ConversationUtil getConversationUtil() {
    return conversationUtil;
  }

  @Override
  public Map<String, JsonRpcResponse> getResponses() {
    return Collections.unmodifiableMap(responses);
  }

  @Override
  public JsonRpcResponse getResponse(String operationId) {
    return responses.get(operationId);
  }

  @Override
  public boolean hasResponse(String operationId) {
    return responses.containsKey(operationId);
  }

  /**
   * Stores a response in this context.
   *
   * @param operationId the id of the robot operation.
   * @param response the response to store.
   */
  private void setResponse(String operationId, JsonRpcResponse response) {
    Preconditions.checkState(
        !responses.containsKey(operationId), "Overwriting an existing response");
    responses.put(operationId, response);
  }
}
