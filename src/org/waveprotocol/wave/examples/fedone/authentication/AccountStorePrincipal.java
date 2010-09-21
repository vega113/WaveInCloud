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

import java.security.Principal;

/**
 * A principal for a wave user who logged in using the AccountStoreLoginModule. 
 * 
 * @author josephg@gmail.com (Joseph Gentle)
 */
public final class AccountStorePrincipal implements Principal {
  private String username;
  
  /**
   * Create a WavePrincipal for the given wave username.
   * 
   * @param username The user's username. Does not include the domain.
   */
  public AccountStorePrincipal(String username) {
    this.username = username;
  }
  
  @Override
  public String getName() {
    return username;
  }

  @Override
  public int hashCode() {
    return username.hashCode();
  }
  
  @Override
  public String toString() {
    return "[Principal " + username + "]";
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    AccountStorePrincipal other = (AccountStorePrincipal) obj;
    if (username == null) {
      if (other.username != null) return false;
    } else if (!username.equals(other.username)) return false;
    return true;
  }
}
