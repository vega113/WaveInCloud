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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.wave.api.RobotSerializer;
import com.google.wave.api.data.converter.EventDataConverterManager;
import com.google.wave.api.robot.CapabilityFetchException;
import com.google.wave.api.robot.RobotName;

import org.waveprotocol.wave.examples.fedone.account.AccountData;
import org.waveprotocol.wave.examples.fedone.account.RobotAccountData;
import org.waveprotocol.wave.examples.fedone.common.VersionedWaveletDelta;
import org.waveprotocol.wave.examples.fedone.persistence.AccountStore;
import org.waveprotocol.wave.examples.fedone.robots.util.ConversationUtil;
import org.waveprotocol.wave.examples.fedone.util.Log;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveBus;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveletProvider;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Gateway for the Passive Robot API, this class can be subscribed to the
 * WaveBus and fires of separate threads to handle any updates for Robots.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class RobotsGateway implements WaveBus.Subscriber {

  private static final Log LOG = Log.get(RobotsGateway.class);

  private final WaveletProvider waveletProvider;
  private final AccountStore accountStore;
  private final EventDataConverterManager converterManager;
  private final RobotConnector connector;
  private final Map<RobotName, Robot> allRobots = Maps.newHashMap();
  private final Set<RobotName> runnableRobots = Sets.newHashSet();
  private final Executor executor;
  private final ConversationUtil conversationUtil;

  @Inject
  @VisibleForTesting
  RobotsGateway(WaveletProvider waveletProvider, RobotConnector connector,
      AccountStore accountStore, RobotSerializer serializer,
      EventDataConverterManager converterManager, @Named("GatewayExecutor") Executor executor,
      ConversationUtil conversationUtil) {
    this.waveletProvider = waveletProvider;
    this.accountStore = accountStore;
    this.converterManager = converterManager;
    this.connector = connector;
    this.executor = executor;
    this.conversationUtil = conversationUtil;
  }

  @Override
  public void waveletCommitted(WaveletName waveletName, HashedVersion version) {
    // We ignore this event
  }

  @Override
  public void waveletUpdate(ReadableWaveletData wavelet, HashedVersion resultingVersion,
      List<VersionedWaveletDelta> deltas) {
    // TODO(ljvderijk): Determine which robot should receive which deltas,
    // enqueue deltas
    // for these robots
    // TODO(ljvderijk): Determine in face of participant remove and add which
    // robots should
    // receive which deltas
    // TODO(ljvderijk): What should happen when the given list of deltas does
    // (removeRobotDelta, randomOpsDelta, addRobotDelta)?
    // TODO(ljvderijk): Operations made by robots may not cause events for
    // itself
    for (ParticipantId participant : wavelet.getParticipants()) {
      RobotName robotName = RobotName.fromAddress(participant.getAddress());

      if (robotName == null) {
        // Not a valid robot name, next
        continue;
      }

      ParticipantId robotId = ParticipantId.ofUnsafe(robotName.toEmailAddress());
      AccountData account = accountStore.getAccount(robotId);

      if (account != null && account.isRobot()) {
        RobotAccountData robotAccount = account.asRobot();
        if (robotAccount.isVerified()) {
          Robot robot = getOrCreateRobot(robotName, robotAccount);
          updateRobot(robot, wavelet, deltas, resultingVersion);
        }
      }
    }
  }

  /**
   * Gets or creates a {@link Robot} for the given name.
   *
   * @param robotName the name of the robot.
   * @param account the {@link RobotAccountData} belonging to the given
   *        {@link RobotName}.
   */
  private Robot getOrCreateRobot(RobotName robotName, RobotAccountData account) {
    Robot robot = allRobots.get(robotName);

    if (robot == null) {
      robot = createNewRobot(robotName, account);
      allRobots.put(robotName, robot);
    }
    return robot;
  }

  /**
   * Creates a new {@link Robot}.
   *
   * @param robotName the name of the robot.
   * @param account the {@link RobotAccountData} belonging to the given
   *        {@link RobotName}.
   */
  private Robot createNewRobot(RobotName robotName, RobotAccountData account) {
    EventGenerator eventGenerator = new EventGenerator(robotName, conversationUtil);
    RobotOperationApplicator operationApplicator = new RobotOperationApplicator(
        converterManager, waveletProvider, new OperationServiceRegistryImpl(), conversationUtil);
    return new Robot(robotName, account, this, connector, converterManager, waveletProvider,
        eventGenerator, operationApplicator);
  }

  /**
   * Updates a {@link Robot} with information about a waveletUpdate event.
   *
   * @param robot The robot to process the update for.
   * @param wavelet the wavelet on which the update is occuring.
   * @param deltas the deltas the have been applied to the given wavelet.
   * @param resultingVersion version after application of deltas
   */
  private void updateRobot(Robot robot, ReadableWaveletData wavelet,
      List<VersionedWaveletDelta> deltas, HashedVersion resultingVersion) {
    try {
      robot.waveletUpdate(wavelet, deltas, resultingVersion);
      ensureScheduled(robot);
    } catch (OperationException e) {
      LOG.warning("Unable to update robot(" + robot.getRobotName() + ")", e);
    }
  }

  /**
   * Ensures that a robot is submitted to the executor, might submit the
   * {@link Robot} if it hasn't been submitted yet.
   *
   * <p>
   * Synchronized in combination with done() to keep proper track of the robots
   * that have been submitted.
   *
   * @param robot the {@link Robot} to enqueue
   */
  public synchronized void ensureScheduled(Robot robot) {
    if (!runnableRobots.contains(robot.getRobotName())) {
      LOG.info("Enqueing robot: " + robot.getRobotName());
      runnableRobots.add(robot.getRobotName());
      executor.execute(robot);
    }
  }

  /**
   * Signal that a robot is done running. Synchronized with ensureRunnable since
   * that method needs to have a synchronized view on the runnableRobots for
   * submitting task to the executor.
   *
   * @param robot the {@link Robot} which is done working.
   */
  public synchronized void doneRunning(Robot robot) {
    runnableRobots.remove(robot.getRobotName());
  }

  /**
   * Updates the account for the given {@link Robot}.
   *
   * @param robot the {@link Robot} to update.
   * @throws CapabilityFetchException if the capabilities could not be fetched
   *         or parsed.
   */
  public void updateRobotAccount(Robot robot) throws CapabilityFetchException {
    // TODO: Pass in activeAPIUrl
    String activeApiUrl = "";
    RobotAccountData newAccount = connector.fetchCapabilities(robot.getAccount(), activeApiUrl);
    accountStore.putAccount(newAccount);
    robot.setAccount(newAccount);
  }
}
