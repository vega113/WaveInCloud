package org.waveprotocol.wave.client.wavepanel.impl.toolbar.gadget;

import java.util.Map;


public interface GadgetInfoProvider <G extends GadgetInfo> {
  Map<String, G> retrieveGadgetInfo(GadgetCategoryType category);
}
