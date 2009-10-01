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

package org.waveprotocol.wave.examples.fedone.common;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.protobuf.ByteString;

import org.waveprotocol.wave.model.document.operation.AnnotationBoundaryMap;
import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.AttributesUpdate;
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.DocOpCursor;
import org.waveprotocol.wave.model.document.operation.impl.AttributesImpl;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.NoOp;
import org.waveprotocol.wave.model.operation.wave.RemoveParticipant;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletDocumentOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.protocol.common.ProtocolDocumentOperation;
import org.waveprotocol.wave.protocol.common.ProtocolHashedVersion;
import org.waveprotocol.wave.protocol.common.ProtocolWaveletDelta;
import org.waveprotocol.wave.protocol.common.ProtocolWaveletOperation;

import java.util.List;
import java.util.Map;

/**
 * Utility class for serialising/deserialising wavelet operations (and their components) to/from
 * their protocol buffer representations (and their components).
 *
 *
 */
public class WaveletOperationSerializer {
  private WaveletOperationSerializer() {
  }

  /**
   * Serialize a {@link WaveletDelta} as a {@link ProtocolWaveletDelta} at a
   * specific version.
   *
   * @param waveletDelta to serialize
   * @param version version at which the delta applies
   * @return serialized protocol buffer wavelet delta
   */
  public static ProtocolWaveletDelta serialize(WaveletDelta waveletDelta, HashedVersion version) {
    ProtocolWaveletDelta.Builder protobufDelta = ProtocolWaveletDelta.newBuilder();

    for (WaveletOperation waveletOp : waveletDelta.getOperations()) {
      protobufDelta.addOperation(serialize(waveletOp));
    }

    protobufDelta.setAuthor(waveletDelta.getAuthor().getAddress());
    protobufDelta.setHashedVersion(serialize(version));
    return protobufDelta.build();
  }

  /**
   * Serialize a {@link WaveletOperation} as a {@link ProtocolWaveletOperation}.
   *
   * @param waveletOp wavelet operation to serialize
   * @return serialized protocol buffer wavelet operation
   */
  public static ProtocolWaveletOperation serialize(WaveletOperation waveletOp) {
    ProtocolWaveletOperation.Builder protobufOp = ProtocolWaveletOperation.newBuilder();

    if (waveletOp instanceof NoOp) {
      protobufOp.setNoOp(true);
    } else if (waveletOp instanceof AddParticipant) {
      protobufOp.setAddParticipant(
          ((AddParticipant) waveletOp).getParticipantId().getAddress());
    } else if (waveletOp instanceof RemoveParticipant) {
      protobufOp.setRemoveParticipant(
          ((RemoveParticipant) waveletOp).getParticipantId().getAddress());
    } else if (waveletOp instanceof WaveletDocumentOperation) {
      ProtocolWaveletOperation.MutateDocument.Builder mutation =
        ProtocolWaveletOperation.MutateDocument.newBuilder();
      mutation.setDocumentId(((WaveletDocumentOperation) waveletOp).getDocumentId());
      mutation.setDocumentOperation(
          serialize(((WaveletDocumentOperation) waveletOp).getOperation()));
      protobufOp.setMutateDocument(mutation.build());
    } else {
      throw new IllegalArgumentException("Unsupported operation type: " + waveletOp);
    }

    return protobufOp.build();
  }

  /**
   * Serialize a {@link DocOp} as a {@link ProtocolDocumentOperation}.
   *
   * @param inputOp document operation to serialize
   * @return serialized protocol buffer document operation
   */
  public static ProtocolDocumentOperation serialize(DocOp inputOp) {
    final ProtocolDocumentOperation.Builder output = ProtocolDocumentOperation.newBuilder();

    inputOp.apply(new DocOpCursor() {
      private ProtocolDocumentOperation.Component.Builder newComponentBuilder() {
        return ProtocolDocumentOperation.Component.newBuilder();
      }

      @Override public void retain(int itemCount) {
        output.addComponent(newComponentBuilder().setRetainItemCount(itemCount).build());
      }

      @Override public void characters(String characters) {
        output.addComponent(newComponentBuilder().setCharacters(characters).build());
      }

      @Override public void deleteCharacters(String characters) {
        output.addComponent(newComponentBuilder().setDeleteCharacters(characters).build());
      }

      @Override public void elementStart(String type, Attributes attributes) {
        ProtocolDocumentOperation.Component.ElementStart e = makeElementStart(type, attributes);
        output.addComponent(newComponentBuilder().setElementStart(e).build());
      }

      @Override public void deleteElementStart(String type, Attributes attributes) {
        ProtocolDocumentOperation.Component.ElementStart e = makeElementStart(type, attributes);
        output.addComponent(newComponentBuilder().setDeleteElementStart(e).build());
      }

      private ProtocolDocumentOperation.Component.ElementStart makeElementStart(
          String type, Attributes attributes) {
        ProtocolDocumentOperation.Component.ElementStart.Builder e =
          ProtocolDocumentOperation.Component.ElementStart.newBuilder();

        e.setType(type);

        for (String name : attributes.keySet()) {
          e.addAttribute(ProtocolDocumentOperation.Component.KeyValuePair.newBuilder()
              .setKey(name).setValue(attributes.get(name)).build());
        }

        return e.build();
      }

      @Override public void elementEnd() {
        output.addComponent(newComponentBuilder().setElementEnd(true).build());
      }

      @Override public void deleteElementEnd() {
        output.addComponent(newComponentBuilder().setDeleteElementEnd(true).build());
      }

      @Override public void replaceAttributes(Attributes oldAttributes, Attributes newAttributes) {
        throw new IllegalArgumentException();
      }

      @Override public void updateAttributes(AttributesUpdate attributes) {
        throw new IllegalArgumentException();
      }

      @Override public void annotationBoundary(AnnotationBoundaryMap map) {
        throw new IllegalArgumentException();
      }
    });

    return output.build();
  }

