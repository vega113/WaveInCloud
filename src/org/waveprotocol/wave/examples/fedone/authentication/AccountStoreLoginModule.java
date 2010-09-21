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
import com.google.common.base.Preconditions;

import org.waveprotocol.wave.examples.fedone.account.AccountData;
import org.waveprotocol.wave.examples.fedone.persistence.AccountStore;

import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

/**
 * A Simple login module which does username & password authentication against
 * users in a database.
 * 
 * This code is based on the example here:
 *   http://java.sun.com/developer/technicalArticles/Security/jaasv2/
 * 
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class AccountStoreLoginModule implements LoginModule {
  private Subject subject;
  private AccountStorePrincipal principal;
  private CallbackHandler callbackHandler;
  private AccountStore accountStore;
  
  private static final int NOT = 0;
  private static final int OK = 1;
  private static final int COMMIT = 2;
  private int status;
  
  @Override
  public void initialize(Subject subject, CallbackHandler callbackHandler,
      Map<String, ?> sharedState, Map<String, ?> options) {
    accountStore = AccountStoreBridge.getAccountStore();
    Preconditions.checkNotNull(accountStore);
    
    status = NOT;
    this.subject = subject;
    this.callbackHandler = callbackHandler;
  }

  @Override
  public boolean login() throws LoginException {
    if(callbackHandler == null) {
      throw new LoginException("No callback handler is available");
    }
    Callback callbacks[] = new Callback[]{
      new NameCallback("Username"),
      new PasswordCallback("Password", false),
    };
    
    String name = null;
    char[] password = null;
    try {
      callbackHandler.handle(callbacks);
      name = ((NameCallback)callbacks[0]).getName();
      password = ((PasswordCallback)callbacks[1]).getPassword();
    } catch(java.io.IOException ioe) {
      throw new LoginException(ioe.toString());
    } catch(UnsupportedCallbackException ce) {
      throw new LoginException("Error: " + ce.getCallback().toString());
    }
    
    boolean success;
    
    AccountData account = accountStore.getAccount(name);
    if (account == null) {
      // The user doesn't exist. Auth failed.
      success = false;
    } else if (!account.isHuman()) {
      // The account is owned by a robot. Auth failed.
      success = false;
    } else {
      success = account.asHuman().getPasswordDigest().verify(password);
    }
    
    if(success) {
      principal = new AccountStorePrincipal(name);
      status = OK;
      return true;
    } else {
      return false;
    }
  }

  @Override
  public boolean commit() {
    if(status == NOT) {
      return false;
    }
    if(subject == null) {
      return false;
    }
    subject.getPrincipals().add(principal);
    status = COMMIT;
    return true;
  }

  @Override
  public boolean abort() {
    if((subject != null) && (principal != null)) {
      subject.getPrincipals().remove(principal);
    }
    subject = null;
    principal = null;
    status = NOT;
    return true;
  }

  @Override
  public boolean logout() {
    subject.getPrincipals().remove(principal);
    status = NOT;
    subject = null;
    return true;
  }
}
