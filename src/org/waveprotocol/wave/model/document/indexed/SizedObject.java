// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.indexed;

/**
 * An object with size
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public interface SizedObject {

  /**
   * @return The total size of the object
   */
  int size();
}
