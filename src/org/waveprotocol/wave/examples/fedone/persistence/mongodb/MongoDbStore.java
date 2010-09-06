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

package org.waveprotocol.wave.examples.fedone.persistence.mongodb;

import com.google.protobuf.InvalidProtocolBufferException;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;

import org.waveprotocol.wave.crypto.CertPathStore;
import org.waveprotocol.wave.crypto.SignatureException;
import org.waveprotocol.wave.crypto.SignerInfo;
import org.waveprotocol.wave.examples.fedone.persistence.AttachmentStore;
import org.waveprotocol.wave.federation.Proto.ProtocolSignerInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <b>CertPathStore:</b><br/>
 * <i>Collection(signerInfo):</i>
 * <ul>
 * <li>_id : signerId byte array.</li>
 * <li>protoBuff : byte array representing the protobuff message of a
 * {@link ProtocolSignerInfo}.</li>
 * </ul>
 * <p>
 *
 * @author ljvderijk@google.com (Lennard de Rijk)
 * @author josephg@gmail.com (Joseph Gentle)
 *
 */
public final class MongoDbStore implements CertPathStore, AttachmentStore {

  private static final Logger LOG = Logger.getLogger(MongoDbStore.class.getName());

  private final DB database;

  MongoDbStore(DB database) {
    this.database = database;
  }

  @Override
  public SignerInfo getSignerInfo(byte[] signerId) {
    DBObject query = getDBObjectForSignerId(signerId);
    DBCollection signerInfoCollection = getSignerInfoCollection();
    DBObject signerInfoDBObject = signerInfoCollection.findOne(query);

    // Sub-class contract specifies return null when not found
    SignerInfo signerInfo = null;

    if (signerInfoDBObject != null) {
      byte[] protobuff = (byte[]) signerInfoDBObject.get("protoBuff");
      try {
        signerInfo = new SignerInfo(ProtocolSignerInfo.parseFrom(protobuff));
      } catch (InvalidProtocolBufferException e) {
        LOG.log(Level.SEVERE, "Couldn't parse the protobuff stored in MongoDB: " + protobuff, e);
      } catch (SignatureException e) {
        LOG.log(Level.SEVERE, "Couldn't parse the certificate chain or domain properly", e);
      }
    }
    return signerInfo;
  }

  @Override
  public void putSignerInfo(ProtocolSignerInfo protocolSignerInfo) throws SignatureException {
    SignerInfo signerInfo = new SignerInfo(protocolSignerInfo);
    byte[] signerId = signerInfo.getSignerId();

    // Not using a modifier here because rebuilding the object is not a lot of
    // work. Doing implicit upsert by using save with a DBOBject that has an _id
    // set.
    DBObject signerInfoDBObject = getDBObjectForSignerId(signerId);
    signerInfoDBObject.put("protoBuff", protocolSignerInfo.toByteArray());
    getSignerInfoCollection().save(signerInfoDBObject);
  }

  /**
   * Returns an instance of {@link DBCollection} for storing SignerInfo.
   */
  private DBCollection getSignerInfoCollection() {
    return database.getCollection("signerInfo");
  }

  /**
   * Returns a {@link DBObject} which contains the key-value pair used to
   * signify the signerId.
   *
   * @param signerId the signerId value to set.
   * @return a new {@link DBObject} with the (_id,signerId) entry.
   */
  private DBObject getDBObjectForSignerId(byte[] signerId) {
    DBObject query = new BasicDBObject();
    query.put("_id", signerId);
    return query;
  }

  // *********** Attachments.
  
  private GridFS attachmentGrid;
  private GridFS getAttachmentGrid() {
    if (attachmentGrid == null) {
      attachmentGrid = new GridFS(database, "attachments");
    }
    
    return attachmentGrid;
  }

  @Override
  public AttachmentData getAttachment(String id) {
    final GridFSDBFile attachment = getAttachmentGrid().findOne(id);
    
    if (attachment == null) {
      return null;
    } else {
      return new AttachmentData() {
        @Override
        public void writeDataTo(OutputStream out) throws IOException {
          attachment.writeTo(out);
        }
        
        @Override
        public Date getLastModifiedDate() {
          return attachment.getUploadDate();
        }
        
        @Override
        public long getContentSize() {
          return attachment.getLength();
        }

        @Override
        public InputStream getInputStream() {
          return attachment.getInputStream();
        }
      };
    }
  }

  @Override
  public boolean storeAttachment(String id, InputStream data) throws IOException {
    // This method returns false if the attachment is already in the database.
    // Unfortunately, as far as I can tell the only way to do this is to perform
    // a second database query.
    if (getAttachment(id) != null) { 
      return false;
    } else {
      GridFSInputFile file = getAttachmentGrid().createFile(data, id);

      try {
        file.save();
      } catch (MongoException e) {
        // Unfortunately, file.save() wraps any IOException thrown in a
        // 'MongoException'. Since the interface explicitly throws IOExceptions,
        // we unwrap any IOExceptions thrown.
        Throwable innerException = e.getCause();
        if (innerException instanceof IOException) {
          throw (IOException) innerException;
        } else {
          throw e;
        }
      }
      return true;
    }
  }

  @Override
  public void deleteAttachment(String id) {
    getAttachmentGrid().remove(id);
  }

}
