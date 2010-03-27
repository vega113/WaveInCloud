// Copyright 2008 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.model.document.operation;


/**
 * Do not rely on this. This is just a temporary location modifier being used
 * for transitional purposes. The proper way will be to use annotations.
 *
*
 */
public class LocationModifier implements DocOpCursor {

  private int mutatingLocation;
  private int scanPoint = 0;

  /**
   * @param location The location to transform
   */
  public LocationModifier(int location) {
    this.mutatingLocation = location;
  }

  @Override
  public void retain(int itemCount) {
    scanPoint += itemCount;
  }

  @Override
  public void characters(String characters) {
    insert(characters.length());
  }

  @Override
  public void elementStart(String type, Attributes attributes) {
    insert(1);
  }

  @Override
  public void elementEnd() {
    insert(1);
  }

  @Override
  public void deleteCharacters(String chars) {
    delete(chars.length());
  }

  @Override
  public void deleteElementStart(String type, Attributes attributes) {
    delete(1);
  }

  @Override
  public void deleteElementEnd() {
    delete(1);
  }

  private void insert(int size) {
    if (scanPoint < mutatingLocation) {
      mutatingLocation += size;
      scanPoint += size;
    }
  }

  private void delete(int size) {
    if (scanPoint <= mutatingLocation) {
      mutatingLocation = Math.max(scanPoint, mutatingLocation - size);
    }
  }

  @Override
  public void replaceAttributes(Attributes oldAttributes, Attributes newAttributes) {
    retain(1);
  }

  @Override
  public void updateAttributes(AttributesUpdate attributesUpdate) {
    retain(1);
  }

  @Override
  public void annotationBoundary(AnnotationBoundaryMap m) {}

  /**
   * @param op
   * @param location
   * @return location transformed against mutation
   */
  public static int transformLocation(DocOp op, int location) {
    LocationModifier modifier = new LocationModifier(location);
    op.apply(modifier);
    return modifier.mutatingLocation;
  }

}
