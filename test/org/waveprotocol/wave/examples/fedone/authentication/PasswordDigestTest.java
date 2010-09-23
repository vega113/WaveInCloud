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

package org.waveprotocol.wave.examples.fedone.authentication;

import junit.framework.TestCase;

/**
 * @author josephg@gmail.com (Joseph Gentle)
 */
public class PasswordDigestTest extends TestCase {
  public void testPasswordValidatesItself() {
    PasswordDigest pwd = new PasswordDigest();
    pwd.set("internet".toCharArray());
    assertTrue(pwd.verify("internet".toCharArray()));
    assertFalse(pwd.verify("wrongpwd".toCharArray()));
  }

  public void testUnsetPasswordVerifiesFalse() {
    PasswordDigest pwd = new PasswordDigest();

    assertFalse(pwd.verify("".toCharArray()));
    assertFalse(pwd.verify(null));
    assertFalse(pwd.verify("somestring".toCharArray()));
  }

  public void testPasswordResetWorks() {
    PasswordDigest pwd = new PasswordDigest();
    pwd.set("internet".toCharArray());
    pwd.set("newandshiny".toCharArray());
    assertTrue(pwd.verify("newandshiny".toCharArray()));
  }

  public void testSerializeDeserialize() {
    PasswordDigest pwd = new PasswordDigest();
    pwd.set("internet".toCharArray());

    byte[] digest = pwd.getDigest();
    byte[] salt = pwd.getSalt();
    PasswordDigest roundtripped = PasswordDigest.from(salt, digest);

    assertTrue(pwd.verify("internet".toCharArray()));
    assertFalse(pwd.verify("wrongpwd".toCharArray()));
  }

  public void testSaltAtLeast10Bytes() {
    PasswordDigest pwd = new PasswordDigest();
    pwd.set("internet".toCharArray());
    byte[] salt = pwd.getSalt();
    assertTrue(salt.length >= 10);
  }
}
