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

package org.waveprotocol.wave.examples.fedone.robots.dataapi;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.JsonRpcResponse;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.ProtocolVersion;
import com.google.wave.api.RobotSerializer;
import com.google.wave.api.data.converter.EventDataConverterManager;
import com.google.wave.api.impl.GsonFactory;

import org.waveprotocol.wave.examples.fedone.robots.OperationContext;
import org.waveprotocol.wave.examples.fedone.robots.OperationContextImpl;
import org.waveprotocol.wave.examples.fedone.robots.OperationResults;
import org.waveprotocol.wave.examples.fedone.robots.OperationServiceRegistry;
import org.waveprotocol.wave.examples.fedone.robots.util.ConversationUtil;
import org.waveprotocol.wave.examples.fedone.robots.util.LoggingRequestListener;
import org.waveprotocol.wave.examples.fedone.robots.util.OperationUtil;
import org.waveprotocol.wave.examples.fedone.util.Log;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveletProvider;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * {@link HttpServlet} that serves as the endpoint for the Data Api.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class DataApiServlet extends HttpServlet {

  private static final Log LOG = Log.get(DataApiServlet.class);
  private static final WaveletProvider.SubmitRequestListener LOGGING_REQUEST_LISTENER =
      new LoggingRequestListener(LOG);
  public static final String USERNAME_HEADER = "X-Wave-Username";

  private final RobotSerializer robotSerializer;
  private final EventDataConverterManager converterManager;
  private final WaveletProvider waveletProvider;
  private final OperationServiceRegistry operationRegistry;
  private final ConversationUtil conversationUtil;

  @Inject
  public DataApiServlet(RobotSerializer robotSerializer, EventDataConverterManager converterManager,
      WaveletProvider waveletProvider,
      @Named("DataApiRegistry") OperationServiceRegistry operationRegistry,
      ConversationUtil conversationUtil) {
    this.robotSerializer = robotSerializer;
    this.converterManager = converterManager;
    this.waveletProvider = waveletProvider;
    this.conversationUtil = conversationUtil;
    this.operationRegistry = operationRegistry;
  }

  /**
   * Entry point for the Data API Calls.
   */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    // TODO(ljvderijk): Remove this once proper authentication is up
    String username = req.getHeader(USERNAME_HEADER);
    LOG.info("Performing operations for: " + username);

    ParticipantId participant;
    try {
      participant = ParticipantId.of(username);
    } catch (InvalidParticipantAddress e) {
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
      return;
    }

    String apiRequest;
    try {
      BufferedReader reader = req.getReader();
      apiRequest = reader.readLine();
    } catch (IOException e) {
      LOG.warning("Unable to read the incoming request", e);
      throw e;
    }

    LOG.info("Received the following Json: " + apiRequest);
    List<OperationRequest> operations;
    try {
      operations = robotSerializer.deserializeOperations(apiRequest);
    } catch (InvalidRequestException e) {
      LOG.info("Unable to parse Json to list of OperationRequests: " + apiRequest);
      resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
          "Unable to parse Json to list of OperationRequests: " + apiRequest);
      return;
    }

    // Create an unbound context.
    ProtocolVersion version = OperationUtil.getProtocolVersion(operations);
    OperationContextImpl context = new OperationContextImpl(
        waveletProvider, converterManager.getEventDataConverter(version), conversationUtil);

    executeOperations(context, operations, participant);
    handleResults(context, resp, version);
  }

  /**
   * Executes operations in the given context.
   *
   * @param context the context to perform the operations in.
   * @param operations the operations to perform.
   * @param author the author for which to perform the robot operations.
   */
  private void executeOperations(
      OperationContext context, List<OperationRequest> operations, ParticipantId author) {
    for (OperationRequest operation : operations) {
      // Execute the request
      OperationUtil.executeOperation(operation, operationRegistry, context, author);
    }
  }

  /**
   * Handles an {@link OperationResults} by submitting the deltas that are
   * generated and writing a response to the robot.
   *
   * @param results the results of the operations performed.
   * @param resp the servlet to write the response in.
   * @param version the version of the protocol to use for writing a response.
   * @throws IOException if the response can not be written.
   */
  private void handleResults(
      OperationResults results, HttpServletResponse resp, ProtocolVersion version)
      throws IOException {
    OperationUtil.submitDeltas(results, waveletProvider, LOGGING_REQUEST_LISTENER);

    List<JsonRpcResponse> responses = Lists.newArrayList(results.getResponses().values());
    String jsonResponse =
        robotSerializer.serialize(responses, GsonFactory.JSON_RPC_RESPONSE_LIST_TYPE, version);
    LOG.info("Returning the following Json: " + jsonResponse);

    // Write the response back through the HttpServlet
    try {
      resp.setContentType("application/json");
      PrintWriter writer = resp.getWriter();
      writer.append(jsonResponse);
      writer.flush();
      resp.setStatus(HttpServletResponse.SC_OK);
    } catch (IOException e) {
      LOG.severe("IOException during writing of a response", e);
      throw e;
    }
  }
}
