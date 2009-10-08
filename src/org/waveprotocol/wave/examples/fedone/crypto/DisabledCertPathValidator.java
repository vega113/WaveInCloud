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

package org.waveprotocol.wave.examples.fedone.crypto;

import java.security.cert.X509Certificate;
import java.util.List;

/**
 * A {@link WaveCertPathValidator} that doesn't do any validation.
 */
public class DisabledCertPathValidator implements WaveCertPathValidator {

  @Override
  public void validate(List<? extends X509Certificate> certs) throws SignatureException {
    // Pass
  }

}
