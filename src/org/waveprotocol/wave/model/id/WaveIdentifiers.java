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
 */

package org.waveprotocol.wave.model.id;

import org.waveprotocol.wave.model.util.Utf16Util;
import org.waveprotocol.wave.model.util.Utf16Util.CodePointHandler;

import java.util.Arrays;

/**
 * Utilities for working with identifiers compliant with the new specification.
 *
 * @author anorth@google.com (Alex North)
 * @see "http://code.google.com/p/wave-protocol/source/browse/spec/waveid/waveidspec.rst"
 */
public final class WaveIdentifiers {

  /**
   * Boolean array defining ASCII chars allowed in an identifier.
   * Entries correspond to character values.
   */
  private static final boolean[] SAFE_ASCII_CHARS;

  static {
    SAFE_ASCII_CHARS = new boolean[0x7F];
    for (char c = 'A'; c <= 'Z'; ++c) {
      SAFE_ASCII_CHARS[c] = true;
    }
    for (char c = 'a'; c <= 'z'; ++c) {
      SAFE_ASCII_CHARS[c] = true;
    }
    for (char c = '0'; c <= '9'; ++c) {
      SAFE_ASCII_CHARS[c] = true;
    }
    for (char c : Arrays.asList('-', '.', '_', '~', '+', '*', '@')) {
      SAFE_ASCII_CHARS[c] = true;
    }
  }

  private static final CodePointHandler<Boolean> GOOD_UTF16_FOR_ID =
      new CodePointHandler<Boolean>() {
        @Override
        public Boolean codePoint(int cp) {
          if (!Utf16Util.isCodePointValid(cp)) {
            return false;
          }
          if (cp < SAFE_ASCII_CHARS.length && !SAFE_ASCII_CHARS[cp]) {
            return false;
          }
          if (cp >= SAFE_ASCII_CHARS.length && !isUcsChar(cp)) {
            return false;
          }
          return null;
        }

        @Override
        public Boolean endOfString() {
          return true;
        }

        @Override
        public Boolean unpairedSurrogate(char c) {
          return false;
        }
  };

  /**
   * Checks whether a UTF-16 string is a valid wave identifier.
   */
  public static boolean isValidIdentifier(String id) {
    return Utf16Util.traverseUtf16String(id, GOOD_UTF16_FOR_ID);
  }

  /**
   * Checks whether an int value is a valid UCS code-point above 0x7F as defined
   * in RFC 3987.
   */
  private static boolean isUcsChar(int c) {
    return (c >= 0xA0 && c <= 0xD7FF) || (c >= 0xF900 && c <= 0xFDCF)
        || (c >= 0xFDF0 && c <= 0xFFEF) || (c >= 0x10000 && c <= 0x1FFFD)
        || (c >= 0x20000 && c <= 0x2FFFD) || (c >= 0x30000 && c <= 0x3FFFD)
        || (c >= 0x40000 && c <= 0x4FFFD) || (c >= 0x50000 && c <= 0x5FFFD)
        || (c >= 0x60000 && c <= 0x6FFFD) || (c >= 0x70000 && c <= 0x7FFFD)
        || (c >= 0x80000 && c <= 0x8FFFD) || (c >= 0x90000 && c <= 0x9FFFD)
        || (c >= 0xA0000 && c <= 0xAFFFD) || (c >= 0xB0000 && c <= 0xBFFFD)
        || (c >= 0xC0000 && c <= 0xCFFFD) || (c >= 0xD0000 && c <= 0xDFFFD)
        || (c >= 0xE1000 && c <= 0xEFFFD);
  }

  private WaveIdentifiers() {
  }
}