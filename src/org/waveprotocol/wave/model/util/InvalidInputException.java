// Copyright 2009 Google Inc. All Rights Reserved.
package org.waveprotocol.wave.model.util;

/**
 * Checked exception thrown for invalid input.
 *
*
 */
public final class InvalidInputException extends Exception {
  public InvalidInputException(String message) {
    super(message);
  }
}
