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

package org.waveprotocol.wave.examples.fedone.persistence;

import com.google.common.collect.Maps;
import com.google.wave.api.ProtocolVersion;
import com.google.wave.api.event.EventType;
import com.google.wave.api.robot.Capability;

import junit.framework.TestCase;

import org.waveprotocol.wave.examples.fedone.account.AccountData;
import org.waveprotocol.wave.examples.fedone.account.HumanAccountData;
import org.waveprotocol.wave.examples.fedone.account.HumanAccountDataImpl;
import org.waveprotocol.wave.examples.fedone.account.RobotAccountData;
import org.waveprotocol.wave.examples.fedone.account.RobotAccountDataImpl;
import org.waveprotocol.wave.examples.fedone.robots.RobotCapabilities;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Testcases for the {@link AccountStore}. Implementors of these testcases are
 * responsible for cleanup.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public abstract class AccountStoreTestBase extends TestCase {

  private static final ParticipantId HUMAN_ID = ParticipantId.ofUnsafe("human@example.com");

  private static final ParticipantId ROBOT_ID = ParticipantId.ofUnsafe("robot@example.com");

  private RobotAccountData robotAccount;

  private RobotAccountData updatedRobotAccount;

  private HumanAccountData humanAccount;

  private HumanAccountData convertedRobot;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    humanAccount = new HumanAccountDataImpl(HUMAN_ID);
    robotAccount = new RobotAccountDataImpl(ROBOT_ID, "example.com", null, false);
    updatedRobotAccount =
        new RobotAccountDataImpl(ROBOT_ID, "example.com", new RobotCapabilities(
            Maps.<EventType, Capability> newHashMap(), "FAKEHASH", ProtocolVersion.DEFAULT), true);
    convertedRobot = new HumanAccountDataImpl(ROBOT_ID);
  }

  /**
   * Returns a new empty {@link AccountStore}.
   */
  protected abstract AccountStore newAccountStore();

  public final void testPutAccount() {
    AccountStore accountStore = newAccountStore();

    accountStore.putAccount(humanAccount);
    AccountData retrievedAccount = accountStore.getAccount(HUMAN_ID);
    assertEquals(humanAccount, retrievedAccount);

    accountStore.putAccount(robotAccount);
    retrievedAccount = accountStore.getAccount(ROBOT_ID);
    assertEquals(robotAccount, retrievedAccount);
  }

  public final void testPutAccountOverrides() {
    AccountStore accountStore = newAccountStore();

    accountStore.putAccount(robotAccount);
    AccountData account = accountStore.getAccount(ROBOT_ID);
    assertEquals(robotAccount, account);

    accountStore.putAccount(updatedRobotAccount);
    AccountData updatedAccount = accountStore.getAccount(ROBOT_ID);
    assertEquals(updatedRobotAccount, updatedAccount);
  }

  public final void testPutAccountCanChangeType() {
    AccountStore accountStore = newAccountStore();

    accountStore.putAccount(robotAccount);
    AccountData account = accountStore.getAccount(ROBOT_ID);
    assertEquals(robotAccount, account);

    accountStore.putAccount(convertedRobot);
    AccountData updatedAccount = accountStore.getAccount(ROBOT_ID);
    assertEquals(convertedRobot, updatedAccount);
  }

  public final void testRemoveAccount() {
    AccountStore accountStore = newAccountStore();

    accountStore.putAccount(robotAccount);
    AccountData account = accountStore.getAccount(ROBOT_ID);
    assertEquals(robotAccount, account);

    accountStore.removeAccount(ROBOT_ID);
    assertNull("Removed account was not null", accountStore.getAccount(ROBOT_ID));
  }
}
