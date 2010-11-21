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
 *
 */

package org.waveprotocol.pst.model;

import com.google.common.collect.Lists;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.EnumValueDescriptor;

import java.util.List;

/**
 * Wraps a {@link EnumDescriptor} with methods suitable for stringtemplate.
 *
 * Called ProtoEnum rather than Enum to avoid java.lang namespace conflict.
 *
 * @author kalman@google.com (Benjamnin Kalman)
 */
public final class ProtoEnum {

  private final EnumDescriptor descriptor;

  public ProtoEnum(EnumDescriptor descriptor) {
    this.descriptor = descriptor;
  }

  /**
   * Returns the enum, for example:
   * <ul>
   * <li>org.waveprotocol.pst.examples.Example1.Person.Gender = "Gender"</li>
   * </ul>
   *
   * @return the name of the enum
   */
  public String getName() {
    return descriptor.getName();
  }

  /**
   * Returns the enum values, for example:
   * <ul>
   * <li>org.waveprotocol.pst.examples.Example1.Person.Gender = [MALE, FEMALE, OTHER]</li>
   * </ul>
   *
   * @return the enum values
   */
  public List<EnumValue> getValues() {
    List<EnumValue> enums = Lists.newArrayList();
    for (EnumValueDescriptor evd : descriptor.getValues()) {
      enums.add(new EnumValue(evd));
    }
    return enums;
  }
}
