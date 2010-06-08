package org.waveprotocol.wave.util;


import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;

/**
 * A random number generator suitable for GWT where there is low available
 * entropy. Instead, we can get a 128-bits of good entropy from a server, and
 * use it to seed a cipher in counter mode.
 * 
 * Code is not thread-safe, but could be made so by serializing updates to counter 
 * and not reusing outbuf.
 * 
 * @author Scott Crosby <scrosby@cs.rice.edu>
 */
public class UUIDGen {
  /** The default size of a an encrypted block. */
  public static final int SIZE = 16;

  /**
   * Factory method for returning UUID generators.
   * 
   * @param cipher
   *          A block cipher object from org.bouncycastle.crypto.engines that
   *          uses 128-bit keys. There are three implementations with different
   *          performance/codesize tradeoffs.
   * 
   * @param seed
   *          An array of 16 bytes used as a seed for the random number
   *          generator. Duplicate seeds are a crypto error and should NEVER be
   *          used. Currently there is no code to detect this misuse.
   *          In production where failure cannot be allowed, this error could be
   *          caught and additional entropy (if weak) added to the seed.
   * 
   * */
  static public UUIDGen getUUIDGenerator(BlockCipher cipher, byte seed[]) {
    if (seed.length != SIZE) {
      throw new IllegalArgumentException("Seeds should be "+SIZE+ " bytes");
    }
    return new UUIDGen(cipher, seed);
  }
  
  /** The underlying cipher to use; one of the three AES implementations. */
  protected BlockCipher cipher;

  /** Stores the counter that we encrypt in counter mode. */
  private final byte counter[];

  /** Scratch space used as the encrypted output. */
  private final byte outbuf[];

   
  /** Create a Random number generator. */
  protected UUIDGen(BlockCipher cipher, byte[] seed) {
    this.cipher = cipher;
    cipher.init(true, new KeyParameter(seed));
    this.counter = new byte[SIZE];
    this.outbuf = new byte[SIZE];
  }

  /** Used by UUIDRandom to generate random numbers. */
  public int next(int numBits) {
    incrementCounter();
    cipher.processBlock(counter, 0, outbuf, 0);
    int out = 0;
    int i = 0;
    while (numBits > 7) {
      out = out << 8;
      out = out ^ (0xff & (int) outbuf[i]);
      i++;
      numBits = numBits - 8;
    }
    if (numBits > 0) {
      out = out << numBits;
      out = out ^ (((1 << numBits) - 1) & (int) outbuf[i]);
    }
    return out;
  }

  /**
   * Fill an array of bytes with random numbers.
   * 
   * @param bytes
   *          An array to fill.
   */

  public void fillBytes(byte[] bytes) {
    int i = 0;
    while (i + SIZE <= bytes.length) {
      incrementCounter();
      cipher.processBlock(counter, 0, bytes, i);
      i = i + SIZE;
    }
    if (i < bytes.length) {
      incrementCounter();
      cipher.processBlock(counter, 0, outbuf, 0);
      int j = 0;
      for (; i + j < bytes.length; j++) {
        bytes[i + j] = outbuf[j];
      }
    }
  }

  /** Return one block of 128 bits of randomness. */
  public byte[] nextBytes() {
    byte out[] = new byte[SIZE];
    fillBytes(out);
    return out;
  }


  /** Increment the AES counter value. */
  private void incrementCounter() {
    for (int i = 0; i < counter.length && ++counter[i] == 0; i++)
      ;
  }

  /** Debug return the current counter value. */
  byte[] getCounter() {
    return counter.clone();
  }
}
