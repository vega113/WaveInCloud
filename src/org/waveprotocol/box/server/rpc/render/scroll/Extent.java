/**
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.waveprotocol.box.server.rpc.render.scroll;

/**
 * An immutable, one-dimensional region.
 *
 * @author hearnden@google.com (David Hearnden)
 */
public final class Extent {
  private final double start;
  private final double end;

  private Extent(double start, double end) {
    this.start = start;
    this.end = end;
  }

  public static Extent of(double start, double end) {
    return new Extent(start, end);
  }

  public double getStart() {
    return start;
  }

  public double getEnd() {
    return end;
  }

  public double getSize() {
    return end - start;
  }

  public Extent scale(double s) {
    double mid = (end + start) / 2;
    double half = s * (end - start) / 2;
    return Extent.of(mid -half, mid + half);
  }

  @Override
  public String toString() {
    return "[" + start + ", " + end + "]";
  }
}
