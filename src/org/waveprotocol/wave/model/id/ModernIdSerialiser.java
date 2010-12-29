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

package org.waveprotocol.wave.model.id;

import com.google.common.base.Preconditions;


/**
 * Id serialiser for spec-conforming identifiers.
 *
 * This serialiser throws a runtime exception if an attempt is made to serialise
 * a non-conforming id. This check may be removed once it's no longer possible
 * to construct one.
 *
 * @author anorth@google.com (Alex North)
 */
public class ModernIdSerialiser implements IdSerialiser {

  public static final ModernIdSerialiser INSTANCE = new ModernIdSerialiser();
  private static final String SEP = "/";

  @Override
  public String serialiseWaveId(WaveId id) {
    Preconditions.checkArgument(WaveIdentifiers.isValidDomain(0, id.getDomain()), "Invalid domain");
    Preconditions.checkArgument(WaveIdentifiers.isValidIdentifier(id.getId()), "Invalid id");
    return new StringBuilder(id.getDomain()).append(SEP).append(id.getId()).toString();
  }

  @Override
  public String serialiseWaveletId(WaveletId id) {
    Preconditions.checkArgument(WaveIdentifiers.isValidDomain(0, id.getDomain()), "Invalid domain");
    Preconditions.checkArgument(WaveIdentifiers.isValidIdentifier(id.getId()), "Invalid id");
    return new StringBuilder(id.getDomain()).append(SEP).append(id.getId()).toString();
  }

  @Override
  public WaveId deserialiseWaveId(String serialisedForm) throws InvalidIdException {
    String[] tokens = serialisedForm.split(SEP);
    if (tokens.length != 2) {
      throw new InvalidIdException(serialisedForm,
          "Required 2 /-separated tokens in serialised wave id, found " + tokens.length);
    }
    return WaveId.ofChecked(tokens[0], tokens[1]);
  }

  @Override
  public WaveletId deserialiseWaveletId(String serialisedForm) throws InvalidIdException {
    String[] tokens = serialisedForm.split(SEP);
    if (tokens.length != 2) {
      throw new InvalidIdException(serialisedForm,
          "Required 2 /-separated tokens in serialised wavelet id, found " + tokens.length);
    }
    return WaveletId.ofChecked(tokens[0], tokens[1]);
  }

  private ModernIdSerialiser() {
  }
}
