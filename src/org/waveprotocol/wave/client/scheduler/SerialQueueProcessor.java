/**
 * Copyright 2009 Google Inc.
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

package org.waveprotocol.wave.client.scheduler;

import com.google.gwt.core.client.GWT;
import org.waveprotocol.wave.client.common.util.FastQueue;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Constructs a queue of items to be processed one by one.
 *
 */
public abstract class SerialQueueProcessor<T> implements Iterable<T> {
  private final Queue<T> queue;
  private final TimerService timerService;
  private final Scheduler.IncrementalTask itemProcessor = new Scheduler.IncrementalTask() {
    /** This is used for debugging in toString() */
    private T lastItem;

    @Override
    public boolean execute() {
      lastItem = queue.poll();
      process(lastItem);
      return !queue.isEmpty();
    }

    @Override
    public String toString() {
      return "SerialQueueProcessor [lastItem: " + lastItem + "]";
    }
  };

  /**
   * Create a serial queue processor which serializes processing of the queued items.
   *
   * @param timerService
   */
  public SerialQueueProcessor(TimerService timerService) {
    this.timerService = timerService;
    if (GWT.isClient()) {
      queue = new FastQueue<T>();
    } else {
      queue = new LinkedList<T>();
    }
  }


  /**
   * Add an item to be processed.
   */
  public void add(T item) {
    queue.add(item);
    if (queue.size() == 1) {
      timerService.schedule(itemProcessor);
    }
  }

  /**
   * @return iterator over the items that does not allow remove.
   */
  public Iterator<T> iterator() {
    final Iterator<T> iter = queue.iterator();
    return new Iterator<T>() {

      @Override
      public boolean hasNext() {
        return iter.hasNext();
      }

      @Override
      public T next() {
        return iter.next();
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  /**
   * Abstract method invoked when there is an item to be processed.
   * @param item
   */
  public abstract void process(T item);
}
