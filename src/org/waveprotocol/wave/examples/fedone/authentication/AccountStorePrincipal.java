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

import java.security.Principal;

/**
 * A principal for a wave user who logged in using the AccountStoreLoginModule. 
 * 
 * @author josephg@gmail.com (Joseph Gentle)
 */
public final class AccountStorePrincipal implements Principal {
  final private String address;
  
  /**
   * Create a WavePrincipal for the given wave username.
   * 
   * @param address The user's username. Does not include the domain.
   */
  public AccountStorePrincipal(String address) {
    Preconditions.checkNotNull(address, "Address is null");
    this.address = address;
  }
  
  @Override
  public String getName() {
    return address;
  }

  @Override
  public String toString() {
    return "[Principal " + address + "]";
  }
  
  @Override
  public int hashCode() {
    return address.hashCode();
  }
  
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    AccountStorePrincipal other = (AccountStorePrincipal) obj;
    return address.equals(other.address);
  }
}
