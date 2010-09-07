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

package org.waveprotocol.wave.examples.fedone.rpc;

import com.google.inject.Inject;

import com.dyuproject.protostuff.json.ReflectionNumericJSON;

import org.waveprotocol.wave.common.util.JavaWaverefEncoder;
import org.waveprotocol.wave.examples.fedone.common.CoreWaveletOperationSerializer;
import org.waveprotocol.wave.examples.fedone.common.HashedVersion;
import org.waveprotocol.wave.examples.fedone.frontend.WaveletSnapshotAndVersions;
import org.waveprotocol.wave.examples.fedone.util.Log;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc.DocumentSnapshot;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc.WaveSnapshot;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc.WaveletSnapshot;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveClientRpc.WaveletSnapshotAndVersion;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveletProvider;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveletSnapshotBuilder;
import org.waveprotocol.wave.federation.Proto;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.model.document.operation.BufferedDocOp;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.core.CoreWaveletData;
import org.waveprotocol.wave.model.waveref.InvalidWaveRefException;
import org.waveprotocol.wave.model.waveref.WaveRef;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 
 *
 */
public class FetchServlet extends HttpServlet {
  private static final Log LOG = Log.get(FetchServlet.class);
  
  @Inject
  public FetchServlet(WaveletProvider waveletProvider) {
    this.waveletProvider = waveletProvider;

    jsonConverter = new ReflectionNumericJSON(new Class<?>[] {
        WaveClientRpc.ProtocolOpenRequest.class,
        WaveClientRpc.ProtocolSubmitRequest.class,
        WaveClientRpc.ProtocolSubmitResponse.class,
        WaveClientRpc.ProtocolWaveClientRpc.class,
        WaveClientRpc.ProtocolWaveletUpdate.class,
        WaveClientRpc.WaveletSnapshot.class,
        WaveClientRpc.DocumentSnapshot.class,
        WaveClientRpc.WaveletSnapshotAndVersion.class,
        WaveClientRpc.WaveSnapshot.class,
        Proto.ProtocolAppliedWaveletDelta.class,
        Proto.ProtocolDocumentOperation.class,
        Proto.ProtocolDocumentOperation.Component.class,
        Proto.ProtocolDocumentOperation.Component.KeyValuePair.class,
        Proto.ProtocolDocumentOperation.Component.KeyValueUpdate.class,
        Proto.ProtocolDocumentOperation.Component.ElementStart.class,
        Proto.ProtocolDocumentOperation.Component.ReplaceAttributes.class,
        Proto.ProtocolDocumentOperation.Component.UpdateAttributes.class,
        Proto.ProtocolDocumentOperation.Component.AnnotationBoundary.class,
        Proto.ProtocolHashedVersion.class,
        Proto.ProtocolSignature.class,
        Proto.ProtocolSignedDelta.class,
        Proto.ProtocolSignerInfo.class,
        Proto.ProtocolWaveletDelta.class,
        Proto.ProtocolWaveletOperation.class,
        Proto.ProtocolWaveletOperation.MutateDocument.class,
    });
  }
  
  WaveletProvider waveletProvider;
  ReflectionNumericJSON jsonConverter;
  
  private WaveletSnapshot serializeSnapshot(CoreWaveletData snapshot) {
    WaveletSnapshot.Builder snapshotBuilder = WaveletSnapshot.newBuilder();
    Map<String, BufferedDocOp> documentMap = snapshot.getDocuments();
    for (Entry<String,BufferedDocOp> document : documentMap.entrySet()) {
      DocumentSnapshot.Builder documentBuilder = DocumentSnapshot.newBuilder();
      documentBuilder.setDocumentId(document.getKey());
      documentBuilder.setDocumentOperation(
          CoreWaveletOperationSerializer.serialize(document.getValue()));
      snapshotBuilder.addDocument(documentBuilder.build());
    }
    for (ParticipantId participant : snapshot.getParticipants()) {
      snapshotBuilder.addParticipantId(participant.toString());
    }
    return snapshotBuilder.build();
  }
  
  protected WaveletSnapshotAndVersions getSnapshot(WaveletName waveletName) {
    WaveletSnapshotBuilder<WaveletSnapshotAndVersions> snapshotBuilder =
      new WaveletSnapshotBuilder<WaveletSnapshotAndVersions>() {
      @Override
      public WaveletSnapshotAndVersions build(CoreWaveletData waveletData,
          HashedVersion currentVersion, ProtocolHashedVersion committedVersion) {
        return new WaveletSnapshotAndVersions(serializeSnapshot(waveletData),
            currentVersion, committedVersion);
      }
    };
    return waveletProvider.getSnapshot(waveletName, snapshotBuilder);
  }
  
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse response)
      throws IOException {
    
    // This path will look like "/google.com/w+abc123/foo.com/conv+root
    // Strip off the leading '/'.
    String urlPath = req.getPathInfo().substring(1);
    
    // Extract the name of the wavelet from the URL
    WaveRef waveref;
    try {
      waveref = JavaWaverefEncoder.decodeWaveRefFromPath(urlPath);
    } catch (InvalidWaveRefException e) {
      // The URL contains an invalid waveref.
      response.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;
    }
    
    renderSnapshot(waveref, response);
  }
   
  protected void renderSnapshot(WaveRef waveref, HttpServletResponse dest) throws IOException {
    // Until we have a way to get all the waves that are visible to the user,
    // we'll just send conv+root.
    WaveletId waveletId = waveref.hasWaveletId() ?
        waveref.getWaveletId() : new WaveletId(waveref.getWaveId().getDomain(), "conv+root");
    
    WaveletName waveletName = WaveletName.of(waveref.getWaveId(), waveletId);
    LOG.info("Fetching snapshot of wavelet " + waveletName);
    WaveletSnapshotAndVersions snapshot = getSnapshot(waveletName);
    
    if (snapshot != null) {
      if (!waveref.hasDocumentId()) {
        // We have a wavelet id. Pull up the wavelet snapshot and return it.
        dest.setContentType("application/json");
        dest.setStatus(HttpServletResponse.SC_OK);
        
        // This is a hack until we move away from core
        WaveletSnapshotAndVersion protoWaveletSnapshot = WaveletSnapshotAndVersion.newBuilder()
            .setWaveletId(waveletId.serialise())
            .setSnapshot(snapshot.snapshot)
            .setVersion(snapshot.currentVersion).build();
        
        if (waveref.hasWaveletId()) {
          jsonConverter.writeTo(dest.getWriter(), protoWaveletSnapshot);
        } else {
          WaveSnapshot waveSnapshot = WaveSnapshot.newBuilder()
              .addWavelet(protoWaveletSnapshot).build();
          
          jsonConverter.writeTo(dest.getWriter(), waveSnapshot);
        }
      } else {
        // We have a wavelet id and document id. Find the document in the snapshot
        // and return it.
        DocumentSnapshot docSnapshot = null;
        for (DocumentSnapshot ds : snapshot.snapshot.getDocumentList()) {
          if (ds.getDocumentId().equals(waveref.getDocumentId())) {
            docSnapshot = ds;
            break;
          }
        }
        if (docSnapshot != null) {
          dest.setContentType("application/json");
          dest.setStatus(HttpServletResponse.SC_OK);
          jsonConverter.writeTo(dest.getWriter(), docSnapshot);          
        } else {
          dest.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
      }
    } else {
      // Snapshot is null. 404.
      dest.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
  }
}
