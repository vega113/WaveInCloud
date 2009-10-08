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

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.waveprotocol.wave.crypto.SignatureException;
import org.waveprotocol.wave.crypto.WaveSigner;
import org.waveprotocol.wave.crypto.WaveSignerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;

/**
 * Guice provider of WaveSigners.
 */
@Singleton
public class WaveSignerProvider implements Provider<WaveSigner> {

  private static final FileOpener FILE_OPENER = new FileOpener();

  private final WaveSigner signer;

  /**
   * Public constructor.
   * @param privateKey file name that has the PKCS#8-PEM-encoded private key.
   * @param certs list of file names that have the certificates of this signer.
   *   The first file name must have the signer's target certificate. The
   *   certificates can be DER or PEM encoded.
   * @param domain the domain for which the certificate was issued.
   * @param factory A {@link WaveSignerFactory}.
   */
  @Inject
  public WaveSignerProvider(
      @Named("certificate_private_key") String privateKey,
      @Named("certificate_files") String certs,
      @Named("certificate_domain") String domain,
      WaveSignerFactory factory) {

    FileInputStream privateKeyStream;
    try {
      privateKeyStream = new FileInputStream(privateKey);
    } catch (FileNotFoundException e) {
      throw new ProvisionException("could not read private key", e);
    }

    Iterable<FileInputStream> certStreams =
        Iterables.transform(Arrays.asList(certs.split(",")), FILE_OPENER);

    try {
      signer = factory.getSigner(privateKeyStream, certStreams, domain);
    } catch (SignatureException e) {
      throw new ProvisionException("could not make wave signer", e);
    }
  }

  @Override
  public WaveSigner get() {
    return signer;
  }

  // Function that turns file names into FileInputStreams
  private static class FileOpener implements Function<String, FileInputStream> {

    @Override
    public FileInputStream apply(String filename) {
      try {
        return new FileInputStream(filename);
      } catch (FileNotFoundException e) {
        throw new ProvisionException("could not read certificates", e);
      }
    }
  }
}
