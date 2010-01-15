/*
 * Copyright 2009-2010 Kikini Limited and contributors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.kikini.logging.simpledb;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * {@link Runnable} meant to be used with
 * {@link Runtime#addShutdownHook(Thread)}. Ensures that the
 * {@link BlockingQueue} is fully drained to the {@link SimpleDBWriter} when the
 * application shuts down.
 * <p>
 * This is necessary because the {@link SimpleDBConsumer} thread is a daemon
 * which would be terminated immediately once all normal threads are done. This
 * thread interrupts the consumer thread, and once that thread is done, proceeds
 * to drain the queue. The normal delay/waiting behavior of the queue is not
 * respected here since the system is shutting down.
 * 
 * @author Gabe Nell
 */
class SimpleDBShutdownHook implements Runnable {

    private BlockingQueue<SimpleDBRow> queue;
    private SimpleDBWriter writer;
    private Thread consumerThread;

    SimpleDBShutdownHook(BlockingQueue<SimpleDBRow> queue, SimpleDBWriter writer, Thread consumerThread) {
        this.queue = queue;
        this.writer = writer;
        this.consumerThread = consumerThread;
    }

    /**
     * Drains the queue by actually removing the items and writing them out.
     * This circumvents any underlying delay behavior in the blocking queue.
     */
    private void drainQueue() {
        List<SimpleDBRow> rows = new ArrayList<SimpleDBRow>();
        while (!queue.isEmpty()) {
            SimpleDBRow row = queue.peek();
            rows.add(row);
            queue.remove(row);
        }
        writer.writeRows(rows);
    }

    @Override
    public void run() {
        // we don't want this shutdown handler and the consumer thread to step
        // on each others' toes. Interrupt the consumer thread and wait for it
        // to stop before continuing.
        consumerThread.interrupt();
        try {
            consumerThread.join();
        } catch (InterruptedException e) {
            // unexpected and not clear what we should do. bail.
            return;
        }

        // all clear, let's drain the queue
        drainQueue();
    }

}
