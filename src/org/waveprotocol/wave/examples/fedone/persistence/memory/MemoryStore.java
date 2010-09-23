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

package org.waveprotocol.wave.examples.fedone.persistence.memory;

import org.waveprotocol.wave.crypto.CertPathStore;
import org.waveprotocol.wave.crypto.DefaultCertPathStore;
import org.waveprotocol.wave.crypto.SignatureException;
import org.waveprotocol.wave.crypto.SignerInfo;
import org.waveprotocol.wave.examples.fedone.account.AccountData;
import org.waveprotocol.wave.examples.fedone.persistence.AccountStore;
import org.waveprotocol.wave.federation.Proto.ProtocolSignerInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of persistence.
 *
 * <p>
 * {@link CertPathStore} implementation just forwards to the
 * {@link DefaultCertPathStore}.
 *
 *<p>
 *{@link AccountStore} implementation stores {@link AccountData} in a map keyed by username.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 *
 */
public class MemoryStore implements CertPathStore, AccountStore {

  private final CertPathStore certPathStore;

  public MemoryStore() {
    certPathStore = new DefaultCertPathStore();
    accountStore = new ConcurrentHashMap<String, AccountData>();
  }

  @Override
  public SignerInfo getSignerInfo(byte[] signerId) {
    return certPathStore.getSignerInfo(signerId);
  }

  @Override
  public void putSignerInfo(ProtocolSignerInfo protobuff) throws SignatureException {
    certPathStore.putSignerInfo(protobuff);
  }


  /*
   *  AccountStore
   */

  private final Map<String, AccountData> accountStore;

  @Override
  public AccountData getAccount(String username) {
    return accountStore.get(username);
  }

  @Override
  public void putAccount(AccountData account) {
    accountStore.put(account.getAddress(), account);
  }

  @Override
  public void removeAccount(String username) {
    accountStore.remove(username);
  }
}
