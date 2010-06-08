package org.waveprotocol.wave.util;

import java.util.Random;

/**
 * Wrap a UUIDGen random number generator to support the Random() interface.
 * 
 * Exports the same interface random number fetching interface as UUIDRandom.
 * 
 * @author Scott Crosby <scrosby@cs.rice.edu>
 * */
public class UUIDRandom extends Random {
  private static final long serialVersionUID = -6436633004565969210L;

  /** The underlying generator we're wrapping */
  private UUIDGen generator;

  /**
   * Create a Random number generator.
   * 
   * @param generator The generator to wrap.
   */
  public UUIDRandom(UUIDGen generator) {
    super();
    if (generator == null)
      throw new NullPointerException("null generator"); 
    this.generator = generator;
  }

  /**
   * It is illegal to set a new seed with this generator. Unfortunately, I
   * cannot unconditionally throw an error, java.util.Random tries to 'seed' the
   * generator by default.
   */
  @Override
  public void setSeed(long seed) {
    // ignore the call from the java.util.Random constructor which is invoked 
    // before generator is set 
    if (generator == null)
      return;   
    // Otherwise error out.
    throw new UnsupportedOperationException(); 
    }

  @Override
  public int next(int numBits) {
    return generator.next(numBits);
  }
}
