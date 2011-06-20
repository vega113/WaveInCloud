package org.waveprotocol.box.server.rpc.render;

import com.google.wave.api.InvalidRequestException;

import org.waveprotocol.box.server.robots.OperationContext;
import org.waveprotocol.wave.model.conversation.ConversationView;
import org.waveprotocol.wave.model.id.IdConstants;
import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.supplement.PrimitiveSupplement;
import org.waveprotocol.wave.model.supplement.SupplementedWave;
import org.waveprotocol.wave.model.supplement.SupplementedWaveImpl;
import org.waveprotocol.wave.model.supplement.SupplementedWaveImpl.DefaultFollow;
import org.waveprotocol.wave.model.supplement.WaveletBasedSupplement;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.opbased.OpBasedWavelet;

public class ServiceUtil {
  
  private ServiceUtil() {
    
  }
  
  /**
   * Builds the supplement model for a wave.
   * 
   * @param operation the operation.
   * @param context the operation context.
   * @param participant the viewer.
   * @return the wave supplement.
   * @throws InvalidRequestException if the wave id provided in the operation is
   *         invalid.
   */
  public static SupplementedWave buildSupplement(WaveId waveId, WaveletId waveletId,
      OperationContext context, ParticipantId participant) throws InvalidRequestException {
    OpBasedWavelet wavelet = context.openWavelet(waveId, waveletId, participant);
    ConversationView conversationView = context.getConversationUtil().buildConversation(wavelet);

    // TODO (Yuri Z.) Find a way to obtain an instance of IdGenerator and use it
    // to create udwId.
    WaveletId udwId = buildUserDataWaveletId(participant);
    OpBasedWavelet udw = context.openWavelet(waveId, udwId, participant);
    PrimitiveSupplement udwState = WaveletBasedSupplement.create(udw);
    SupplementedWave supplement =
        SupplementedWaveImpl.create(udwState, conversationView, participant, DefaultFollow.ALWAYS);
    return supplement;
  }

  /**
   * Builds user data wavelet id.
   */
  public static WaveletId buildUserDataWaveletId(ParticipantId participant) {
    WaveletId udwId =
        WaveletId.of(participant.getDomain(),
            IdUtil.join(IdConstants.USER_DATA_WAVELET_PREFIX, participant.getAddress()));
    return udwId;
  }
}
