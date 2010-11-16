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
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.waveprotocol.box.server.account.AccountData;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.persistence.protos.ProtoAccountDataSerializer;
import org.waveprotocol.box.server.persistence.protos.ProtoAccountStoreData.ProtoAccountData;
import org.waveprotocol.box.server.util.Log;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

/**
 * A flat file based implementation of {@link AccountStore}
 * 
 * @author tad.glines@gmail.com (Tad Glines)
 */
public class FileAccountStore implements AccountStore {
  private static final String ACCOUNT_FILE_EXTENSION = ".account";
  private final String accountStoreBasePath;
  private final Map<ParticipantId, AccountData> accounts = Maps.newHashMap();

  private static final Log LOG = Log.get(FileAccountStore.class);
  
  @Inject
  public FileAccountStore(@Named("account_store_directory") String accountStoreBasePath) {
    Preconditions.checkNotNull(accountStoreBasePath, "Requested path is null");
    this.accountStoreBasePath = accountStoreBasePath;
  }
  
  @Override
  public AccountData getAccount(ParticipantId id) {
    synchronized (accounts) {
      AccountData account = accounts.get(id);
      if (account == null) {
        account = readAccount(id);
        if (account != null) {
          accounts.put(id, account);
        }
      }
      return account;
    }
  }

  @Override
  public void putAccount(AccountData account) {
    synchronized (accounts) {
      Preconditions.checkNotNull(account);
      writeAccount(account);
      accounts.put(account.getId(), account);
    }
  }

  @Override
  public void removeAccount(ParticipantId id) {
    synchronized (accounts) {
      File file = new File(participantIdToFileName(id));
      if (file.exists()) {
        file.delete();
      }
      accounts.remove(id);
    }
  }

  private String participantIdToFileName(ParticipantId id) {
    return accountStoreBasePath + File.separator + id.getAddress().toLowerCase()
        + ACCOUNT_FILE_EXTENSION;
  }
  
  private AccountData readAccount(ParticipantId id) {
    FileInputStream file = null;
    try {
      File accountFile = new File(participantIdToFileName(id));
      if (!accountFile.exists()) {
        return null;
      }
      file = new FileInputStream(accountFile);
      ProtoAccountData data = ProtoAccountData.newBuilder().mergeFrom(file).build();
      return ProtoAccountDataSerializer.deserialize(data);
    } catch (IOException e) {
      LOG.severe("Failed to read account data from disk!", e);
      throw new RuntimeException(e);
    } finally {
      if (file != null) {
        try {
          file.close();
        } catch (IOException e) {
          LOG.warning("Failed to close account data file!", e);
        }
      }
    }
  }
  
  private void writeAccount(AccountData account) {
    OutputStream file = null;
    try {
      File accountFile = new File(participantIdToFileName(account.getId()));
      file = new FileOutputStream(accountFile);
      ProtoAccountData data = ProtoAccountDataSerializer.serialize(account);
      file.write(data.toByteArray());
      file.flush();
    } catch (IOException e) {
      LOG.severe("Failed to write account data to disk!", e);
      throw new RuntimeException(e);
    } finally {
      if (file != null) {
        try {
          file.close();
        } catch (IOException e) {
          LOG.warning("Failed to close account data file!", e);
        }
      }
    }
  }
}
