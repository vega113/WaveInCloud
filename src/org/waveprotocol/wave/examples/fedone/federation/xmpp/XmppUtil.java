package org.waveprotocol.wave.examples.fedone.federation.xmpp;

import org.waveprotocol.wave.protocol.common;
import org.dom4j.Element;
import org.apache.commons.codec.binary.Base64;
import com.google.protobuf.ByteString;

import java.util.logging.Logger;
import java.util.List;

/**
 * Common utility code for XMPP packet generation and parsing.
 *
 *
 *
 */
public class XmppUtil {

  private static final Logger logger =
      Logger.getLogger(XmppUtil.class.getCanonicalName());

  static void protocolSignerInfoToXml(common.ProtocolSignerInfo signerInfo, Element parent) {
    Element signature = parent.addElement("signature", WaveXmppComponent.NAMESPACE_WAVE_SERVER);
    signature.addAttribute("domain", signerInfo.getDomain());
    common.ProtocolSignerInfo.HashAlgorithm hashValue = signerInfo.getHashAlgorithm();
    switch (hashValue) {
      case SHA256:
        signature.addAttribute("algorithm", "SHA256");
        break;
      case SHA512:
        signature.addAttribute("algorithm", "SHA512");
        break;
      default:
        logger.severe("Unknown hash algorithm in " + signerInfo);
        return; // TODO: raise
      // TODO: fail packet response
    }
    for (ByteString cert : signerInfo.getCertificateList()) {
      signature.addElement("certificate").addCDATA(new String(Base64.encodeBase64(cert.toByteArray())));
    }
  }

  static common.ProtocolSignerInfo xmlToProtocolSignerInfo(Element signature) {
    common.ProtocolSignerInfo.HashAlgorithm hash =
        common.ProtocolSignerInfo.HashAlgorithm.valueOf(signature.attributeValue("algorithm").toUpperCase());
    common.ProtocolSignerInfo.Builder signer = common.ProtocolSignerInfo.newBuilder();
    signer.setHashAlgorithm(hash);
    signer.setDomain(signature.attributeValue("domain"));

    for (Element certElement : (List<Element>) signature.elements("certificate")) {
      byte[] certText = Base64.decodeBase64(certElement.getText().getBytes());
      signer.addCertificate(ByteString.copyFrom(certText));
    }
    return signer.build();
  }
}
