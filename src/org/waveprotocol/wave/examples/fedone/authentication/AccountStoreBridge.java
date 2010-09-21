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

package org.waveprotocol.wave.examples.fedone.authentication;

import com.google.inject.Inject;

import org.waveprotocol.wave.examples.fedone.persistence.AccountStore;

/**
 * An ugly hack.
 * 
 * Our application never gets a reference to the AccountStoreLoginModule. This
 * makes it impossible to wire up the account store (for account store-based
 * authentication).
 * 
 * Thus (unfortunately) this class must make the account store object (injected
 * with guice) accessible to the login module statically.
 * 
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class AccountStoreBridge {
  private static AccountStore store = null;
  
  @Inject
  public AccountStoreBridge(AccountStore store) {
    setAccountStore(store);
  }
  
  private static void setAccountStore(AccountStore newStore) {
    if (store != null && store != newStore) {
      throw new IllegalStateException("Account store bridge does not support"
          + " multiple account stores");
    }
    
    store = newStore;
  }
  
  /**
   * Get the currently configured AccountStore.
   * 
   * This should only be used by classes which cannot access the account store
   * in other ways (Eg, LoginModules).
   */
  public static AccountStore getAccountStore() {
    return store;
  }
}
