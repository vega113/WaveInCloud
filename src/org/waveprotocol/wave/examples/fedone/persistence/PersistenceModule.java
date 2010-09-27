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

package org.waveprotocol.wave.examples.fedone.persistence;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.waveprotocol.wave.crypto.CertPathStore;
import org.waveprotocol.wave.examples.fedone.persistence.file.FileAttachmentStore;
import org.waveprotocol.wave.examples.fedone.persistence.memory.MemoryStore;
import org.waveprotocol.wave.examples.fedone.persistence.mongodb.MongoDbProvider;

/**
 * Module for setting up the different persistence stores.
 *
 *<p>
 * The valid names for the cert store are 'memory' and 'mongodb'
 *
 *<p>
 *The valid names for the attachment store are 'disk' and 'mongodb'
 *
 *<p>
 *The valid names for the account store are 'memory'.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 */
public class PersistenceModule extends AbstractModule {

  private final String certPathStoreType;

  private final String attachmentStoreType;

  private final String accountStoreType;

  private MongoDbProvider mongoDbProvider;

  @Inject
  public PersistenceModule(@Named("cert_path_store_type") String certPathStoreType,
      @Named("attachment_store_type") String attachmentStoreType,
      @Named("account_store_type") String accountStoreType) {
    this.certPathStoreType = certPathStoreType;
    this.attachmentStoreType = attachmentStoreType;
    this.accountStoreType = accountStoreType;
  }

  /**
   * Returns a {@link MongoDbProvider} instance.
   */
  public MongoDbProvider getMongoDbProvider() {
    if (mongoDbProvider == null) {
      mongoDbProvider = new MongoDbProvider();
    }
    return mongoDbProvider;
  }

  @Override
  protected void configure() {
    bindCertPathStore();
    bindAttachmentStore();
    bindAccountStore();
  }

  /**
   * Binds the CertPathStore implementation to the store specified in the
   * properties.
   */
  private void bindCertPathStore() {
    if (certPathStoreType.equalsIgnoreCase("memory")) {
      bind(CertPathStore.class).to(MemoryStore.class).in(Singleton.class);
    } else if (certPathStoreType.equalsIgnoreCase("mongodb")) {
      MongoDbProvider mongoDbProvider = getMongoDbProvider();
      bind(CertPathStore.class).toInstance(mongoDbProvider.provideMongoDbStore());
    } else {
      throw new RuntimeException(
          "Invalid certificate path store type: '" + certPathStoreType + "'");
    }
  }

  private void bindAttachmentStore() {
    if (attachmentStoreType.equalsIgnoreCase("disk")) {
      bind(AttachmentStore.class).to(FileAttachmentStore.class).in(Singleton.class);
    } else if (attachmentStoreType.equalsIgnoreCase("mongodb")) {
      MongoDbProvider mongoDbProvider = getMongoDbProvider();
      bind(AttachmentStore.class).toInstance(mongoDbProvider.provideMongoDbStore());
    } else {
      throw new RuntimeException("Invalid attachment store type: '" + attachmentStoreType + "'");
    }
  }

  private void bindAccountStore() {
    if (accountStoreType.equalsIgnoreCase("memory")) {
      bind(AccountStore.class).to(MemoryStore.class).in(Singleton.class);
    } else if (accountStoreType.equalsIgnoreCase("fake")) {
      bind(AccountStore.class).to(FakePermissiveAccountStore.class).in(Singleton.class);
    } else {
      throw new RuntimeException("Invalid account store type: '" + accountStoreType + "'");
    }
  }
}
