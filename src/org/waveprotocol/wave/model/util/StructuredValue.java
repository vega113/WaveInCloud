// Copyright 2009 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.util;

import java.util.Map;

/**
 * Encapsulates a structured value with named fields which can be set and read.
 * It is intended that implementations function as records, with a fixed set of
 * valid keys.
 *
 * @author anorth@google.com (Alex North)
 * @param <K> enumerated type of the field names
 * @param <V> field value type
 */
public interface StructuredValue<K extends Enum<K>, V> {
  /**
   * Sets the value for a field.
   *
   * @param name field name to set
   * @param value new value
   */
  void set(K name, V value);

  /**
   * Atomically sets values for multiple fields.
   *
   * @param values field names and values to set
   */
  void set(Map<K,  V> values);

  /**
   * @return the current value of a field.
   */
  V get(K name);
}
