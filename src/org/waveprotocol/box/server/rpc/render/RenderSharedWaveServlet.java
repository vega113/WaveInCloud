package org.waveprotocol.box.server.rpc.render;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.wave.api.InvalidRequestException;
import com.google.wave.api.ProtocolVersion;
import com.google.wave.api.data.converter.EventDataConverterManager;

import org.waveprotocol.box.server.CoreSettings;
import org.waveprotocol.box.server.authentication.AccountStoreHolder;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.robots.OperationContextImpl;
import org.waveprotocol.box.server.robots.util.ConversationUtil;
import org.waveprotocol.box.server.rpc.render.web.template.Templates;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.waveref.InvalidWaveRefException;
import org.waveprotocol.wave.model.waveref.WaveRef;
import org.waveprotocol.wave.util.escapers.jvm.JavaWaverefEncoder;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author vega113@gmail.com (Yuri Zelikov)
 */
@SuppressWarnings("serial")
@Singleton
public class RenderSharedWaveServlet extends HttpServlet {


  private static final String NO_CONVERSATIONS = "<div><b>No conversations in this wave or invalid wave id!</b></div>";

  private static Logger LOG = Logger
      .getLogger(RenderSharedWaveServlet.class.getName());

  private final Templates templates;
  private final ConversationUtil conversationUtil;
  private final EventDataConverterManager converterManager;
  private final WaveletProvider waveletProvider;
  private final SessionManager sessionManager;
  private final String httpAddress;
  
  @Inject
  public RenderSharedWaveServlet(EventDataConverterManager converterManager,
      WaveletProvider waveletProvider, ConversationUtil conversationUtil, Templates templates,
      SessionManager sessionManager, @Named(CoreSettings.HTTP_FRONTEND_PUBLIC_ADDRESS) String httpAddress) {
    this.converterManager = converterManager;
    this.waveletProvider = waveletProvider;
    this.conversationUtil = conversationUtil;
    this.templates = templates;
    this.sessionManager = sessionManager;
    this.httpAddress = httpAddress;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
 throws ServletException,
      IOException {
    ParticipantId user = sessionManager.getLoggedInUser(req.getSession(false));
    PrintWriter w = resp.getWriter();
    StringBuilder out = new StringBuilder();
    String waveRefStringValue = req.getRequestURI().replace("/render/wave/", "");
    WaveRef waveRef = null;
    try {
      waveRef = JavaWaverefEncoder.decodeWaveRefFromPath(waveRefStringValue);
      if (waveRef.getWaveletId() == null) {
        waveRef = JavaWaverefEncoder.decodeWaveRefFromPath(waveRefStringValue + "/~/conv+root");
      }
    } catch (InvalidWaveRefException e) {
      out.append(NO_CONVERSATIONS);
      w.print(out.toString());
      w.flush();
      return;
    }
    WaveId waveId = waveRef.getWaveId();
    WaveletId waveletId = waveRef.getWaveletId();
    String blipId = null; // TODO (Yuri Z.) Enable up to blip level referencing.
    String innerHtml = fetchRenderedWavelet(waveId, waveletId, blipId, user);
    if (innerHtml == null) {
      innerHtml = NO_CONVERSATIONS;
    }
    String userIdStr =
        user != null ? user.getAddress() : "@" + AccountStoreHolder.getDefaultDomain();
    String link = "http://" + httpAddress + req.getRequestURI();
    String indexLink = "http://" + httpAddress + "/render/index.html";
    String outerHtml =
        templates.process(Templates.OUTER_TEMPLATE, new String[] {userIdStr, innerHtml, waveRefStringValue, Templates.GA_FRAGMENT, link, indexLink});
    out.append(outerHtml);
    w.print(out.toString());
    w.flush();
  }

  private String fetchRenderedWavelet(WaveId waveId, WaveletId waveletId, String blipId,
      ParticipantId viewer) {
    OperationContextImpl context =
        new OperationContextImpl(waveletProvider,
            converterManager.getEventDataConverter(ProtocolVersion.DEFAULT), conversationUtil);
    LOG.fine("Fetching wavelet: waveId: " + waveId.serialise() + ", waveletId: "
        + waveletId != null ? waveletId.serialise() : "");
    if (viewer == null) {
      viewer = ParticipantId.ofUnsafe("@" + AccountStoreHolder.getDefaultDomain());
    }
    String html = null;
    try {
      html = RenderWaveService.create().exec(waveId, waveletId, blipId, viewer, context);
    } catch (InvalidRequestException e) {
    }
    return html;
  }
}
