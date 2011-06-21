package org.waveprotocol.wave.client.wavepanel.impl.toolbar.gadget;


public class GadgetInfoImpl implements GadgetInfo {
  
  private final String name;
  private final String description;
  private final GadgetCategoryType primaryCategory;
  private final GadgetCategoryType secondaryCategory;
  private final String gadgetUrl;
  private final String author;
  private final String submittedBy;
  private final String imageUrl;
  
  public static GadgetInfoImpl of(String name, String description,
          GadgetCategoryType primaryCategory, GadgetCategoryType secondaryCategory,
          String gadgetUrl, String author, String submittedBy, String imageUrl) {
    return new GadgetInfoImpl(name, description, primaryCategory, secondaryCategory, gadgetUrl,
            author, submittedBy, imageUrl);
  }
  
  GadgetInfoImpl(String name, String description, GadgetCategoryType primaryCategory,
          GadgetCategoryType secondaryCategory, String gadgetUrl, String author, String submittedBy,
          String imageUrl) {
    this.name = name;
    this.description = description;
    this.primaryCategory = primaryCategory;
    this.secondaryCategory = secondaryCategory;
    this.gadgetUrl = gadgetUrl;
    this.author = author;
    this.submittedBy = submittedBy;
    this.imageUrl = imageUrl;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public GadgetCategoryType getPrimaryCategory() {
    return primaryCategory;
  }

  @Override
  public GadgetCategoryType getSecondaryCategory() {
    return secondaryCategory;
  }

  @Override
  public String getGadgetUrl() {
    return gadgetUrl;
  }

  @Override
  public String getAuthor() {
    return author;
  }

  @Override
  public String getSubmittedBy() {
    return submittedBy;
  }
  
  @Override
  public String getImageUrl() {
    return imageUrl;
  }
}
