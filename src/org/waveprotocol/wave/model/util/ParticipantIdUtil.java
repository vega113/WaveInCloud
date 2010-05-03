// Copyright 2008 Google Inc.  All Rights Reserved.

package org.waveprotocol.wave.model.util;

import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Utility methods for participant IDs.
 *
*
 */
public final class ParticipantIdUtil {

  /** Unknown participant ID */
  public static final ParticipantId UNKNOWN = new ParticipantId("unknown");

  private ParticipantIdUtil() {}

  /**
   * Normalises an address.
   *
   * @param address  address to normalise; may be null
   * @return normal form of {@code address} if non-null; null otherwise.
   */
  // TODO(ohler): Make this not nullable.
  public static String normalize(String address) {
    if (address == null) {
      return null;
    }
    return address.toLowerCase();
  }

  /**
   * @param x a String
   * @return true if x is a wave address and normalized
   */
  // NOTE(ohler), 2009-10-27:
  // Unfortunately, not all addresses in our database are normalized:
  // Some deltas contain authors of "<nobody>" and similar (not wave
  // addresses), or may contain upper-case characters (not
  // normalized).  For future deltas, however, the wave server
  // enforces that they have normalized addresses as their authors and
  // as participant IDs in add/remove participant ops.
  //
  // If the federation code that serves outgoing deltas encounters a
  // (historic) delta that does not satisfy this constraint, it will
  // try to normalize the addresses in case the problem is just
  // upper-case characters, but if there's a junk address like
  // "<nobody>" that normalization doesn't fix, the entire wavelet
  // will become non-federable.
  public static boolean isNormalizedAddress(String x) {
    Preconditions.checkNotNull(x, "Null address");
    // TODO(ohler): Define what wave addresses really are, and add proper checks here.
    if (!x.equals(normalize(x))) {
      return false;
    }
    int at = x.indexOf('@');
    return at > 0 && isValidDomain(at + 1, x);
  }

  /**
   * Checks if the given string has a valid host name specified, starting at the
   * given start index. This method implements a check for a valid domain as
   * specified by RFC 1035, Section 2.3.1. It essentially checks if the domain
   * matches the following regular expression:
   * <tt>[a-z0-9]([a-z0-9\-]*[a-z0-9])(\.[a-z0-9]([a-z0-9\-]*[a-z0-9]))*</tt>.
   * Please note that the specification does not restrict TLDs, and therefore
   * my.arbitrary.domain passes the check. We also allow labels to start with
   * a digit to allow for domains such as 76.com. Furthermore, we allow only
   * strings specified by the subdomain non-terminal,to avoid allowing empty
   * string, which can be derived from the domain non-terminal.
   */
  public static boolean isValidDomain(int start, String x) {
    // TODO(user): Make sure we accept only valid TLDs.
    int index = start;
    int length = x.length() - start;
    if (length > 253 || length < 1) {
      return false;
    }
    while (index < x.length()) {
      char c = x.charAt(index);
      // A label must being with a letter or a digit.
      if (('a' > c || c > 'z') && ('0' > c || c > '9')) {
        return false;
      }
      char d = c;
      while (++index < x.length()) {
        c = x.charAt(index);
        // Subsequent characters may be letters, digits or the dash.
        if (('a' > c || c > 'z') && ('0' > c || c > '9') && (c != '-')) {
          break;
        }
        d = c;
      }
      if (index >= x.length()) {
        return d != '-';
      }
      // Labels must be separated by dots, and may not end with the dash.
      if ('.' != c || d == '-') {
        return false;
      }
      ++index;
    }
    // The domain ended in a dot, legal but we do not approve.
    return false;
  }

}
