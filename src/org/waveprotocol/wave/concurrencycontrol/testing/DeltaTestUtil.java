/**
 * Copyright 2008 Google Inc.
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

package org.waveprotocol.wave.concurrencycontrol.testing;

import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.BlipContentOperation;
import org.waveprotocol.wave.model.operation.wave.NoOp;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletBlipOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Arrays;
import java.util.List;


/**
 * A bunch of utility functions to make testing easier.
 *
 * @author zdwang@google.com (David Wang)
 */
public class DeltaTestUtil {
  private final ParticipantId creator;

  /**
   * Creates a {@link DeltaTestUtil} with which operations authored by the given
   * creator can readily be made.
   */
  public DeltaTestUtil(String creator) {
    this(new ParticipantId(creator));
  }

  /**
   * Creates a {@link TestUtil} with which operations authored by the given
   * creator can readily be made.
   */
  public DeltaTestUtil(ParticipantId creator) {
    this.creator = creator;
  }

  public ParticipantId getParticipant() {
    return creator;
  }

  /**
   * Creates an XmlDelete with the given data.
   */
  public WaveletOperation delete(int posStart, String characters, int remaining) {
    BufferedDocOp op = new DocOpBuilder()
        .retain(posStart)
        .deleteCharacters(characters)
        .retain(remaining)
        .build();
    BlipContentOperation blipOp = new BlipContentOperation(
        new WaveletOperationContext(creator, 0L, 1), op);
    WaveletBlipOperation waveOp = new WaveletBlipOperation("blip id", blipOp);
    return waveOp;
  }

  /**
   * Wrap an op with a delta.
   */
  public TransformedWaveletDelta delta(long targetVersion, WaveletOperation op) {
    return new TransformedWaveletDelta(creator, HashedVersion.unsigned(targetVersion + 1),
        0L, Arrays.asList(op));
  }

  /**
   * Creates an XmlInsert with the given data.
   */
  public WaveletOperation insert(int pos, String text, int remaining,
      HashedVersion resultingVersion) {
    return insert(pos, text, remaining, creator, resultingVersion);
  }

  /**
   * Create a delta with a single NoOp operation.
   *
   * @param initialVersion The version before the operation.
   */
  public TransformedWaveletDelta noOpDelta(long initialVersion) {
    return createNoOpDelta(1, HashedVersion.unsigned(initialVersion + 1));
  }

  /**
   * Create a NoOp operation.
   */
  public NoOp noOp() {
    return new NoOp(new WaveletOperationContext(creator, 0L, 1L));
  }

  /**
   * Create an AddParticipant operation.
   */
  public AddParticipant addParticipant(ParticipantId participant) {
    return new AddParticipant(new WaveletOperationContext(creator, 0L, 1L), participant);
  }

  /**
   * A docop that is empty. i.e. does nothing to the document. The document must
   * also be empty, otherwise the operation is invalid.
   */
  public WaveletOperation noOpDocOp(String blipId) {
    WaveletOperationContext context = new WaveletOperationContext(creator, 0L, 1L);
    BlipContentOperation blipOp = new BlipContentOperation(context, (new DocOpBuilder()).build());
    return new WaveletBlipOperation(blipId, blipOp);
  }


  /**
   * Create a delta with several NoOp operation.
   * @param initialVersion The version before the first operation.
   * @param numOps The number of NoOp operations to create.
   */
  public TransformedWaveletDelta createNoOpDelta(int numOps, HashedVersion resultingVersion) {
    List<WaveletOperation> ops = CollectionUtils.newArrayList();
    for (int i = 0; i < numOps; i++) {
      ops.add(noOp());
    }
    return new TransformedWaveletDelta(creator, resultingVersion, 0L, ops);
  }

  /**
   * Creates an XmlInsert with the given data.
   */
  public static WaveletOperation insert(int pos, String text, int remaining,
      ParticipantId participant, HashedVersion resultingVersion) {
    DocOpBuilder builder = new DocOpBuilder();
    builder.retain(pos).characters(text);
    if (remaining > 0) {
      builder.retain(remaining);
    }
    BlipContentOperation blipOp = new BlipContentOperation(
        new WaveletOperationContext(participant, 0L, 1, resultingVersion), builder.build());
    WaveletBlipOperation waveOp = new WaveletBlipOperation("blip id", blipOp);
    return waveOp;
  }
}
