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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.wave.api.FetchProfilesRequest;
import com.google.wave.api.FetchProfilesResult;
import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.JsonRpcConstant.ParamsProperty;
import com.google.wave.api.OperationRequest;
import com.google.wave.api.ParticipantProfile;

import org.waveprotocol.box.server.robots.OperationContext;
import org.waveprotocol.box.server.robots.util.OperationUtil;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.List;
import java.util.Map;

/**
 * {@link OperationService} for the "fetchProfiles" operation.
 * 
 * @author yurize@apache.org (Yuri Zelikov)
 */
public class FetchProfilesService implements OperationService {

  public interface ProfilesFetcher {

    ParticipantProfile fetchProfile(String address);
    
    
    @SuppressWarnings("serial")
    class ProfileFetchException extends Exception {

      public ProfileFetchException(String msg, Throwable t) {
        super(msg, t);
      }

      public ProfileFetchException(String msg) {
        super(msg);
      }
      
    }

    static ProfilesFetcher SIMPLE_PROFILE_FETCHER = new ProfilesFetcher() {

      /**
       * Attempts to create the fragments of the participant's name from their
       * address, for example "john.smith@example.com" into ["John", "Smith"].
       */
      private String buildNames(String address) {
        String fullName;
        List<String> names = CollectionUtils.newArrayList();
        String nameWithoutDomain = address.split("@")[0];
        if (nameWithoutDomain != null && !nameWithoutDomain.isEmpty()) {
          // Include empty names from fragment, so split with a -ve.
          for (String fragment : nameWithoutDomain.split("[._]", -1)) {
            if (!fragment.isEmpty()) {
              names.add(capitalize(fragment));
            }
          }
          // ParticipantId normalization implies names can not be empty.
          assert !names.isEmpty();
          fullName = Joiner.on(' ').join(names);
          return fullName;
        } else {
          // Name can be empty in case of shared domain participant which has
          // the the form:
          // @example.com.
          return address;
        }
      }

      private String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
      }

      @Override
      public ParticipantProfile fetchProfile(String address) {
        String name = buildNames(address);
        return new ParticipantProfile(address, name, "/static/unknown.jpg", "");
      }
    };
  };
  
  private final ProfilesFetcher profilesFetcher;
  
  public static FetchProfilesService create() {
    return new FetchProfilesService(GravatarProfileFetcher.create());
  }

  FetchProfilesService(ProfilesFetcher profilesFetcher) {
    this.profilesFetcher = profilesFetcher;
  }

  @Override
  public void execute(OperationRequest operation, OperationContext context,
      ParticipantId participant) throws InvalidRequestException {
    FetchProfilesRequest request =
        OperationUtil.getRequiredParameter(operation, ParamsProperty.FETCH_PROFILES_REQUEST);
    List<String> requestAddresses = request.getParticipantIds();
    List<ParticipantProfile> profiles = Lists.newArrayListWithCapacity(requestAddresses.size());
    for (String address : requestAddresses) {
      ParticipantProfile participantProfile = null;
      participantProfile = profilesFetcher.fetchProfile(address);
      if (participantProfile != null) {
        profiles.add(participantProfile);
      }
    }
    FetchProfilesResult result = new FetchProfilesResult(profiles);
    Map<ParamsProperty, Object> data =
        ImmutableMap.<ParamsProperty, Object> of(ParamsProperty.FETCH_PROFILES_RESULT, result);
    context.constructResponse(operation, data);
  }
}
