/**
 * Copyright (C) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.waveprotocol.box.server.rpc.testing;

import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;


/**
  * An {@code RpcController} that just handles error text and failure condition.
  */
public class FakeRpcController implements RpcController {
  private boolean failed = false;
  private String errorText = null;

  @Override
  public String errorText() {
    return errorText;
  }

  @Override
  public boolean failed() {
    return failed;
  }

  @Override
  public boolean isCanceled() {
    return false;
  }

  @Override
  public void notifyOnCancel(RpcCallback<Object> arg) {
  }

  @Override
  public void reset() {
    failed = false;
    errorText = null;
  }

  @Override
  public void setFailed(String error) {
    failed = true;
    errorText = error;
  }

  @Override
  public void startCancel() {
  }
}
