// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.util;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.util.FuzzingBackOffGenerator.BackOffParameters;

/**
 * Test for FuzzingBackOffGenerator.
 *
 * @author zdwang@google.com (David Wang)
 */
public class FuzzingBackOffGeneratorTest extends TestCase {

  public void testSimple() {
    FuzzingBackOffGenerator generator = new FuzzingBackOffGenerator(2, 1000, 0.5);

    // this number is fibonacci
    BackOffParameters next = generator.next();
    expectRange(next.targetDelay, 2, 3);
    expectRange(next.minimumDelay, 1, 2);

    next = generator.next();
    expectRange(next.targetDelay, 2, 3);
    expectRange(next.minimumDelay, 1, 3);

    next = generator.next();
    expectRange(next.targetDelay, 4, 6);
    expectRange(next.minimumDelay, 2, 4);

    next = generator.next();
    expectRange(next.targetDelay, 6, 9);
    expectRange(next.minimumDelay, 3, 6);

    for (int i = 0; i < 100; i++) {
     generator.next();
    }
    next = generator.next();
    expectRange(next.targetDelay, 1000, 1500);
    expectRange(next.minimumDelay, 500, 1000);
  }

  private void expectRange(int value, int min, int max) {
    assertTrue("expected " + min + " <= (value) " + value, min <= value);
    assertTrue("expected " + max + " >= (value) " + value, value <= max);
  }

  public void testIllegalArgument() {
    try {
      new FuzzingBackOffGenerator(2, 1000, 2);
      fail("Should not be able to create FuzzingBackOffGenerator with bad randomisationFactor");
    } catch (IllegalArgumentException ex) {
      // Expected
    }

    try {
      new FuzzingBackOffGenerator(0, 1000, 0.5);
      fail("Should not be able to create FuzzingBackOffGenerator with bad initialBackOff");
    } catch (IllegalArgumentException ex) {
      // Expected
    }
  }

  public void testReset() {
    FuzzingBackOffGenerator generator = new FuzzingBackOffGenerator(2, 1000, 0.5);

    BackOffParameters next = generator.next();
    expectRange(next.targetDelay, 2, 3);
    expectRange(next.minimumDelay, 1, 2);

    next = generator.next();
    expectRange(next.targetDelay, 2, 3);
    expectRange(next.minimumDelay, 1, 2);

    next = generator.next();
    expectRange(next.targetDelay, 4, 6);
    expectRange(next.minimumDelay, 2, 4);

    generator.reset();
    next = generator.next();

    expectRange(next.targetDelay, 2, 3);
    expectRange(next.minimumDelay, 1, 2);

    next = generator.next();
    expectRange(next.targetDelay, 2, 3);
    expectRange(next.minimumDelay, 1, 2);

    next = generator.next();
    expectRange(next.targetDelay, 4, 6);
    expectRange(next.minimumDelay, 2, 4);
  }

  /**
   * Rather than trying to inject a random number generator to such a tiny clas, such an overkill,
   * let's do something simple
   */
  public void testGeneratesRamdomNumber() {
    FuzzingBackOffGenerator generator = new FuzzingBackOffGenerator(2000, 10000, 0.5);

    for (int i = 0; i < 100; i++) {
      if (generator.next().targetDelay != 2000) {
        return;
      }
    }
    fail("100x same value, it's not random.");
  }

  public void testMaxReachedImmediately() {
    FuzzingBackOffGenerator generator = new FuzzingBackOffGenerator(5, 2, 0.5);

    BackOffParameters next = generator.next();
    expectRange(next.targetDelay, 2, 3);
    expectRange(next.minimumDelay, 1, 2);

    next = generator.next();
    expectRange(next.targetDelay, 2, 3);
    expectRange(next.minimumDelay, 1, 2);

    next = generator.next();
    expectRange(next.targetDelay, 2, 3);
    expectRange(next.minimumDelay, 1, 2);


  }

}
