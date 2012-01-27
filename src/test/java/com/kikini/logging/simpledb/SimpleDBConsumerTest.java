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

import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.*;

import java.util.concurrent.BlockingQueue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

/**
 * Tests for the {@link SimpleDBConsumer} class
 * 
 * @author Gabe Nell
 */
public class SimpleDBConsumerTest {

    private SimpleDBWriter writer;
    private BlockingQueue<SimpleDBRow> queue;
    SimpleDBConsumer consumer;

    /**
     * Create a SimpleDBConsumer wired to a mock writer and queue
     */
    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        writer = mock(SimpleDBWriter.class);
        queue = mock(BlockingQueue.class);
        consumer = new SimpleDBConsumer(queue, writer);
    }

    /**
     * Verifies that the consumer waits (via take()) before draining and writing
     * 
     * @throws InterruptedException
     */
    @Test
    public void runTakesThenDrains() throws InterruptedException {
        SimpleDBRow row1 = new SimpleDBRow("test msg 1", "i-001", "com.kikini.test", "logger", "level", 1000000000000L, 1);
        when(queue.take()).thenReturn(row1).thenThrow(new InterruptedException());
        InOrder inOrder = inOrder(queue, writer);
        consumer.run();
        inOrder.verify(queue).take();
        inOrder.verify(queue).drainTo(anyListOf(SimpleDBRow.class));
        inOrder.verify(writer).writeRows(anyListOf(SimpleDBRow.class));
        inOrder.verify(queue).take();
    }

    /**
     * Verifies that run will return on an interruption without doing anything
     * further to the queue or the writer
     */
    @Test
    public void runReturnsOnInterrupted() throws InterruptedException {
        when(queue.take()).thenThrow(new InterruptedException());
        consumer.run();
        verify(queue).take();
        verifyNoMoreInteractions(queue);
        verifyZeroInteractions(writer);
    }
}
