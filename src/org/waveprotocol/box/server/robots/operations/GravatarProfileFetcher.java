/**
 * Copyright 2011 Google Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package org.waveprotocol.box.server.robots.operations;

import com.google.common.base.Preconditions;
import com.google.common.collect.MapMaker;
import com.google.gxp.com.google.common.collect.Maps;
import com.google.wave.api.ParticipantProfile;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.waveprotocol.box.server.robots.operations.FetchProfilesService.ProfilesFetcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link ProfilesFetcher} implementation that fetches profile data from
 * Gravatar using the wave address as email.
 * 
 * @author yurize@apache.org (Yuri Zelikov)
 */
public class GravatarProfileFetcher implements ProfilesFetcher {

  private static final Logger LOG = Logger.getLogger(GravatarProfileFetcher.class.getName());

  private static final String EXCEPTION_FETCHING_PROFILE = "Exception while fetching profile for: ";
  private static final String GRAVATAR_BASIC_URL = "http://www.gravatar.com/";
  private static final String GRAVATAR_URL = "http://www.gravatar.com/avatar/";
  private static final long EXPIRE_AFTER = 1;

  private static String ROBOT_APP_ID = "wavyemail";

  private static final ConcurrentMap<String, ParticipantProfile> PROFILES_CACHE = new MapMaker()
      .expireAfterWrite(EXPIRE_AFTER, TimeUnit.HOURS).makeMap();
   
  private static final Map<String, ParticipantProfile> KNOWN_PROFILES_CACHE = Maps.newHashMap();

  private static final String DEFAULT_WAVE_DOMAIN = "vegalabz.com";
  
  static {
    KNOWN_PROFILES_CACHE.put("@waveinabox.net", new ParticipantProfile("@waveinabox.net",
        "Shared Wave", "https://wave.google.com/wave/static/images/profiles/public.png", ""));
    KNOWN_PROFILES_CACHE.put("@vegalabz.com", new ParticipantProfile("@vegalabz.com",
        "Shared Wave", "https://wave.google.com/wave/static/images/profiles/public.png", ""));
    KNOWN_PROFILES_CACHE
        .put(
            "wavyemail@waveinabox.net",
            new ParticipantProfile(
                "wavyemail@waveinabox.net",
                "Wavy Email bot",
                "https://lh6.googleusercontent.com/_tsWs83xehHE/TLBQ-7Nu-VI/AAAAAAAAFiA/ePzhrvMXkfI/wavyemailbeta.png",
                ""));
  }

  public static GravatarProfileFetcher create() {
    return new GravatarProfileFetcher();
  }

  private GravatarProfileFetcher() {
  }

