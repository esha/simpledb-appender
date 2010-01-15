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
 * {@link Runnable} to read from a {@link BlockingQueue} of {@link SimpleDBRow}
 * s, and write them to a {@link SimpleDBWriter}. The thread will wait on
 * {@link BlockingQueue#take()}, drain all the objects from the queue, then
 * write them to the writer before waiting again.
 * 
 * @author Gabe Nell
 */
class SimpleDBConsumer implements Runnable {

    private BlockingQueue<SimpleDBRow> queue;
    private SimpleDBWriter writer;

    SimpleDBConsumer(BlockingQueue<SimpleDBRow> queue, SimpleDBWriter writer) {
        this.queue = queue;
        this.writer = writer;
    }

    @Override
    public void run() {
        while (true) {
            List<SimpleDBRow> rows = new ArrayList<SimpleDBRow>();
            try {
                rows.add(queue.take());
            } catch (InterruptedException e) {
                // The thread was interrupted, perhaps by the shutdown handler.
                // Let's exit so that it can do its work.
                return;
            }
            queue.drainTo(rows);
            writer.writeRows(rows);
        }
    }
}
