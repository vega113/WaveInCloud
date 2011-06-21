package org.waveprotocol.wave.client.wavepanel.impl.toolbar.gadget;


public interface GadgetInfo {

  String getName();

  String getDescription();

  GadgetCategoryType getPrimaryCategory();

  GadgetCategoryType getSecondaryCategory();

  String getGadgetUrl();

  String getAuthor();

  String getSubmittedBy();

  String getImageUrl();
}