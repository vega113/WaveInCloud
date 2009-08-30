/**
 * Copyright 2009 Google Inc.
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

package org.waveprotocol.wave.examples.fedone.waveserver;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

/**
 * Enforces consistency between serialised representations of protocol buffers (stored as
 * {@code ByteString}s) and their types.
 *
 *
 */
public final class ByteStringMessage<T extends Message> {
  private final ByteString byteString;
  private final T message;

  /**
   * Create a {@code ByteStringMessage} from a prototype of the message and its {@code ByteString}
   * representation.
   *
   * @param prototype to create the {@code Message} from
   * @param byteString representation of the message
   * @throws InvalidProtocolBufferException if byteString is not a valid protocol buffer
   */
  public static <K extends Message> ByteStringMessage<K> from(K prototype, ByteString byteString)
      throws InvalidProtocolBufferException {
    return new ByteStringMessage<K>(prototype, byteString);
  }

  /**
   * Create a {@code ByteStringMessage} from the message itself.  This should only ever be used when
   * committing the serialised version of a message, otherwise it defeats the purpose of this class.
   *
   * @param message to form the serialised version of
   */
  @SuppressWarnings("unchecked")
  public static <K extends Message> ByteStringMessage<K> fromMessage(K message) {
    try {
      return (ByteStringMessage<K>) from(
          message.getDefaultInstanceForType(), message.toByteString());
    } catch (InvalidProtocolBufferException e) {
      // This cannot possibly fail, unless the protocol buffer library is broken
      throw new AssertionError(e);
    }
  }

  @SuppressWarnings("unchecked")
  private ByteStringMessage(T prototype, ByteString byteString)
      throws InvalidProtocolBufferException {
    this.message = (T) prototype.newBuilderForType().mergeFrom(byteString).build();
    this.byteString = byteString;
  }

  /**
   * @return the immutable underlying {@code Message}
   */
  public T getMessage() {
    return message;
  }

  /**
   * @return the serialised representation of this message
   */
  public ByteString getByteString() {
    return this.byteString;
  }

  /**
   * @return the serialised byte array representation of this message
   */
  public byte[] getByteArray() {
    return this.byteString.toByteArray();
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ByteStringMessage)) {
      return false;
    } else {
      ByteStringMessage<Message> bsm = (ByteStringMessage<Message>) o;
      return byteString.equals(bsm.byteString);
    }
  }

  @Override
  public int hashCode() {
    return byteString.hashCode();
  }

  @Override
  public String toString() {
    return "ByteStringMessage: " + message;
  }
}
