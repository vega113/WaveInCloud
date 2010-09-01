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

import com.google.common.collect.ImmutableMap;

import org.waveprotocol.wave.examples.fedone.common.SessionConstants;
import org.waveprotocol.wave.examples.fedone.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The HTTP servlet for serving a wave client along with content generated on
 * the server.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public class WaveClientServlet extends HttpServlet {

  /**
   * Container for the cached static HTML. Both reads/caches some static HTML,
   * and prints server generated content derived from the HTML and some
   * server-generated content.
   *
   * This is a pretty hacky way to achieve effectively extremely lightweight but
   * undisciplined JSP. It might be a good idea to, at some point (if the
   * required server-generated content gets more complicated), replace this with
   * real JSP or equivalent.
   */
  private static final class CachedStaticHtml {
    private final String prefix;
    private final String suffix;

    private CachedStaticHtml(String prefix, String suffix) {
      this.prefix = prefix;
      this.suffix = suffix;
    }

    /**
     * Loads the static HTML from a given filename and caches the static
     * content.
     */
    public static CachedStaticHtml create(String filename) throws IOException {
      BufferedReader in = new BufferedReader(new FileReader(filename));
      StringBuilder prefixBuilder = new StringBuilder();
      StringBuilder suffixBuilder = new StringBuilder();

      boolean isPrefix = true;
      String line = null;
      while ((line = in.readLine()) != null) {
        if (isPrefix && line.contains("SERVER-GENERATED")) {
          isPrefix = false;
        } else {
          if (isPrefix) {
            prefixBuilder.append(line).append('\n');
          } else {
            suffixBuilder.append(line).append('\n');
          }
        }
      }

      in.close();
      return new CachedStaticHtml(prefixBuilder.toString(), suffixBuilder.toString());
    }

    /**
     * Writes the cached static HTML with the server-generated placeholder
     * replaced by a given server generated string.
     */
    public void apply(String serverData, PrintWriter out) {
      out.print(prefix);
      out.println(serverData);
      out.print(suffix);
    }
  }

  private static final Log LOG = Log.get(WaveClientServlet.class);

  private final String hostname;
  private final CachedStaticHtml cachedStaticHtml;

  private WaveClientServlet(String hostname, CachedStaticHtml cachedStaticHtml) {
    this.hostname = hostname;
    this.cachedStaticHtml = cachedStaticHtml;
  }

  /**
   * Creates a servlet and preloads the static HTML.
   */
  public static WaveClientServlet create(String hostname) {
    CachedStaticHtml cachedStaticHtml = null;
    try {
      String staticHtmlFilename = "war/webclient.html.raw";
      cachedStaticHtml = CachedStaticHtml.create(staticHtmlFilename);
      LOG.info("Successfully loaded static HTML at " + staticHtmlFilename);
    } catch (IOException e) {
      throw new RuntimeException("Couldn't load static HTML", e);
    }

    return new WaveClientServlet(hostname, cachedStaticHtml);
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) {
    try {
      cachedStaticHtml.apply(getServerData(), response.getWriter());
      response.setStatus(HttpServletResponse.SC_OK);
    } catch (IOException e) {
      LOG.warning("Failed to send HTML for request " + request, e);
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * @return the extra data to insert into the served static HTML
   */
  private String getServerData() throws IOException {
    return "var __session = " + mapToJson(ImmutableMap.of(SessionConstants.HOSTNAME, hostname))
        + ";";
  }

  /**
   * @return a single JSON string representing a given string map
   */
  private String mapToJson(Map<String, String> map) {
    StringBuilder json = new StringBuilder();
    json.append("{");
    for (Map.Entry<String, String> entry : map.entrySet()) {
      json.append("\"" + entry.getKey() + "\":\"" + entry.getValue() + "\",");
    }
    json.append("}");
    return json.toString();
  }
}
