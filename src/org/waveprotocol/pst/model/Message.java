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

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.EnumDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;

import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Set;

/**
 * Wraps a {@link Descriptor} with methods suitable for stringtemplate.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public final class Message {

  private final Descriptor descriptor;
  private final String templateName;

  // Lazily created.
  private List<Field> fields = null;
  private Set<Type> fieldTypes = null;
  private List<Message> messages = null;
  private List<ProtoEnum> enums = null;

  public Message(Descriptor descriptor, String templateName) {
    this.descriptor = descriptor;
    this.templateName = templateName;
  }

  /**
   * Returns the type of the message, for example:
   * <ul>
   * <li>org.waveprotocol.pst.examples.Example1.Person = "Person"</li>
   * </ul>
   *
   * @return the name of the protocol buffer message
   */
  public String getMessageType() {
    return descriptor.getName();
  }

  /**
   * Returns the qualified type of the protobuf message, for example:
   * <ul>
   * <li>org.waveprotocol.pst.examples.Example1.Person =
   *     "org.waveprotocol.pst.examples.Example1.Person"</li>
   * </ul>
   *
   * @return the full type of the protocol buffer message
   */
  public String getProtoType() {
    String[] path = descriptor.getFile().getName().split("[/.]");
    String baseClass = Util.capitalize(path[path.length - 2]);
    Deque<String> messages = Lists.newLinkedList();
    for (Descriptor message = descriptor; message != null; message = message.getContainingType()) {
      messages.push(message.getName());
    }
    return getPackage() + '.' + baseClass + '.' + Joiner.on('.').join(messages);
  }

  /**
   * Returns the type of the Java message, for example:
   * <ul>
   * <li>org.waveprotocol.pst.examples.Example1.Person = <ul>
   *     <li>"PersonMessage" (for template name "message")</li>
   *     <li>"PersonMessageServerImpl" (for template name "messageServerImpl")</li></ul>
   * </ul>
   *
   * @return the name of the Java message
   */
  public String getJavaType() {
    return descriptor.getName() + Util.capitalize(templateName);
  }

  /**
   * Returns the name of the Java message, for example:
   * <ul>
   * <li>org.waveprotocol.pst.examples.Example1.Person = "org.waveprotoco.pst.examples"</li>
   * </ul>
   *
   * @return the package of the message
   */
  public String getPackage() {
    String javaPackage = descriptor.getFile().getOptions().getJavaPackage();
    if (Strings.isNullOrEmpty(javaPackage)) {
      javaPackage = descriptor.getFile().getPackage();
    }
    return javaPackage;
  }

  /**
   * @return the filename of the protocol buffer (.proto) file where the message
   *         is defined
   */
  public String getFilename() {
    return descriptor.getFile().getName();
  }

  /**
   * @return the fields of the message
   */
  public List<Field> getFields() {
    if (fields == null) {
      ImmutableList.Builder<Field> builder = ImmutableList.builder();
      for (FieldDescriptor fd : descriptor.getFields()) {
        builder.add(new Field(fd, new Type(fd, templateName)));
      }
      fields = builder.build();
    }
    return fields;
  }

  /**
   * @return the set of all types (primitive, enum, message) of the fields of
   *         this message
   */
  public Set<Type> getFieldTypes() {
    if (fieldTypes == null) {
      Set<Type> fieldTypesSet = Sets.newLinkedHashSet();
      for (FieldDescriptor fd : descriptor.getFields()) {
        fieldTypesSet.add(new Type(fd, templateName));
      }
      fieldTypes = Collections.unmodifiableSet(fieldTypesSet);
    }
    return fieldTypes;
  }

  /**
   * @return the nested messages of the message
   */
  public List<Message> getMessages() {
    if (messages == null) {
      ImmutableList.Builder<Message> builder = ImmutableList.builder();
      for (Descriptor d : descriptor.getNestedTypes()) {
        builder.add(new Message(d, templateName));
      }
      messages = builder.build();
    }
    return messages;
  }

  /**
   * @return the nested enums of the message
   */
  public List<ProtoEnum> getEnums() {
    if (enums == null) {
      ImmutableList.Builder<ProtoEnum> builder = ImmutableList.builder();
      for (EnumDescriptor ed : descriptor.getEnumTypes()) {
        builder.add(new ProtoEnum(ed));
      }
      enums = builder.build();
    }
    return enums;
  }

  /**
   * @return whether this is an inner class
   */
  public boolean isInner() {
    return descriptor.getContainingType() != null;
  }
}
