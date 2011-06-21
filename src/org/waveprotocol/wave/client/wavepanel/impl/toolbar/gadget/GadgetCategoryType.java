package org.waveprotocol.wave.client.wavepanel.impl.toolbar.gadget;

public enum GadgetCategoryType {
  ALL("All"),
  GAME("Game"),
  IMAGE("Image"),
  MAP("Map"),
  MUSIC("Music"),
  PRODUCTIVITY("Productivity"),
  UTILITY("Utility"),
  VIDEO("Video"),
  VOTING("Voting"),
  OTHER("Other"),
  ;
  
  private final String value;
  
  private GadgetCategoryType(String value) {
    this.value = value;
  }
  
  public String getValue() {
    return value;
  }
}
