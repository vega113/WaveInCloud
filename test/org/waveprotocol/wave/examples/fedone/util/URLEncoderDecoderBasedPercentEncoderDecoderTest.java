// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.examples.fedone.util;

import org.waveprotocol.wave.model.id.URIEncoderDecoder;

import junit.framework.TestCase;

/**
 * Tests {@link URLEncoderDecoderBasedPercentEncoderDecoder}.
 *
 *
 */
public class URLEncoderDecoderBasedPercentEncoderDecoderTest extends TestCase {
  private static final String NOT_ESCAPED =
      "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ:@!$&'()*+,;=-._~";

  // Example from http://en.wikipedia.org/wiki/UTF-16/UCS-2
  private static final String DECODED_STRING = new String(new char[] {
      'a', '1', '?', '#', '@',
      0x7A,  // 122, small Z (Latin)
      0x6C34,  // 27700 ,  water (Chinese)
      0xD834, 0xDD1E,  // 119070, musical G clef
  });

  private static final String ENCODED_STRING = "a1%3F%23@z%E6%B0%B4%F0%9D%84%9E";

  private final URIEncoderDecoder encoder =
      new URIEncoderDecoder(new URLEncoderDecoderBasedPercentEncoderDecoder());

  public void testEncodingNotEscaped() throws URIEncoderDecoder.EncodingException {
    TestCase.assertEquals(NOT_ESCAPED, encoder.encode(NOT_ESCAPED));
  }

  public void testEncoding() throws URIEncoderDecoder.EncodingException {
    TestCase.assertEquals(ENCODED_STRING, encoder.encode(DECODED_STRING));
  }

  public void testDecodingNotEscaped() throws URIEncoderDecoder.EncodingException {
    TestCase.assertEquals(NOT_ESCAPED, encoder.decode(NOT_ESCAPED));
  }

  public void testDecoding() throws URIEncoderDecoder.EncodingException {
    TestCase.assertEquals(DECODED_STRING, encoder.decode(ENCODED_STRING));
  }

  public void testBadDecoding() {
    try {
      TestCase.assertFalse("%".equals(encoder.decode("%")));
    } catch (IllegalArgumentException ex) {
      // Expected to happen in pure java test
    } catch (URIEncoderDecoder.EncodingException e) {
      // Also valid to throw
    }


    try {
      // Bad utf 8, example from http://en.wikipedia.org/wiki/UTF-8
      encoder.decode("abc%FE%FF");
      TestCase.fail("Not supposed to be able to decode invalid utf-8");
    } catch (URIEncoderDecoder.EncodingException e) {
      // Also valid to throw an exception
    }
  }

  public void testBadEncoding() {
    try {

      String invalidUTF16 = new String(new char[] {
        0xD834, 0xD834  // first half of surrogate pair x 2
      });

      // Encoding invalid UTF16 is unspecified, so anything is OK.
      // If percent encoding is done using java.net.URLEncoder, no exception is thrown
      // except the invalid chars are replaces with "?", whilst com.google.gwt.http.client.URL
      // throws an exception.
      encoder.encode(invalidUTF16);
    } catch (URIEncoderDecoder.EncodingException e) {
      // Throwing an exception is acceptable too.
    }
  }
}
