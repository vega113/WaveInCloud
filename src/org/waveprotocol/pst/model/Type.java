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

import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;

/**
 * Wraps a {@link FieldDescriptor} to expose type-only information for
 * stringtemplate.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public final class Type {

  private final FieldDescriptor field;
  private final String templateName;

  public Type(FieldDescriptor field, String templateName) {
    this.field = field;
    this.templateName = templateName;
  }

  /**
   * Returns the type of the field as the Java type, for example:
   * <ul>
   * <li>org.waveprotocol.pst.examples.Example1.Person.first_name = "String"</li>
   * <li>org.waveprotocol.pst.examples.Example1.Person.age = "int"</li>
   * <li>org.waveprotocol.pst.examples.Example1.Person.gender = "Gender"</li>
   * <li>org.waveprotocol.pst.examples.Example1.Person.address = <ul>
   *     <li>"AddressMessage" (if template name is "message")</li>
   *     <li>"AddressMessageServerImpl" (if template name is "messageServerImpl")</li></ul></li>
   * </ul>
   *
   * @return the type of the field as the Java type
   */
  public String getJavaType() {
    switch (field.getJavaType()) {
      case BOOLEAN:
        return "boolean";
      case BYTE_STRING:
        return "ByteString";
      case DOUBLE:
        return "double";
      case ENUM:
        return field.getEnumType().getName();
      case FLOAT:
        return "float";
      case INT:
        return "int";
      case LONG:
        return "long";
      case MESSAGE:
        return field.getMessageType().getName() + Util.capitalize(templateName);
      case STRING:
        return "String";
      default:
        throw new UnsupportedOperationException("Unsupported field type " + field.getJavaType());
    }
  }

  /**
   * Returns the type of the field as the Java type capitalized, for example:
   * <ul>
   * <li>org.waveprotocol.pst.examples.Example1.Person.first_name = "String"</li>
   * <li>org.waveprotocol.pst.examples.Example1.Person.age = "Int"</li>
   * <li>org.waveprotocol.pst.examples.Example1.Person.gender = "Gender"</li>
   * <li>org.waveprotocol.pst.examples.Example1.Person.address = <ul>
   *     <li>"AddressMessage" (if template name is "message")</li>
   *     <li>"AddressMessageServerImpl" (if template name is "messageServerImpl")</li></ul></li>
   * </ul>
   *
   * @return the type of the field as the Java type
   */
  public String getCapJavaType() {
    return Util.capitalize(getJavaType());
  }

  /**
   * Returns the type of the field as the boxed Java type, for example:
   * <ul>
   * <li>org.waveprotocol.pst.examples.Example1.Person.first_name = "String"</li>
   * <li>org.waveprotocol.pst.examples.Example1.Person.age = "Integer"</li>
   * <li>org.waveprotocol.pst.examples.Example1.Person.gender = "Gender"</li>
   * <li>org.waveprotocol.pst.examples.Example1.Person.address = <ul>
   *     <li>"AddressMessage" (if template name is "message")</li>
   *     <li>"AddressMessageServerImpl" (if template name is "messageServerImpl")</li></ul></li>
   * </ul>
   *
   * @return the type of the field as a boxed Java type
   */
  public String getBoxedJavaType() {
    switch (field.getJavaType()) {
      case BOOLEAN:
        return "Boolean";
      case DOUBLE:
        return "Double";
      case FLOAT:
        return "Float";
      case INT:
        return "Integer";
      case LONG:
        return "Long";
      default:
        return getJavaType();
    }
  }

  /**
   * Gets the default value of the field (null for objects, empty strings/arrays, zero, false, etc).
   *
   * @return the "default value" of the field
   */
  public String getDefaultValue() {
    switch (field.getJavaType()) {
      case BOOLEAN:
        return "false";
      case BYTE_STRING:
        return "ByteString.EMPTY";
      case DOUBLE:
        return "0.0";
      case ENUM:
        return field.getEnumType().getName() + ".UNKNOWN";
      case FLOAT:
        return "0.0f";
      case INT:
        return "0";
      case LONG:
        return "0L";
      case MESSAGE:
        return "null";
      case STRING:
        return "\"\"";
      default:
        throw new UnsupportedOperationException("Unsupported field type " + field.getJavaType());
    }
  }

  /**
   * Returns the message type of the field without template suffix, for example:
   * <ul>
   * <li>org.waveprotocol.pst.examples.Example1.Person.first_name = undefined</li>
   * <li>org.waveprotocol.pst.examples.Example1.Person.age = undefined</li>
   * <li>org.waveprotocol.pst.examples.Example1.Person.gender = undefined</li>
   * <li>org.waveprotocol.pst.examples.Example1.Person.address =
   *     "Address" (regardless of template name)</li>
   * </ul>
   *
   * @return the message type of the field without template suffix
   */
  public String getMessageType() {
    return field.getMessageType().getName();
  }

  /**
   * Returns the full type of the protocol buffer enum or message, for example
   * <ul>
   * <li>org.waveprotocol.pst.examples.Example1.Person.first_name = undefined</li>
   * <li>org.waveprotocol.pst.examples.Example1.Person.age = undefined</li>
   * <li>org.waveprotocol.pst.examples.Example1.Person.gender =
   *     "org.waveprotocol.pst.examples.Example1.Person"</li>
   * <li>org.waveprotocol.pst.examples.Example1.Person.address =
   *     "org.waveprotocol.pst.examples.Example1.Address"</li>
   * </ul>
   *
   * @return the full type of the protocol buffer enum or message
   */
  public String getProtoType() {
    Descriptor containingType = null;
    switch (field.getType()) {
      case ENUM:
        containingType = field.getEnumType().getContainingType();
        break;
      case MESSAGE:
        containingType = field.getMessageType().getContainingType();
        break;
      default:
        throw new IllegalArgumentException("field is neither an enum nor message type");
    }
    return new Message(containingType, templateName).getProtoType();
  }

  /**
   * @return whether the field is a message
   */
  public boolean isMessage() {
    return field.getType().equals(FieldDescriptor.Type.MESSAGE);
  }

  /**
   * @return whether the field is an enum
   */
  public boolean isEnum() {
    return field.getType().equals(FieldDescriptor.Type.ENUM);
  }

  /**
   * @return whether the field type is a Java primitive
   */
  public boolean isPrimitive() {
    switch (field.getJavaType()) {
      case BOOLEAN:
      case DOUBLE:
      case FLOAT:
      case INT:
      case LONG:
        return true;
      default:
        return false;
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (o instanceof Type) {
      Type t = (Type) o;
      if (field.getType() == t.field.getType()) {
        switch (field.getType()) {
          case MESSAGE:
            return field.getMessageType().equals(t.field.getMessageType());
          case ENUM:
            return field.getEnumType().equals(t.field.getEnumType());
          default:
            return true;
        }
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    switch (field.getType()) {
      case MESSAGE:
        return field.getMessageType().hashCode();
      case ENUM:
        return field.getEnumType().hashCode();
      default:
        return field.getType().hashCode();
    }
  }
}
