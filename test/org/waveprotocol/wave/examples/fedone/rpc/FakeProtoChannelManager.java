/*
 * Copyright (C) 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.waveprotocol.wave.examples.fedone.rpc;

import com.google.protobuf.Message;
import com.google.protobuf.UnknownFieldSet;

import java.nio.channels.ByteChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * A tiny manager for a SequencedProtoChannel that allows incoming messages to
 * be waited for and returned cleanly.
 * 
 *
 */
public class FakeProtoChannelManager {
  public final SequencedProtoChannel channel;
  private final BlockingQueue<SequencedObject<?>> queue =
      new LinkedBlockingQueue<SequencedObject<?>>();
  
  /**
   * A container class for a sequence numbed and protocol buffer message pair.
   */
  public static class SequencedObject<V> {
    final public long sequenceNo;
    final public V message;
    
    private SequencedObject(long sequenceNo, V message) {
      this.sequenceNo = sequenceNo;
      this.message = message;
    }

    /**
     * Helper method to instantiate new SequencedObject instances.
     * @param <T>
     */
    public static <T> SequencedObject<T> of(long sequenceNo, T message) {
      return new SequencedObject<T>(sequenceNo, message);
    }

    @Override
    public boolean equals(Object thatObject) {
      if (!(thatObject instanceof SequencedObject)) {
        return false;
      } else {
        SequencedObject<?> that = (SequencedObject<?>) thatObject;
        return that.sequenceNo == this.sequenceNo && that.message.equals(this.message);
      }
    }

    @Override
    public int hashCode() {
      return (31*17 + (int) sequenceNo) * 31 + message.hashCode();
    }
  }

  public FakeProtoChannelManager(ByteChannel channel) {
    this.channel = new SequencedProtoChannel(channel, new ProtoCallback() {
      @Override
      public void message(long sequenceNo, Message message) {
        queue.add(SequencedObject.of(sequenceNo, message));
      }

      @Override
      public void unknown(long sequenceNo, String messageType, UnknownFieldSet message) {
        queue.add(SequencedObject.of(sequenceNo, message));
      }

      @Override
      public void unknown(long sequenceNo, String messageType, String message) {
        queue.add(SequencedObject.of(sequenceNo, message));
      }
    });
    this.channel.startAsyncRead();
  }

  /**
   * Wait for any incoming message and return it. If this is called from
   * multiple threads, each thread will be handed a different message from the
   * queue.
   * 
   * @param timeout the timeout, in seconds, to wait for messages
   * @return an incoming message, or null if none were found
   */
  public SequencedObject<?> waitForMessage(int timeout) {
    try {
      return queue.poll(timeout, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      throw new IllegalStateException("Operation should not be interrupted", e);
    }
  }
}
