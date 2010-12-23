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

package org.waveprotocol.box.server.waveserver;

import com.google.common.util.concurrent.AbstractCheckedFuture;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.ListenableFuture;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

/**
 * Utility methods for futures.
 *
 * @author soren@google.com (Soren Lassen)
 */
public class FutureUtil {

  /**
   * Wraps an unexpected exception as a {@link RuntimeException}.
   */
  public static class UnexpectedExceptionFromFuture extends RuntimeException {
    public UnexpectedExceptionFromFuture(Throwable cause) {
      super(cause);
    }
  }

  /**
   * Adapts a ListenableFuture as a CheckedFuture. The future is expected to
   * report only one type of exception, wrapped in an ExecutionException. The
   * returned checked future unwraps it and throws it directly.
   *
   * <p>
   * A cancellation runtime exception from the future is re-thrown as is. If
   * {@code get()} throws an InterruptedException or an ExecutionException not
   * containing the expected type <E>, the InterruptedException or wrapped
   * exception is wrapped in the expected exception type if that type provides a
   * single-parameter {@code Throwable} constructor, else it is wrapped in a
   * runtime exception.
   *
   * @param future future to check
   * @param exceptionClass class of exception expected to be wrapped
   */
  public static <V, E extends Exception> CheckedFuture<V, E> check(ListenableFuture<V> future,
      final Class<E> exceptionClass) {
    return new AbstractCheckedFuture<V, E>(future) {
      @Override
      protected E mapException(Exception e) {
        if (e instanceof CancellationException) {
          throw (CancellationException) e;
        } else if (e instanceof ExecutionException) {
          Throwable cause = e.getCause();
          if (exceptionClass.isInstance(cause)) {
            return exceptionClass.cast(cause);
          } else {
            return instantiateException(exceptionClass, cause);
          }
        }
        // From construction of AbstractCheckedFuture, e must be
        // InterruptedException. Attempt to wrap it in <E>.
        return instantiateException(exceptionClass, e);
      }
    };
  }

  /**
   * Attempts to construct and return an exception of type E using its
   * single-argument {@code Throwable} constructor.
   *
   * @param exceptionClass exception type to construct
   * @param toWrap throwable to wrap
   * @throws UnexpectedExceptionFromFuture if the exception class is not
   *         instantiable with a {@code Throwable} parameter
   */
  private static <E> E instantiateException(Class<E> exceptionClass, Throwable toWrap) {
    try {
      Constructor<E> ctor = exceptionClass.getConstructor(Throwable.class);
      return ctor.newInstance(toWrap);
    } catch (SecurityException e) {
      throw new UnexpectedExceptionFromFuture(e);
    } catch (NoSuchMethodException e) {
      throw new UnexpectedExceptionFromFuture(e);
    } catch (InvocationTargetException e) {
      throw new UnexpectedExceptionFromFuture(e);
    } catch (InstantiationException e) {
      throw new UnexpectedExceptionFromFuture(e);
    } catch (IllegalAccessException e) {
      throw new UnexpectedExceptionFromFuture(e);
    }
  }

  private FutureUtil() { } // prevent instantiation
}