  /**
   * Deserializes a {@link ProtocolWaveletDelta} as a {@link WaveletDelta} and
   * {@link HashedVersion}.
   *
   * @param delta protocol buffer wavelet delta to deserialize
   * @return deserialized wavelet delta and version
   */
  public static Pair<WaveletDelta, HashedVersion> deserialize(ProtocolWaveletDelta delta) {
    List<WaveletOperation> ops = Lists.newArrayList();

    for (ProtocolWaveletOperation op : delta.getOperationList()) {
      ops.add(deserialize(op));
    }

    HashedVersion hashedVersion = deserialize(delta.getHashedVersion());
    return Pair.of(new WaveletDelta(new ParticipantId(delta.getAuthor()), ops), hashedVersion);
  }

  /** Deserializes a protobuf to a HashedVersion POJO. */
  public static HashedVersion deserialize(ProtocolHashedVersion hashedVersion) {
    return new HashedVersion(hashedVersion.getVersion(),
        hashedVersion.getHistoryHash().toByteArray());
  }

  /** Serializes a HashedVersion POJO to a protobuf. */
  public static ProtocolHashedVersion serialize(HashedVersion hashedVersion) {
    return ProtocolHashedVersion.newBuilder().setVersion(hashedVersion.getVersion()).
      setHistoryHash(ByteString.copyFrom(hashedVersion.getHistoryHash())).build();
  }

  /**
   * Deserialize a {@link ProtocolWaveletOperation} as a {@link WaveletOperation}.
   *
   * @param protobufOp protocol buffer wavelet operation to deserialize
   * @return deserialized wavelet operation
   */
  public static WaveletOperation deserialize(ProtocolWaveletOperation protobufOp) {
    if (protobufOp.hasNoOp()) {
      return new NoOp();
    } else if (protobufOp.hasAddParticipant()) {
      return new AddParticipant(new ParticipantId(protobufOp.getAddParticipant()));
    } else if (protobufOp.hasRemoveParticipant()) {
      return new RemoveParticipant(new ParticipantId(protobufOp.getRemoveParticipant()));
    } else if (protobufOp.hasMutateDocument()) {
      return new WaveletDocumentOperation(
          protobufOp.getMutateDocument().getDocumentId(),
          deserialize(protobufOp.getMutateDocument().getDocumentOperation()));
    } else {
      throw new IllegalArgumentException("Unsupported operation: " + protobufOp);
    }
  }

  /**
   * Deserialize a {@link ProtocolDocumentOperation} into a {@link DocOp}.
   *
   * @param op protocol buffer document operation to deserialize
   * @return deserialized DocOp
   */
  public static BufferedDocOp deserialize(ProtocolDocumentOperation op) {
    DocOpBuilder output = new DocOpBuilder();

    for (ProtocolDocumentOperation.Component c : op.getComponentList()) {
      if (c.hasCharacters()) {
        output.characters(c.getCharacters());
      } else if (c.hasElementStart()) {
        Map<String, String> attributesMap = Maps.newHashMap();
        for (ProtocolDocumentOperation.Component.KeyValuePair pair :
            c.getElementStart().getAttributeList()) {
          attributesMap.put(pair.getKey(), pair.getValue());
        }
        output.elementStart(c.getElementStart().getType(), new AttributesImpl(attributesMap));
      } else if (c.hasElementEnd()) {
        output.elementEnd();
      } else if (c.hasRetainItemCount()) {
        output.retain(c.getRetainItemCount());
      } else if (c.hasDeleteCharacters()) {
        output.deleteCharacters(c.getDeleteCharacters());
      } else if (c.hasDeleteElementStart()) {
        Map<String, String> attributesMap = Maps.newHashMap();
        for (ProtocolDocumentOperation.Component.KeyValuePair pair :
            c.getDeleteElementStart().getAttributeList()) {
          attributesMap.put(pair.getKey(), pair.getValue());
        }
        output.deleteElementStart(c.getDeleteElementStart().getType(),
            new AttributesImpl(attributesMap));
      } else if (c.hasDeleteElementEnd()) {
        output.deleteElementEnd();
      } else {
        throw new IllegalArgumentException("Unsupported operation component: " + c);
      }
    }

    return output.build();
  }
}
