/**
 * Copyright 2010 Google Inc.
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

package org.waveprotocol.box.server.persistence.file;

import org.waveprotocol.box.server.persistence.DeltaStoreTestBase;
import org.waveprotocol.box.server.waveserver.DeltaStore;

import java.io.File;
import java.io.IOException;

/**
 * Tests for FileDeltaStore.
 *
 * @author Joseph Gentle (josephg@gmail.com)
 */
public class DeltaStoreTest extends DeltaStoreTestBase {
  private File path;

  @Override
  protected void setUp() throws Exception {
    path = FileUtils.createTemporaryDirectory();
    super.setUp();
  }

  @Override
  protected DeltaStore newDeltaStore() throws IOException {
    return new FileDeltaStore(path.getAbsolutePath());
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    org.apache.commons.io.FileUtils.deleteDirectory(path);

    // This assertion may fail if a test hasn't closed all streams.
    assertFalse(path.exists());
  }

  // TODO: Test the delta store strips partially written data.
}
