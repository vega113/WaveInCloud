/**
 * Copyright 2011 Google Inc.
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

package org.waveprotocol.box.server.rpc;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.MessageLite;
import com.google.wave.api.FetchProfilesResult;
import com.google.wave.api.ParticipantProfile;

import org.waveprotocol.box.profile.ProfilesProto.ProfileRequest;
import org.waveprotocol.box.profile.ProfilesProto.ProfileResponse;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.rpc.ProtoSerializer.SerializationException;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.util.logging.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A servlet that enables the client side to fetch user profiles using Data API.
 * Typically will be hosted on /profile.
 * 
 * Valid request is: GET /profile/?addresses=user1@example.com,user2@example.com
 * - in URL encoded format. The format of the returned information is the
 * protobuf-JSON format used by the websocket interface.
 * 
 * @author yurize@apache.org (Yuri Zelikov)
 */
@SuppressWarnings("serial")
@Singleton
public final class FetchProfilesServlet extends HttpServlet {

  private static final Log LOG = Log.get(FetchProfilesServlet.class);

  private final SessionManager sessionManager;
  private final ProtoSerializer serializer;

  /**
   * Extracts profile query params from request.
   * 
   * @param req the request.
   * @param response the response.
   * @return the ProfileRequest with query data.
   * @throws UnsupportedEncodingException if the request parameters encoding is
   *         invalid.
   */
  public static ProfileRequest parseProfileRequest(HttpServletRequest req,
      HttpServletResponse response) throws UnsupportedEncodingException {
    String[] addresses = URLDecoder.decode(req.getParameter("addresses"), "UTF-8").split(",");
    ProfileRequest profileRequest =
        ProfileRequest.newBuilder().addAllAddresses(Lists.newArrayList(addresses)).build();
    return profileRequest;
  }

  /**
   * Constructs ProfileResponse which is a protobuf generated class from the
   * output of Data API profile service. ProfileResponse contains the same
   * information as profileResult.
   * 
   * @param profileResult the profile results with digests.
   * @return ProfileResponse
   */
  public static ProfileResponse serializeProfileResult(FetchProfilesResult profileResult) {
    ProfileResponse.Builder builder = ProfileResponse.newBuilder();
    for (ParticipantProfile participantProfile : profileResult.getProfiles()) {
      ProfileResponse.FetchedProfile fetchedProfile =
          ProfileResponse.FetchedProfile.newBuilder().setAddress(participantProfile.getAddress())
              .setImageUrl(participantProfile.getImageUrl()).setName(participantProfile.getName())
              .setProfileUrl(participantProfile.getProfileUrl()).build();
      builder.addProfiles(fetchedProfile);
    }
    return builder.build();
  }

  private static FetchProfilesResult fakeResult(List<String> addresses) {
    List<ParticipantProfile> profiles = Lists.newArrayListWithCapacity(addresses.size());
    for (String address : addresses) {
      // Return a different image/name to ensure the profile update mechanism
      // works.
      ParticipantProfile profile =
          new ParticipantProfile(address, "Faked " + address.split("@")[0],
              "/static/microphone.png", "");
      profiles.add(profile);
    }
    FetchProfilesResult result = new FetchProfilesResult(profiles);
    return result;
  }

  @Inject
  public FetchProfilesServlet(SessionManager sessionManager, ProtoSerializer serializer) {
    this.sessionManager = sessionManager;
    this.serializer = serializer;
  }

  /**
   * Creates HTTP response to the profile query. Main entrypoint for this class.
   */
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse response) throws IOException {
    ParticipantId user = sessionManager.getLoggedInUser(req.getSession(false));
    if (user == null) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }
    ProfileRequest profileRequest = parseProfileRequest(req, response);
    ProfileResponse profileResponse = fetchProfiles(profileRequest, user);
    printJson(profileResponse, response);
  }

  /**
   * Performs profile using Data API.
   */
  private ProfileResponse fetchProfiles(ProfileRequest profileRequest, ParticipantId user) {
    LOG.fine("Fetching profiles: " + Joiner.on(",").join(profileRequest.getAddressesList()));
    // TODO (Yuri Z.) Implement the robot operation FETCH_PROFILES and call it to
    // fetch profiles. For now just fake the results.
    FetchProfilesResult profileResult = fakeResult(profileRequest.getAddressesList());
    LOG.fine("Fetched profiles: " + profileResult.getProfiles().size());
    return serializeProfileResult(profileResult);
  }

  /**
   * Writes the json with profile results to Response.
   */
  private void printJson(MessageLite message, HttpServletResponse resp)
      throws IOException {
    if (message == null) {
      resp.sendError(HttpServletResponse.SC_FORBIDDEN);
    } else {
      resp.setStatus(HttpServletResponse.SC_OK);
      resp.setContentType("application/json");
      // This is to make sure the fetched data is fresh - since the w3c spec
      // is rarely respected.
      resp.setHeader("Cache-Control", "no-store");
      try {
        // FIXME (user) Returning JSON directly from an HTTP GET is vulnerable
        // to XSSI attacks. Issue https://issues.apache.org/jira/browse/WAVE-135
        resp.getWriter().append(serializer.toJson(message).toString());
      } catch (SerializationException e) {
        throw new IOException(e);
      }
    }
  }
}
