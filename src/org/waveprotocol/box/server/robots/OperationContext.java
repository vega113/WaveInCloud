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

import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.data.converter.EventDataConverter;
import com.google.wave.api.event.Event;

import org.waveprotocol.box.server.robots.util.ConversationUtil;
import org.waveprotocol.wave.model.conversation.Conversation;
import org.waveprotocol.wave.model.conversation.ConversationBlip;
import org.waveprotocol.wave.model.conversation.ObservableConversationView;
import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;

import java.util.Map;

/**
 * Context for performing robot operations.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public interface OperationContext {

  /** Marks temporary wave and blip ID's since V2 */
  final String TEMP_ID_MARKER = "TBD_";

  /**
   * @return true iff this context is bound to a wavelet.
   */
  boolean isBound();

  /**
   * @param operationId the id of the robot operation.
   * @return True iff a response has been set for the given id.
   */
  boolean hasResponse(String operationId);

  /**
   * Constructs a response with the given data in its payload field.
   *
   * @param data the data to be put in the repsonse.
   */
  void constructResponse(OperationRequest operation, Map<ParamsProperty, Object> data);

  /**
   * Constructs and stores a response signifying an error to be put in the
   * context.
   *
   * @param errorMessage the error message to be put in the response.
   */
  void constructErrorResponse(OperationRequest operation, String errorMessage);

  /**
   * Processes the event and sets the proper response.
   *
   * @param event the event to process.
   * @throws InvalidRequestException If the event could not be properly
   *         processed.
   */
  void processEvent(OperationRequest operation, Event event) throws InvalidRequestException;

  /**
   * Stores a reference from a temporary wavelet id to a real wavelet id. If the
   * given id is not a temporary id no reference will be stored.
   *
   * @param waveId the wave id.
   * @param waveletId the wavelet id.
   * @param newWavelet the new wavelet to remember.
   */
  void putWavelet(String waveId, String waveletId, RobotWaveletData newWavelet);

  /**
   * Opens a wavelet for the given wave id and wavelet id.
   *
   * @param waveId the wave id of the wavelet to open.
   * @param waveletId the wavelet id of the wavelet to open.
   * @param participant the id of the participant that wants to open the wavelet.
   * @throws InvalidRequestException if the wavelet can not be opened.
   */
  OpBasedWavelet openWavelet(String waveId, String waveletId, ParticipantId participant)
      throws InvalidRequestException;

  /**
   * Creates a conversation for the given wavelet. The wavelet must be a valid
   * conversational wavelet.
   *
   * @param wavelet the wavelet to construct a conversation for.
   */
  ObservableConversationView getConversation(ObservableWavelet wavelet);

  /**
   * Stores a reference from a temporary blip id to a real blip id. If the given
   * id is not a temporary id it will be ignored.
   *
   * @param blipId the temporary blip id.
   * @param newBlip the blip that this id should reference.
   */
  void putBlip(String blipId, ConversationBlip newBlip);

  /**
   * Retrieve a blip with the given, possible temporary id, from the
   * conversation.
   *
   * @param conversation the conversation the blip belongs to.
   * @param blipId the id of the blip, maybe be a temporary id.
   */
  ConversationBlip getBlip(Conversation conversation, String blipId);

  /**
   * @return the converter to convert to API objects
   */
  EventDataConverter getConverter();

  /**
   * Returns the {@link OpBasedWavelet} as specified by the given operation.
   *
   * @param operation the operation which contains the wave and wavelet id.
   * @return {@link OpBasedWavelet} object for the wavelet specified in this
   *         context.
   * @throws InvalidRequestException if the wave could not be retrieved
   */
  OpBasedWavelet getWavelet(OperationRequest operation, ParticipantId participant)
      throws InvalidRequestException;

  /**
   * Returns {@link ConversationUtil} which is used to generate conversations and
   * ids.
   */
  ConversationUtil getConversationUtil();
}
