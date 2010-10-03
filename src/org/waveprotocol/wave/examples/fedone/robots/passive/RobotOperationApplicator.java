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

package org.waveprotocol.wave.examples.fedone.robots.passive;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.ProtocolVersion;
import com.google.wave.api.data.converter.EventDataConverterManager;
import com.google.wave.api.event.EventType;
import com.google.wave.api.event.OperationErrorEvent;
import com.google.wave.api.robot.RobotName;

import org.waveprotocol.wave.examples.common.HashedVersion;
import org.waveprotocol.wave.examples.fedone.account.RobotAccountData;
import org.waveprotocol.wave.examples.fedone.robots.OperationContext;
import org.waveprotocol.wave.examples.fedone.robots.OperationContextImpl;
import org.waveprotocol.wave.examples.fedone.robots.OperationResults;
import org.waveprotocol.wave.examples.fedone.robots.OperationServiceRegistry;
import org.waveprotocol.wave.examples.fedone.robots.RobotWaveletData;
import org.waveprotocol.wave.examples.fedone.robots.operations.OperationService;
import org.waveprotocol.wave.examples.fedone.robots.util.ConversationUtil;
import org.waveprotocol.wave.examples.fedone.robots.util.LoggingRequestListener;
import org.waveprotocol.wave.examples.fedone.robots.util.OperationUtil;
import org.waveprotocol.wave.examples.fedone.util.Log;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveletProvider;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;

import java.util.List;

/**
 * This class is responsible for applying operations received from a Robot in
 * the Passive API. Operations that fail will result in an
 * {@link OperationErrorEvent} being generated and they might be sent off to the
 * robot after all of the robot's operations have been completed.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class RobotOperationApplicator {

  private static final Log LOG = Log.get(RobotOperationApplicator.class);

  private final static WaveletProvider.SubmitRequestListener LOGGING_REQUEST_LISTENER =
      new LoggingRequestListener(LOG);

  private final EventDataConverterManager converterManager;
  private final WaveletProvider waveletProvider;
  private final OperationServiceRegistry operationRegistry;
  private final ConversationUtil conversationUtil;

  /**
   * Constructs a new {@link RobotOperationApplicator}.
   *
   * @param converterManager used to convert to Robot API objects
   * @param waveletProvider used to retrieve wavelets and submit deltas
   * @param operationRegistry registry containing the {@link OperationService}s
   *        that this applicator can perform.
   * @param conversationUtil used to create conversations.
   */
  public RobotOperationApplicator(EventDataConverterManager converterManager,
      WaveletProvider waveletProvider, OperationServiceRegistry operationRegistry,
      ConversationUtil conversationUtil) {
    this.converterManager = converterManager;
    this.waveletProvider = waveletProvider;
    this.operationRegistry = operationRegistry;
    this.conversationUtil = conversationUtil;
  }

  /**
   * Applies the operations within the context of the wavelet.
   *
   * @param operations the operations to apply
   * @param wavelet the wavelet which is the context in which this operation is
   *        performed.
   * @param hashedVersion the version of the wavelet to which to apply the
   *        operations to.
   * @param account the account for which to apply robot operations.
   */
  void applyOperations(List<OperationRequest> operations, ReadableWaveletData wavelet,
      HashedVersion hashedVersion, RobotAccountData account) {
    // The robots we support should be sending us their version in their first
    // operation
    ProtocolVersion protocolVersion = OperationUtil.getProtocolVersion(operations);

    OperationContextImpl context = new OperationContextImpl(waveletProvider,
        converterManager.getEventDataConverter(protocolVersion), conversationUtil,
        new RobotWaveletData(wavelet, hashedVersion));

    executeOperations(context, operations, account);
    handleResults(context, account);
  }

  /**
   * Executes operations in the given context.
   *
   * @param context the context to perform the operations in.
   * @param operations the operations to perform.
   * @param account the account for which to execute robot operations.
   */
  private void executeOperations(
      OperationContext context, List<OperationRequest> operations, RobotAccountData account) {
    for (OperationRequest operation : operations) {
      // Get the operation of the author taking into account the proxying for
      // field.
      ParticipantId author;
      try {
        author = getOperationAuthor(operation, account);
      } catch (InvalidParticipantAddress e) {
        LOG.warning("Error has occurred when constructing the author for an operation", e);
        context.constructErrorResponse(operation, e.getMessage());
        continue;
      }
      OperationUtil.executeOperation(operation, operationRegistry, context, author);
    }
  }

  /**
   * Handles an {@link OperationResults} by submitting the deltas it generates
   * and sending off any events to the robot. Note that currently no events are
   * send off to the robot.
   *
   * @param results the results of the operations performed
   * @param account the account for which to handle results of robot operations.
   */
  private void handleResults(OperationResults results, RobotAccountData account) {
    OperationUtil.submitDeltas(results, waveletProvider, LOGGING_REQUEST_LISTENER);

    // TODO(ljvderijk): In theory we should be sending off all events that are
    // generated by the operations. Currently not done in production. We should
    // make it possible though.
    boolean notifyOnError =
        account.getCapabilities().getCapabilitiesMap().containsKey(EventType.OPERATION_ERROR);
  }

  /**
   * Returns a {@link ParticipantId} for which the given operation is to be
   * performed. It takes into account the possibility that this operation is
   * being proxied for another user on behalf of the robot.
   *
   * @param operation the operation to be performed
   * @param account the account of the robot wanting to perform the action
   * @throws InvalidParticipantAddress if the participant id can not be
   *         constructed
   */
  @VisibleForTesting
  ParticipantId getOperationAuthor(OperationRequest operation, RobotAccountData account)
      throws InvalidParticipantAddress {
    // NOTE(ljvderijk): Operations with proxying for set will likely fail due to
    // operations being applied for unknown participants. API's seem to send add
    // participant operations for them, check if proxying works once those
    // operations are implemented.
    // Python API defines proxying for in Event, Java does not. Seems not to be
    // handled properly, they need to be updated.
    String proxy = (String) operation.getParameter(ParamsProperty.PROXYING_FOR);
    RobotName robotName = RobotName.fromAddress(account.getId().getAddress());
    if (!Strings.isNullOrEmpty(proxy)) {
      robotName.setProxyFor(proxy);
      String robotAddress = robotName.toParticipantAddress();
      if (!RobotName.isWellFormedAddress(robotAddress)) {
        throw new InvalidParticipantAddress(
            robotAddress, "is not a valid robot name, the proxy is likely to be wrong");
      }
    }
    return ParticipantId.of(robotName.toParticipantAddress());
  }
}
