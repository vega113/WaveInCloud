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

package org.waveprotocol.wave.examples.fedone.rpc;

import com.google.common.collect.Maps;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Message;
import com.google.protobuf.UnknownFieldSet;

import org.waveprotocol.wave.examples.fedone.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Create a two-way channel for protocol buffer exchange. Enhances this exchange
 * with metadata in the form of sequence numbers.
 * 
 *
 */
public class SequencedProtoChannel {
  private static final Log LOG = Log.get(SequencedProtoChannel.class);
  private final Map<String, Message> expectedMessages = Maps.newHashMap();
  private final CodedOutputStream outputStream;
  private final ByteChannel channel;
  private final ExecutorService threadPool;
  private final Runnable asyncRead;
  private boolean isReading = false;
  
  public interface ProtoCallback {
    public void message(long sequenceNo, Message message);
    public void unknown(long sequenceNo, String messageType, UnknownFieldSet message);
  }

  /**
   * Internal helper method to remove and return the specified number of bytes
   * from the beginning of the specified ByteBuffer.
   * 
   * @param buffer the ByteBuffer instance to remove data from
   * @param size the number of bytes requested
   * @return an array of length exactly equal to size, the bytes at the start of
   *         the buffer which have now been removed - or null if the buffer
   *         contained less than this number of bytes
   */
  private static byte[] popFromBuffer(ByteBuffer buffer, int size) {
    if (buffer.position() < size) {
      return null;
    } else {
      byte[] result = new byte[size];
      buffer.flip();
      buffer.get(result);
      buffer.compact();
      return result;
    }
  }

  /**
   * Instantiate a new SequencedProtoChannel. Requires the backing SocketChannel
   * as well as the ProtoCallback to be notified on incoming messages.
   * 
   * @param channel the backing ByteChannel, which must be blocking
   * @param callback the callback for incoming known and unknown messages
   * @param threadPool the service used to create threads
   */
  public SequencedProtoChannel(final ByteChannel channel, final ProtoCallback callback,
      ExecutorService threadPool) {
    this.channel = channel;
    this.threadPool = threadPool;

    this.asyncRead = new Runnable() {
      @Override
      public void run() {
        final int bufferSize = 8192 * 4;
        ByteBuffer inputBuffer = ByteBuffer.allocate(bufferSize);
        int requiredSize = -1;
        try {
          // we don't have enough data - read from buffer
          while (-1 != channel.read(inputBuffer)) {
            
            // While there's still data available, try to process it.
            while (inputBuffer.position() > 0) {

              // Grab our requiredSize if we still need it, popping a 32-bit int.
              if (requiredSize == -1) {
                byte[] buffer = popFromBuffer(inputBuffer, CodedOutputStream.LITTLE_ENDIAN_32_SIZE);
                if (buffer != null) {
                  requiredSize = CodedInputStream.newInstance(buffer).readRawLittleEndian32();
                } else {
                  // not enough data - fall out
                  break;
                }
              }
  
              // Try to grab the whole payload.
              if (requiredSize > bufferSize) {
                throw new IllegalStateException(String.format("Payload (%d bytes) too large for" +
                		" buffer (%d bytes)", requiredSize, bufferSize));
              } else if (requiredSize > -1) {
                byte[] buffer = popFromBuffer(inputBuffer, requiredSize);
                if (buffer != null) {
                  CodedInputStream inputStream = CodedInputStream.newInstance(buffer);
                  long incomingSequenceNo = inputStream.readInt64();
                  String messageType = inputStream.readString();
                  Message prototype = expectedMessages.get(messageType);
                  if (prototype == null) {
                    LOG.info("Received misunderstood message (??? " + messageType + " ???, seq "
                        + incomingSequenceNo + ") from: " + channel);
                    // We have to emulate some of the semantics of reading a
                    // whole message here, including reading its encoded length.
                    final int length = inputStream.readRawVarint32();
                    final int oldLimit = inputStream.pushLimit(length);
                    UnknownFieldSet unknownFieldSet = UnknownFieldSet.parseFrom(inputStream);
                    inputStream.popLimit(oldLimit);
                    callback.unknown(incomingSequenceNo, messageType, unknownFieldSet);
                  } else {
                    // TODO: change to LOG.debug
                    LOG.fine("Received message (" + messageType + ", seq "
                        + incomingSequenceNo + ") from: " + channel);
                    Message.Builder builder = prototype.newBuilderForType();
                    inputStream.readMessage(builder, null);
                    callback.message(incomingSequenceNo, builder.build());
                  }
  
                  // Reset the required size for the next invocation of this loop.
                  requiredSize = -1;
                } else {
                  // not enough data - fall out
                  break;
                }
              }
            }
          }
        } catch (IOException e) {
          // TODO: error case.
          e.printStackTrace();
        }
      }
    };

    outputStream = CodedOutputStream.newInstance(new OutputStream() {
      @Override
      public void write(int b) throws IOException {
        channel.write(ByteBuffer.wrap(new byte[] {(byte) b}));
      }
    });
  }

  /**
   * Create a new SequencedProtoChannel with a default thread executor. See
   * {@link #SequencedProtoChannel(ByteChannel, ProtoCallback, ExecutorService)}.
   */
  public SequencedProtoChannel(ByteChannel channel, ProtoCallback callback) {
    this(channel, callback, Executors.newSingleThreadExecutor());
  }

  /**
   * Kick off this class's asynchronous read method. Must be called to receive
   * any messages on the callback.
   */
  public void startAsyncRead() {
    if (isReading) {
      throw new IllegalStateException("This protoChannel is already reading asynchronously.");
    }
    threadPool.execute(asyncRead);
    isReading = true;
  }
  
  /**
   * Register an expected incoming message type.
   * 
   * @param messagePrototype the prototype of the expected type
   */
  public void expectMessage(Message messagePrototype) {
    expectedMessages.put(messagePrototype.getDescriptorForType().getFullName(), messagePrototype);
  }

  /**
   * Send the given message across the connection along with the sequence number.
   * 
   * @param sequenceNo
   * @param message
   */
  public void sendMessage(long sequenceNo, Message message) {
    String messageType = message.getDescriptorForType().getFullName();
    int size = CodedOutputStream.computeInt64SizeNoTag(sequenceNo)
            + CodedOutputStream.computeStringSizeNoTag(messageType)
            + CodedOutputStream.computeMessageSizeNoTag(message);
    // TODO: change to LOG.debug
    LOG.fine("Sending message (" + message.getDescriptorForType().getFullName() + ", seq "
        + sequenceNo + ") to: " + channel);
    // Only one message should be written at at time.
    synchronized (outputStream) {
      try {
        // TODO: turn this into a data structure which can read/write itself
        outputStream.writeRawLittleEndian32(size); // i.e., not including itself
        outputStream.writeInt64NoTag(sequenceNo);
        outputStream.writeStringNoTag(messageType);
        outputStream.writeMessageNoTag(message);
        outputStream.flush();
      } catch (IOException e) {
        throw new IllegalStateException(e);
      }
    }
  }

  /**
   * Helper method around {{@link #sendMessage(long, Message)} which
   * automatically registers the response type as an expected input to this
   * SequencedProtoChannel.
   * 
   * @param sequenceNo
   * @param message
   * @param expectedResponsePrototype
   */
  public void sendMessage(long sequenceNo, Message message, Message expectedResponsePrototype) {
    expectMessage(expectedResponsePrototype);
    sendMessage(sequenceNo, message);
  }
}