  public ParticipantProfile fetchFullProfile(String email, String waveAddress)
      throws ProfileFetchException {
    String imageUrl = null;
    String name = null;
    String profileUrl = null;
    JSONObject json = null;
    JSONArray entryArr = null;
    JSONObject entry = null;
    String emailHash = DigestUtils.md5Hex(email.toLowerCase().trim());

    HttpURLConnection connection = null;
    URL url;
    try {
      url = new URL(GRAVATAR_BASIC_URL + emailHash + ".json");
      connection = (HttpURLConnection) url.openConnection();
      connection.setDoOutput(true);
      connection.setRequestMethod("GET");
      connection.connect();
    } catch (MalformedURLException e) {
      throw new ProfileFetchException(EXCEPTION_FETCHING_PROFILE + email, e);
    } catch (ProtocolException e) {
      throw new ProfileFetchException(EXCEPTION_FETCHING_PROFILE + email, e);
    } catch (IOException e) {
      throw new ProfileFetchException(EXCEPTION_FETCHING_PROFILE + email, e);
    }

    StringBuilder sb = new StringBuilder();
    try {
      if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
          sb.append(line);
        }
      } else {
        throw new ProfileFetchException("Response code: " + connection.getResponseCode()
            + "while fetching profile for: " + email + ", fetching 'identicon' image instead.");
      }
    } catch (java.io.IOException e) {
      throw new ProfileFetchException(EXCEPTION_FETCHING_PROFILE + email, e);
    } finally {
      connection.disconnect();
    }
    LOG.fine("Json profile for " + email + " : " + sb.toString());
    try {
      json = new JSONObject(sb.toString());
      entryArr = new JSONArray(json.getString("entry"));
      entry = entryArr.getJSONObject(0);
      imageUrl = entry.getString("thumbnailUrl");
      name = entry.getString("displayName");
      profileUrl = entry.getString("profileUrl");
    } catch (JSONException e) {
      throw new ProfileFetchException(EXCEPTION_FETCHING_PROFILE + email, e);
    }
    ParticipantProfile profile = new ParticipantProfile(waveAddress, name, imageUrl, profileUrl);
    PROFILES_CACHE.put(email, profile);
    return profile;
  }

  /**
   * Returns the Gravatar URL for the given email address.
   */
  public String getImageUrl(String email) {
    // Hexadecimal MD5 hash of the requested user's lowercased email address
    // with all whitespace trimmed.
    String emailHash = DigestUtils.md5Hex(email.toLowerCase().trim());
    return GRAVATAR_URL + emailHash + ".jpg" + "?s=100&d=identicon";
  }

  private ParticipantProfile fetchOnlyGravatarImage(String waveAddress, String email) throws ProfileFetchException {
    ParticipantProfile pTemp = null;
    pTemp = FetchProfilesService.ProfilesFetcher.SIMPLE_PROFILE_FETCHER.fetchProfile(email);
    ParticipantProfile profile =
        new ParticipantProfile(waveAddress, pTemp.getName(), getImageUrl(email), pTemp.getProfileUrl());
    return profile;
  }

  @Override
  public ParticipantProfile fetchProfile(String waveAddress) {
    Preconditions.checkNotNull(waveAddress);
    if (PROFILES_CACHE.containsKey(waveAddress)) {
      return PROFILES_CACHE.get(waveAddress);
    } else if (KNOWN_PROFILES_CACHE.containsKey(waveAddress)) {
      return KNOWN_PROFILES_CACHE.get(waveAddress);
    }
    ParticipantProfile participantProfile = null;
    String[] split = waveAddress.split("@");
    String wavyEmailAddress;
    if (split[1].equals(DEFAULT_WAVE_DOMAIN)) {
      wavyEmailAddress = split[0] + "@" + ROBOT_APP_ID + ".appspotmail.com";
      try {
        participantProfile = fetchFullProfile(wavyEmailAddress, waveAddress);
      } catch (ProfileFetchException e) {
        LOG.fine("Can't load full profile for: " + wavyEmailAddress);
      }
    }
    if (!split[1].equals(DEFAULT_WAVE_DOMAIN) || participantProfile == null) {
      wavyEmailAddress = split[0] + "_" + split[1] + "@" + ROBOT_APP_ID + ".appspotmail.com";
      try {
        participantProfile = fetchFullProfile(wavyEmailAddress, waveAddress);
      } catch (ProfileFetchException e) {
        LOG.fine("Can't load full profile for: " + wavyEmailAddress);
        try {
          wavyEmailAddress = split[0] + "@" + ROBOT_APP_ID + ".appspotmail.com";
          LOG.fine("Trying to fetch only Gravatar image for: " + wavyEmailAddress);
          participantProfile = fetchOnlyGravatarImage(waveAddress, wavyEmailAddress);
        } catch (ProfileFetchException e1) {
          LOG.log(Level.SEVERE, EXCEPTION_FETCHING_PROFILE + waveAddress + "wavyEmail: " + wavyEmailAddress, e1);
          participantProfile = new ParticipantProfile();
        }
      }
    }
    PROFILES_CACHE.put(waveAddress, participantProfile);
    return participantProfile;
  }
}
