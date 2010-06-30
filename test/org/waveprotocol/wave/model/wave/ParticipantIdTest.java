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

package org.waveprotocol.wave.model.wave;

import junit.framework.TestCase;

/**
 * Tests for the {@link ParticipantId} class.
 * 
 * @author ljvderijk@gmail.com (Lennard de Rijk)
 */
public class ParticipantIdTest extends TestCase {

  private static final String EMPTY_ADDRESS = "";
  private static final String NO_DOMAIN_PREFIX = "test";
  private static final String NO_DOMAIN_ADDRESS = "test@";
  private static final String PREFIX_ONLY_ADDRESS = "@";
  private static final String DOMAIN_ONLY_ADDRESS = "@example.com";
  private static final String TYPICAL_ADDRESS = "test@example.com";

  /**
   * Tests that the validation fails on an empty address using the factory
   * methods in {@link ParticipantId}.
   */
  public void testEmptyAddressIsNotValid() {
    try {
      ParticipantId.of(EMPTY_ADDRESS);
      fail("Expected InvalidParticipantAddress Exception");
    } catch (InvalidParticipantAddress e) {
      // Should fail
    }

    try {
      ParticipantId.ofUnsafe(EMPTY_ADDRESS);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // Should fail
    }
  }

  /**
   * Tests that the validation fails on an address with no domain prefix using
   * the factory methods in {@link ParticipantId}.
   */
  public void testNoDomainPrefixIsNotValid() {
    try {
      ParticipantId.of(NO_DOMAIN_PREFIX);
      fail("Expected InvalidParticipantAddress Exception");
    } catch (InvalidParticipantAddress e) {
      // Should fail
    }

    try {
      ParticipantId.ofUnsafe(NO_DOMAIN_PREFIX);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // Should fail
    }
  }

  /**
   * Tests that the validation fails on an address that has no domain specified
   * using the factory methods in {@link ParticipantId}.
   */
  public void testNoDomainAddressIsNotValid() {
    try {
      ParticipantId.of(NO_DOMAIN_ADDRESS);
      fail("Expected InvalidParticipantAddress Exception");
    } catch (InvalidParticipantAddress e) {
      // Should fail
    }

    try {
      ParticipantId.ofUnsafe(NO_DOMAIN_ADDRESS);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // Should fail
    }
  }

  /**
   * Tests that the validation fails on an address with only the domain prefix
   * using the factory methods in {@link ParticipantId}.
   */
  public void testPrefixOnlyAddressIsNotValid() {
    try {
      ParticipantId.of(PREFIX_ONLY_ADDRESS);
      fail("Expected InvalidParticipantAddress Exception");
    } catch (InvalidParticipantAddress e) {
      // Should fail
    }

    try {
      ParticipantId.ofUnsafe(PREFIX_ONLY_ADDRESS);
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException e) {
      // Should fail
    }
  }

  /**
   * Tests the validation of a domain-only address using the factory methods in
   * {@link ParticipantId}.
   */
  public void testDomainOnlyIsValid() throws Exception {
    ParticipantId.of(DOMAIN_ONLY_ADDRESS);
    ParticipantId.ofUnsafe(DOMAIN_ONLY_ADDRESS);
  }

  /**
   * Tests the validation of a typical address using the factory methods in
   * {@link ParticipantId}.
   */
  public void testTypicalAddressIsValid() throws Exception {
    ParticipantId.of(TYPICAL_ADDRESS);
    ParticipantId.ofUnsafe(TYPICAL_ADDRESS);
  }

}
