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

package org.waveprotocol.box.server.persistence.file;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.apache.commons.codec.binary.Hex;
import org.waveprotocol.box.server.util.Log;
import org.waveprotocol.wave.crypto.CertPathStore;
import org.waveprotocol.wave.crypto.DefaultCertPathStore;
import org.waveprotocol.wave.crypto.SignatureException;
import org.waveprotocol.wave.crypto.SignerInfo;
import org.waveprotocol.wave.federation.Proto.ProtocolSignerInfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * A flat file based implementation of {@link CertPathStore}
 * 
 * @author tad.glines@gmail.com (Tad Glines)
 */
public class FileCertPathStore implements CertPathStore {
  private static final String SIGNER_FILE_EXTENSION = ".signer";
  private final String certPathStoreBasePath;
  private final CertPathStore certPathStore = new DefaultCertPathStore();

  private static final Log LOG = Log.get(FileCertPathStore.class);
  
  @Inject
  public FileCertPathStore(@Named("cert_path_store_directory") String certPathStoreBasePath) {
    Preconditions.checkNotNull(certPathStoreBasePath, "Requested path is null");
    this.certPathStoreBasePath = certPathStoreBasePath;
  }

  private String signerIdToFileName(byte[] id) {
    return certPathStoreBasePath + File.separator + new String(Hex.encodeHex(id))
        + SIGNER_FILE_EXTENSION;
  }

  @Override
  public SignerInfo getSignerInfo(byte[] signerId) {
    synchronized(certPathStore) {
      SignerInfo signerInfo = certPathStore.getSignerInfo(signerId);
      if (signerInfo == null) {
        File signerFile = new File(signerIdToFileName(signerId));
        if (signerFile.exists()) {
          FileInputStream file = null;
          try {
            file = new FileInputStream(signerFile);
            ProtocolSignerInfo data = ProtocolSignerInfo.newBuilder().mergeFrom(file).build();
            signerInfo = new SignerInfo(data);
          } catch (SignatureException e) {
            LOG.severe("Failed to parse signer info from file!", e);
            throw new RuntimeException(e);
          } catch (FileNotFoundException e) {
            LOG.severe("Failed to open signer info file!", e);
            throw new RuntimeException(e);
          } catch (IOException e) {
            LOG.severe("Failed to read/parse signer info file!", e);
            throw new RuntimeException(e);
          } finally {
            if (file != null) {
              try {
                file.close();
              } catch (IOException e) {
                LOG.severe("Failed to close signer info file!", e);
              }
            }
          }
        }
      }
      return signerInfo;
    }
  }

  @Override
  public void putSignerInfo(ProtocolSignerInfo protoSignerInfo) throws SignatureException {
    synchronized(certPathStore) {
      SignerInfo signerInfo = new SignerInfo(protoSignerInfo);
      File signerFile = new File(signerIdToFileName(signerInfo.getSignerId()));
      FileOutputStream file = null;
      try {
        file = new FileOutputStream(signerFile);
        file.write(protoSignerInfo.toByteArray());
        file.flush();
        certPathStore.putSignerInfo(protoSignerInfo);
      } catch (IOException e) {
        LOG.severe("Failed to write signer info file!", e);
        throw new RuntimeException(e);
      } finally {
        if (file != null) {
          try {
            file.close();
          } catch (IOException e) {
            LOG.severe("Failed to close signer info file!", e);
          }
        }
      }
    }
  }
}
