/**
 * Copyright 2010 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 */

package org.waveprotocol.wave.examples.client.webclient.client;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;

import org.waveprotocol.wave.common.util.GwtWaverefEncoder;
import org.waveprotocol.wave.examples.client.webclient.common.communication.callback.SimpleCallback;
import org.waveprotocol.wave.examples.client.webclient.util.Log;
import org.waveprotocol.wave.examples.fedone.waveserver.WaveSnapshot;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.waveref.WaveRef;

/**
 * Helper class to fetch wavelet snapshots using the static snapshot fetch 
 * service.
 *
 * @author josephg@gmail.com (Joseph Gentle)
 */
public final class SnapshotFetcher {
  private static Log LOG = Log.get(SnapshotFetcher.class);
  private static final String FETCH_URL_BASE = "/fetch";
  
  private SnapshotFetcher() {}

  private static String getUrl(WaveRef waveRef) {
    String pathSegment = GwtWaverefEncoder.encodeToUriPathSegment(waveRef);
    return FETCH_URL_BASE + "/" + pathSegment;
  }
  
  /**
   * Fetch a wave view snapshot from the static fetch servlet.
   * 
   * @param waveId The wave to fetch
   * @param callback A callback through which the fetched wave will be returned.
   */
  public static void fetchWave(WaveId waveId,
      final SimpleCallback<WaveSnapshot, Throwable> callback) {
    String url = getUrl(WaveRef.of(waveId));
    LOG.info("Fetching wavelet " + waveId.toString() + " at " + url);
    
    RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);
    requestBuilder.setCallback(new RequestCallback() {
      @Override
      public void onResponseReceived(Request request, Response response) {
        LOG.info("Snapshot response recieved: " + response.getText());
        // Pull the snapshot out of the response object and return it using
        // the provided callback function.
        if (response.getStatusCode() != Response.SC_OK) {
          callback.onFailure(new RequestException("Got back status code " + response.getStatusCode()));
        } else if (!response.getHeader("Content-Type").startsWith("application/json")) {
          callback.onFailure(new RuntimeException("Fetch service did not return json"));
        } else {
          WaveSnapshot snapshot;
          try {
            snapshot = WaveSnapshot.parse(response.getText());
          } catch (Throwable e) {
            callback.onFailure(e);
            return;
          }
          
          callback.onSuccess(snapshot);
        }
      }
    
      @Override
      public void onError(Request request, Throwable exception) {
        LOG.info("Snapshot error: " + exception.toString());
        callback.onFailure(exception);
      }
    });
    
    try {
      requestBuilder.send();
    } catch (RequestException e) {
      callback.onFailure(e);
    }
  }
}
