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

package org.waveprotocol.box.server.robots.active;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.JsonRpcResponse;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.ProtocolVersion;
import com.google.wave.api.RobotSerializer;
import com.google.wave.api.data.converter.EventDataConverterManager;
import com.google.wave.api.impl.GsonFactory;

import net.oauth.OAuth;
import net.oauth.OAuthAccessor;
import net.oauth.OAuthConsumer;
import net.oauth.OAuthException;
import net.oauth.OAuthMessage;
import net.oauth.OAuthServiceProvider;
import net.oauth.OAuthValidator;
import net.oauth.server.HttpRequestMessage;

import org.waveprotocol.box.server.account.AccountData;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.robots.OperationContext;
import org.waveprotocol.box.server.robots.OperationContextImpl;
import org.waveprotocol.box.server.robots.OperationResults;
import org.waveprotocol.box.server.robots.OperationServiceRegistry;
import org.waveprotocol.box.server.robots.util.ConversationUtil;
import org.waveprotocol.box.server.robots.util.LoggingRequestListener;
import org.waveprotocol.box.server.robots.util.OperationUtil;
import org.waveprotocol.box.server.util.Log;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * {@link HttpServlet} that serves as the endpoint for the Active Api.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class ActiveApiServlet extends HttpServlet {

  private static final Log LOG = Log.get(ActiveApiServlet.class);
  private static final WaveletProvider.SubmitRequestListener LOGGING_REQUEST_LISTENER =
      new LoggingRequestListener(LOG);
  private static final String JSON_CONTENT_TYPE = "application/json";

  private final RobotSerializer robotSerializer;
  private final EventDataConverterManager converterManager;
  private final WaveletProvider waveletProvider;
  private final OperationServiceRegistry operationRegistry;
  private final ConversationUtil conversationUtil;
  private final OAuthValidator validator;
  private final OAuthServiceProvider oauthServiceProvider;
  private final AccountStore accountStore;

  @Inject
  public ActiveApiServlet(RobotSerializer robotSerializer,
      EventDataConverterManager converterManager, WaveletProvider waveletProvider,
      @Named("ActiveApiRegistry") OperationServiceRegistry operationRegistry,
      ConversationUtil conversationUtil, OAuthServiceProvider oAuthServiceProvider,
      OAuthValidator validator, AccountStore accountStore) {
    this.robotSerializer = robotSerializer;
    this.converterManager = converterManager;
    this.waveletProvider = waveletProvider;
    this.conversationUtil = conversationUtil;
    this.operationRegistry = operationRegistry;
    this.validator = validator;
    this.oauthServiceProvider = oAuthServiceProvider;
    this.accountStore = accountStore;
  }

  /**
   * Entry point for the Active Api Calls.
   */
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    OAuthMessage message = new HttpRequestMessage(req, req.getRequestURL().toString());
    // OAuth %-escapes the @ in the username so we need to decode it.
    String username = OAuth.decodePercent(message.getConsumerKey());

    ParticipantId participant;
    try {
      participant = ParticipantId.of(username);
    } catch (InvalidParticipantAddress e) {
      LOG.info("Participant id invalid", e);
      resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }

    AccountData account = accountStore.getAccount(participant);
    if (account == null || !account.isRobot()) {
      LOG.info("The account for robot named " + participant + " does not exist");
      resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }

    OAuthConsumer consumer =
        new OAuthConsumer(null, participant.getAddress(), account.asRobot().getConsumerSecret(),
            oauthServiceProvider);
    OAuthAccessor accessor = new OAuthAccessor(consumer);

    try {
      validator.validateMessage(message, accessor);
    } catch (OAuthException e) {
      LOG.info("The message does not conform to OAuth", e);
      resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    } catch (URISyntaxException e) {
      LOG.info("The message URL is invalid", e);
      resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }

    String apiRequest;
    try {
      // message.readBodyAsString() doesn't work due to a NPE in the OAuth
      // libraries.
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

    List<JsonRpcResponse> responses = ImmutableList.copyOf(results.getResponses().values());
    String jsonResponse =
        robotSerializer.serialize(responses, GsonFactory.JSON_RPC_RESPONSE_LIST_TYPE, version);
    LOG.info("Returning the following Json: " + jsonResponse);

    // Write the response back through the HttpServlet
    try {
      resp.setContentType(JSON_CONTENT_TYPE);
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
