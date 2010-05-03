// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.operation.testing;

import java.util.Random;

import org.waveprotocol.wave.model.operation.testing.RationalDomain.Affine;

public class AffineGenerator implements RandomOpGenerator<RationalDomain.Data, Affine> {

  @Override
  public Affine randomOperation(RationalDomain.Data state, Random random) {
    int neg1 = random.nextBoolean() ? -1 : 1;
    int neg2 = random.nextBoolean() ? -1 : 1;

    long mulNum, mulDenom;
//
//    final BigInteger TOO_BIG = BigInteger.valueOf(1000);
//
//    if (state.value.numerator) > TOO_BIG) {
//      mulDenom = state.value.numerator;
//    } else {
//      mulDenom =  random.nextInt(9) + 1;
//    }
//    if (state.value.denominator > TOO_BIG) {
//      mulNum = state.value.denominator;
//    } else {
//      mulNum = neg1 * (random.nextInt(9) + 1);
//    }

    return new Affine(
        state.value,
        new Rational(neg1 * (random.nextInt(9) + 1), random.nextInt(9) + 1),
        new Rational(neg2 * (random.nextInt(10)), random.nextInt(9) + 1)
        );
  }

}
