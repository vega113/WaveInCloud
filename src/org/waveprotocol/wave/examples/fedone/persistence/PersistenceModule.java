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

import com.google.common.base.Preconditions;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.waveprotocol.wave.crypto.CertPathStore;
import org.waveprotocol.wave.examples.fedone.persistence.memory.MemoryStore;
import org.waveprotocol.wave.examples.fedone.persistence.mongodb.MongoDbProvider;

import java.util.HashMap;

/**
 * Module for setting up the different persistence stores.
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 *
 */
public class PersistenceModule extends AbstractModule {

  /**
   * Persistence types that are valid in the properties file.
   */
  public static enum PersistenceType {
    MEMORY("memory"), MONGO_DB("mongodb");

    private static HashMap<String, PersistenceType> reverseLookupMap =
        new HashMap<String, PersistenceType>();

    static {
      for (PersistenceType type : PersistenceType.values()) {
        reverseLookupMap.put(type.name, type);
      }
    }

    private final String name;

    private PersistenceType(String name) {
      this.name = name;
    }

    public static PersistenceType getTypeForName(String name) {
      Preconditions.checkArgument(
          reverseLookupMap.containsKey(name), name + " isn't a valid PersistenceType name");
      return reverseLookupMap.get(name);
    }
  }

  /**
   * Type of persistence to use for the {@link CertPathStore}.
   */
  private final String certPathStoreType;

  private MongoDbProvider mongoDbProvider;

  @Inject
  public PersistenceModule(@Named("cert_path_store_type") String certPathStoreType) {
    this.certPathStoreType = certPathStoreType;
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
  }

  /**
   * Binds the CertPathStore implementation to the store specified in the
   * properties.
   */
  private void bindCertPathStore() {
    PersistenceModule.PersistenceType persistenceType =
        PersistenceModule.PersistenceType.getTypeForName(certPathStoreType);
    switch (persistenceType) {
      case MEMORY:
        bind(CertPathStore.class).to(MemoryStore.class).in(Singleton.class);
        break;
      case MONGO_DB:
        MongoDbProvider mongoDbProvider = getMongoDbProvider();
        bind(CertPathStore.class).toInstance(mongoDbProvider.provideMongoDbStore());
        break;
      default:
        break;
    }
  }
}
