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

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

/**
 * Tests for the {@link SimpleDBShutdownHook} class
 * 
 * @author Gabe Nell
 */
public class SimpleDBShutdownHookTest {

    private SimpleDBWriter writer;
    private BlockingQueue<SimpleDBRow> queue;
    private SimpleDBShutdownHook shutdownHook;
    private Thread consumerThread;

    /**
     * Create a SimpleDBConsumer wired to a mock writer and queue
     */
    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        writer = mock(SimpleDBWriter.class);
        queue = mock(BlockingQueue.class);
        consumerThread = mock(Thread.class);
        shutdownHook = new SimpleDBShutdownHook(queue, writer, consumerThread);
    }

    /**
     * Validates that the queue interrupts the consumer thread before writing
     */
    @Test
    public void interruptBeforeWrite() {
        SimpleDBRow row1 = new SimpleDBRow("test msg 1", "i-001", "com.kikini.test", "logger", "level", 1000000000000L, 1);
        when(queue.isEmpty()).thenReturn(false, true);
        when(queue.peek()).thenReturn(row1);
        InOrder inOrder = inOrder(queue, writer, consumerThread);
        shutdownHook.run();
        inOrder.verify(consumerThread).interrupt();
        inOrder.verify(queue).isEmpty();
        inOrder.verify(writer).writeRows(anyListOf(SimpleDBRow.class));
        verifyNoMoreInteractions(consumerThread);
        verifyNoMoreInteractions(writer);
    }

    /**
     * Verify that the queue is written properly
     */
    @SuppressWarnings("unchecked")
    @Test
    public void queueIsWritten() {
        SimpleDBRow row1 = new SimpleDBRow("test msg 1", "i-001", "com.kikini.test", "logger", "level", 1000000000000L, 1);
        SimpleDBRow row2 = new SimpleDBRow("test msg 2", "i-001", "com.kikini.test", "logger", "level", 1500000000000L, 1);
        ArgumentCaptor<List> argument = ArgumentCaptor.forClass(List.class);
        when(queue.isEmpty()).thenReturn(false, false, true);
        when(queue.peek()).thenReturn(row1, row2);
        shutdownHook.run();
        verify(writer).writeRows(argument.capture());
        List<SimpleDBRow> list = argument.getValue();
        assertTrue(list.size() == 2);
        assertTrue(list.containsAll(Arrays.asList(row1, row2)));
        verifyNoMoreInteractions(writer);
    }

    /**
     * Verifies that an empty list is written when the queue is empty
     */
    @SuppressWarnings("unchecked")
    @Test
    public void verifyEmptyList() {
        ArgumentCaptor<List> argument = ArgumentCaptor.forClass(List.class);
        when(queue.isEmpty()).thenReturn(true);
        shutdownHook.run();
        verify(writer).writeRows(argument.capture());
        List<SimpleDBRow> list = argument.getValue();
        assertTrue(list.isEmpty());
        verifyNoMoreInteractions(writer);
    }
}
