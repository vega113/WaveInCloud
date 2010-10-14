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

package org.waveprotocol.box.server.persistence;

import com.google.wave.api.ProtocolVersion;
import com.google.wave.api.event.EventType;
import com.google.wave.api.robot.Capability;

import junit.framework.TestCase;

import org.waveprotocol.box.server.account.AccountData;
import org.waveprotocol.box.server.account.HumanAccountData;
import org.waveprotocol.box.server.account.HumanAccountDataImpl;
import org.waveprotocol.box.server.account.RobotAccountData;
import org.waveprotocol.box.server.account.RobotAccountDataImpl;
import org.waveprotocol.box.server.authentication.PasswordDigest;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.robots.RobotCapabilities;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Map;

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

  private HumanAccountData convertedRobot;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    robotAccount = new RobotAccountDataImpl(ROBOT_ID, "example.com", "secret", null, false);

    // For the updatedRobotAccount, we'll put a few capabilities in with a mix
    // of field values.
    Map<EventType, Capability> capabilities = CollectionUtils.newHashMap();
    // Currently, capabilities can't be compared by .equals.
//    capabilities.put(
//        EventType.WAVELET_BLIP_CREATED, new Capability(EventType.WAVELET_BLIP_CREATED));
//    capabilities.put(EventType.DOCUMENT_CHANGED,
//        new Capability(EventType.DOCUMENT_CHANGED, CollectionUtils.newArrayList(Context.SIBLINGS)));
    
    // Currently, the pattern isn't persisted.
//    capabilities.put(EventType.BLIP_SUBMITTED,
//        new Capability(EventType.BLIP_SUBMITTED,
//            CollectionUtils.newArrayList(Context.SIBLINGS, Context.PARENT), "blah"));
    
    updatedRobotAccount =
        new RobotAccountDataImpl(ROBOT_ID, "example.com", "secret", new RobotCapabilities(
            capabilities, "FAKEHASH", ProtocolVersion.DEFAULT), true);
    convertedRobot = new HumanAccountDataImpl(ROBOT_ID);
  }

  /**
   * Returns a new empty {@link AccountStore}.
   */
  protected abstract AccountStore newAccountStore();

  public final void testRoundtripHumanAccount() {
    AccountStore accountStore = newAccountStore();

    HumanAccountDataImpl account = new HumanAccountDataImpl(HUMAN_ID);
    accountStore.putAccount(account);
    AccountData retrievedAccount = accountStore.getAccount(HUMAN_ID);
    assertEquals(account, retrievedAccount);
  }
  
  public final void testRoundtripHumanAccountWithPassword() {
    AccountStore accountStore = newAccountStore();
    
    accountStore.putAccount(
        new HumanAccountDataImpl(HUMAN_ID, new PasswordDigest("internet".toCharArray())));
    AccountData retrievedAccount = accountStore.getAccount(HUMAN_ID);
    assertTrue(retrievedAccount.asHuman().getPasswordDigest().verify("internet".toCharArray()));
  }

  public final void testRoundtripRobotAccount() {
    AccountStore accountStore = newAccountStore();

    accountStore.putAccount(robotAccount);
    AccountData retrievedAccount = accountStore.getAccount(ROBOT_ID);
    assertEquals(robotAccount, retrievedAccount);
  }

  public final void testGetMissingAccountReturnsNull() {
    AccountStore accountStore = newAccountStore();

    assertNull(accountStore.getAccount(HUMAN_ID));
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