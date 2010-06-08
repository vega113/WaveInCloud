package org.waveprotocol.wave.util;

import static org.junit.Assert.*;
import org.bouncycastle.crypto.engines.*;
import org.junit.Test;

/**
 * Test UUIDGen
 * 
 * @author Scott Crosby <scrosby@cs.rice.edu>
 */

public class UUIDGenTest {
  @Test
  /** Make sure we have the correct number of bits in each output */
  public void testNext() {
    byte key[] = { 0x77, 0x77, 0x66, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
    UUIDRandom rand = new UUIDRandom(UUIDGen.getUUIDGenerator(new AESEngine(), key));

    int i, bits;
    long max;
    for (bits = 1; bits <= 32; bits++) {
      max = 0;
      System.out.format("(%2d)  ", bits);
      for (i = 0; i < 10; i++) {
        int out = rand.next(bits);
        long out2 = ((long) out) & 0xffffffffL;
        if (max < out2)
          max = out2;
        System.out.format("%x ", out2);
      }
      System.out.print("\n");
      System.out.format("Bits=%2d : MAX = %x\n", bits, max);
      assertTrue((max >> bits) == 0); // Everything should have at most bits
                                      // bits.
      assertTrue((max >>> (bits - 1)) != 0); // Should always a high bit of one
                                             // in the max.
    }
  }

  /** Try to confirm that the key counter increments all bytes as appropriate. */
  @Test
  public void testNextBytes() {
    byte key[] = { 0x77, 0x77, 0x77, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
    UUIDGen rand = UUIDGen.getUUIDGenerator(new AESEngine(), key);
    int i;
    byte buf[];

    for (i = 0; i < 100000; i++) {
      assertEquals(rand.getCounter()[0] & 0xff, (i) % 256);
      assertEquals(rand.getCounter()[1] & 0xff, ((i) / 256) % 256);
      assertEquals(rand.getCounter()[2] & 0xff, ((i) / 65536) % 256);
      buf = rand.nextBytes();
      if (i < 10) {
        prettyPrint(rand.getCounter());
        System.out.print(" : ");
        prettyPrint(buf);
        System.out.print("\n");
      }
    }
  }

  @Test
  public void testFillBytes() {
    byte buf[];

    buf = checkFillBytes(2);
    byte ref2[] = { 0x5a, (byte) 0x90 };
    assertArrayEquals(buf, ref2);

    // Test some edge-cases and eyeball to confirm we have no leftover 0x00
    // bytes.
    buf = checkFillBytes(15);
    assertTrue(buf[14] == (byte) 0xd9);
    buf = checkFillBytes(16);
    assertTrue(buf[15] == (byte) 0x3a);
    buf = checkFillBytes(17);
    assertTrue(buf[16] == (byte) 0x11);
    buf = checkFillBytes(18);
  }

  private void prettyPrint(byte buf[]) {
    int i = 0;
    for (i = 0; i < buf.length; i++) {
      System.out.format("%2x", buf[i]);
      if (i % 4 == 3)
        System.out.print(", ");
    }
  }

  private byte[] checkFillBytes(int len) {
    byte key[] = { 0x66, 0x66, 0x66, (byte) len, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
    UUIDGen rand = UUIDGen.getUUIDGenerator(new AESEngine(), key);

    byte buf[] = new byte[len];
    int i;

    for (i = 0; i < 4; i++) {
      prettyPrint(rand.getCounter());
      System.out.print(" : ");
      rand.fillBytes(buf);
      prettyPrint(buf);
      System.out.print("\n");
    }
    return buf;
  }
}
