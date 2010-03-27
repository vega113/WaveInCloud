// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.util;

/**
 * Receives notification of the deletion of some structure.
 *
 * @author anorth@google.com (Alex North)
 */
public interface DeletionListener {
  void onDeleted();
}
