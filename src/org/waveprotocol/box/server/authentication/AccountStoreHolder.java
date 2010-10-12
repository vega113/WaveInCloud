/**
 * Copyright 2010 Google Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.waveprotocol.box.server.authentication;

import com.google.gxp.com.google.common.base.Preconditions;

import org.waveprotocol.box.server.persistence.AccountStore;

/**
 * This class holds a reference to a global AccountStore object. It is used in
 * classes which are not instantiated by Guice, and for which the only way to
 * access the account store is via static singletons. This is the case for JAAS
 * configured login modules, like AccountStoreLoginModule.
 *
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class AccountStoreHolder {
  private static AccountStore store = null;

  synchronized public static void init(AccountStore newStore) {
    Preconditions.checkNotNull(newStore, "Account store cannot be null");
    Preconditions.checkState(store == null, "Account store already set");
    store = newStore;
  }

  /**
   * @return the non-null account store.
   */
  public static AccountStore getAccountStore() {
    Preconditions.checkNotNull(store, "Account store not set");
    return store;
  }
  
  /** Needed for testing. */
  public static void clearAccountStore() {
    store = null;
  }
}
