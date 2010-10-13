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

package org.waveprotocol.box.server.rpc;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import com.google.protobuf.UnknownFieldSet;

import org.waveprotocol.box.server.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Create a two-way channel for protocol buffer exchange. Enhances this exchange
 * with metadata in the form of sequence numbers.
 * 
 *
 */
public class SequencedProtoChannel extends MessageExpectingChannel {
  // The default ProtoBuffer message size limit.
  private static final int MAX_MESSAGE_SIZE = 64 << 20; //64MB

  private static final Log LOG = Log.get(SequencedProtoChannel.class);
  private final CodedOutputStream outputStream;
  private final ByteChannel channel;
  private final ExecutorService threadPool;
  private final Runnable asyncRead;
  private boolean isReading = false;
  
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

    /*
     * This class wraps {@code channel} and provides an InputStream interface that can be used
     * by {@link CodedInputStream}.
     */
    class CustomInputStream extends InputStream {
      int totalRemaining = 0;

      public void setLimit(int limit) {
        totalRemaining = limit;
      }
      /*
       * Keep calling channel.read until all requested bytes are returned or EOF is reached.
       * If channel.read throws an exception and some bytes have already been read, then the
       * number of bytes read will be returned, otherwise the exception will be re-thrown.
       */
      private int readAll(ByteBuffer b) throws IOException {
        int total = 0;
        int nread = 0;
        while (b.remaining() > 0) {
          nread = 0;
          
          try {
            nread = channel.read(b);

            if (nread == 0) {
              nread = -1;
            }
          } catch (IOException e) {
            // If there are no bytes to return re-throw the exception
            if (total == 0) {
              throw e;
            } else {
              nread = -1;
            }
          }

          if (nread != -1) {
            total += nread;
          } else {
            break;
          }
        }

        totalRemaining -= total;
        
        return nread != -1 ? total : -1;
      }
      
      @Override
      public int read() throws IOException {
        if (totalRemaining <= 0) return -1;

        ByteBuffer b = ByteBuffer.allocate(1);
        if (channel.read(b) != 1) {
          return -1;
        }

        return b.get();
      }

      @Override
      public int read(byte[] b, int off, int len) throws IOException {
        if (totalRemaining <= 0) return -1;
        return readAll(ByteBuffer.wrap(b, off, len < totalRemaining ? len : totalRemaining));
      }

      @Override
      public int read(byte[] b) throws IOException {
        if (totalRemaining <= 0) return -1;
        return readAll(ByteBuffer.wrap(b, 0,
            b.length < totalRemaining ? b.length : totalRemaining));
      }
    }
    
    final CustomInputStream inputStream = new CustomInputStream();
    final CodedInputStream codedInputStream = CodedInputStream.newInstance(inputStream);
    

    this.asyncRead = new Runnable() {
      @Override
      public void run() {
        int requiredSize = -1;
        try {
          while(true) {
            inputStream.setLimit(CodedOutputStream.LITTLE_ENDIAN_32_SIZE);
            codedInputStream.resetSizeCounter();
            codedInputStream.setSizeLimit(CodedOutputStream.LITTLE_ENDIAN_32_SIZE);
            requiredSize = codedInputStream.readRawLittleEndian32();
            if (requiredSize > MAX_MESSAGE_SIZE) {
              throw new IllegalStateException(String.format("Reported payload size (%d bytes) " +
                  " exeeds the limit (%d bytes)", requiredSize, MAX_MESSAGE_SIZE));
            }
            if (requiredSize > 0) {
              inputStream.setLimit(requiredSize);
              codedInputStream.resetSizeCounter();
              codedInputStream.setSizeLimit(requiredSize);
              long incomingSequenceNo = codedInputStream.readInt64();
              String messageType = codedInputStream.readString();
                  Message prototype = getMessagePrototype(messageType);
              if (prototype == null) {
                LOG.info("Received misunderstood message (??? " + messageType + " ???, seq "
                    + incomingSequenceNo + ") from: " + channel);
                // We have to emulate some of the semantics of reading a
                // whole message here, including reading its encoded length.
                final int length = codedInputStream.readRawVarint32();
                final int oldLimit = codedInputStream.pushLimit(length);
                UnknownFieldSet unknownFieldSet = UnknownFieldSet.parseFrom(codedInputStream);
                codedInputStream.popLimit(oldLimit);
                callback.unknown(incomingSequenceNo, messageType, unknownFieldSet);
              } else {
                // TODO: change to LOG.debug
                LOG.fine("Received message (" + messageType + ", seq "
                    + incomingSequenceNo + ") from: " + channel);
                Message.Builder builder = prototype.newBuilderForType();
                codedInputStream.readMessage(builder, null);
                callback.message(incomingSequenceNo, builder.build());
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

      @Override
      public void write(byte[] buf) throws IOException {
        channel.write(ByteBuffer.wrap(buf));
      }

      @Override
      public void write(byte[] buf, int off, int len) throws IOException {
        channel.write(ByteBuffer.wrap(buf, off, len));
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
  @Override
  public void startAsyncRead() {
    if (isReading) {
      throw new IllegalStateException("This protoChannel is already reading asynchronously.");
    }
    threadPool.execute(asyncRead);
    isReading = true;
  }
  
  /**
   * Send the given message across the connection along with the sequence number.
   * 
   * @param sequenceNo
   * @param message
   */
  @Override
  public void sendMessage(long sequenceNo, Message message) {
    internalSendMessage(sequenceNo, message, message.getDescriptorForType().getFullName());
  }

  private void internalSendMessage(long sequenceNo, MessageLite message, String messageType) {
    int size = CodedOutputStream.computeInt64SizeNoTag(sequenceNo)
             + CodedOutputStream.computeStringSizeNoTag(messageType)
             + CodedOutputStream.computeMessageSizeNoTag(message);
    // TODO: change to LOG.debug
    LOG.fine("Sending message (" + messageType + ", seq " + sequenceNo + ") to: " + channel);
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
}
