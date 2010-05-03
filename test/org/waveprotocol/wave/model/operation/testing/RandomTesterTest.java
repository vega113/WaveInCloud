// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.operation.testing;


import junit.framework.TestCase;

import org.waveprotocol.wave.model.operation.testing.DomainRandomTester.FailureException;
import org.waveprotocol.wave.model.operation.testing.DomainRandomTester.Log;
import org.waveprotocol.wave.model.operation.testing.RationalDomain.Affine;

import java.math.BigInteger;

public class RandomTesterTest extends TestCase {

  private final int NUM_ITERATIONS = 50;
  DomainRandomTester<RationalDomain.Data, Affine> t;

  boolean expectFailure = false;

  public void testDataOpEquivalence() {
    BigInteger x = BigInteger.ZERO;
    createTester();
    t.testDataOperationEquivalence(NUM_ITERATIONS);
  }

  public void testInversion() {
    createTester();
    t.testOperationInversion(NUM_ITERATIONS);
  }

  public void testCompose() {
    createTester();
    t.testCompositionOnInitialState(NUM_ITERATIONS);
    t.testCompositionAssociativity(NUM_ITERATIONS);
    t.testSimpleComposition(NUM_ITERATIONS);
  }

  public void testTransform() {
    createTester();
    t.testTransformDiamondProperty(NUM_ITERATIONS);
  }

  protected void createTester() {
    RationalDomain d = new RationalDomain();
    AffineGenerator g = new AffineGenerator();
    createTester(d, g);
  }

  protected void createTester(RationalDomain d, AffineGenerator g) {
    t = new DomainRandomTester<RationalDomain.Data, Affine>(new Log() {
        @Override
        public void inconsistent(String... lines) {
          if (!expectFailure) {
            for (String line : lines) {
              System.err.println(line);
            }
          }
          throw new FailureException();
        }

        @Override
        public void fatal(Throwable exception, String... lines) {
          for (String line : lines) {
            System.err.println(line);
          }
          fail("EXCEPTION THROWN");
        }

        @Override
        public void info(String... lines) {
          for (String line : lines) {
            System.out.println(line);
          }
        }
      }, d, g);
  }
}
