package org.waveprotocol.box.server.rpc.render.web.template;

import com.google.common.base.Function;
import com.google.common.collect.MapMaker;
import com.google.wave.api.ParticipantProfile;

import java.util.Collection;
import java.util.Map;

public class ProfileStore {

  public Map<String, ParticipantProfile> getProfiles(Collection<String> participants) {
    Map<String, ParticipantProfile> profiles =
        new MapMaker().makeComputingMap(new Function<String, ParticipantProfile>() {
          @Override
          public ParticipantProfile apply(String key) {
            return new ParticipantProfile(key, "", "");
          }
        });
    return profiles;
  }
}
