// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.examples.fedone.federation.xmpp;

import com.google.protobuf.ByteString;

import org.apache.commons.codec.binary.Base64;

import java.nio.charset.Charset;

/**
 * Quick util for encoding and decoding ByteStrings.
 *
 *
 */
public class Base64Util {

  private static final Charset CHAR_SET = Charset.forName("UTF-8");

  /**
   * prevent instantiation
   */
  private Base64Util() {
  }

  public static String encode(ByteString bs) {
    return new String(Base64.encodeBase64(bs.toByteArray()), CHAR_SET);
  }

  public static String encode(byte[] ba) {
    return new String(Base64.encodeBase64(ba), CHAR_SET);
  }

  public static byte[] decodeFromArray(String str) {
    return Base64.decodeBase64(str.getBytes(CHAR_SET));
  }

  public static ByteString decode(String str) {
    return ByteString.copyFrom(Base64.decodeBase64(str.getBytes(CHAR_SET)));
  }
}
