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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.concurrent.BlockingQueue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.LoggingEvent;

import com.google.common.collect.ImmutableMap;

import com.amazonaws.services.simpledb.AmazonSimpleDB;

/**
 * Tests for the {@link SimpleDBAppender} class
 * 
 * @author Gabe Nell
 */
public class SimpleDBAppenderTest {

    AmazonSimpleDB sdb;
    String dom;
    SimpleDBConsumer consumer;
    SimpleDBWriter writer;
    BlockingQueue<SimpleDBRow> queue;
    String instanceId;
    SimpleDBAppender appender;
    LoggingEvent event;
    String loggerName;
    Level level;
    ArgumentCaptor<SimpleDBRow> argument;

    /**
     * Call a single logging event
     */
    @SuppressWarnings("unchecked")
    @Before
    public void setUp() {
        sdb = mock(AmazonSimpleDB.class);
        dom = "test";
        consumer = mock(SimpleDBConsumer.class);
        writer = mock(SimpleDBWriter.class);
        queue = mock(BlockingQueue.class);
        instanceId = "i-001";
        appender = new SimpleDBAppender(sdb, dom, consumer, writer, queue, instanceId);
        event = mock(LoggingEvent.class);
        loggerName = "logger";
        level = Level.toLevel(Level.INFO_INT);
        argument = ArgumentCaptor.forClass(SimpleDBRow.class);
        when(event.getLoggerName()).thenReturn(loggerName);
        when(event.getLevel()).thenReturn(level);
        when(event.getFormattedMessage()).thenReturn("this is a log message");
        when(event.getTimeStamp()).thenReturn(1500000000000L);
        when(event.getMDCPropertyMap()).thenReturn(ImmutableMap.of("key", "value"));
    }

    /**
     * Make sure we added exactly one event to the queue
     */
    @Test
    public void exactlyOneEventQueued() {
        appender.append(event);
        verify(queue).add(isA(SimpleDBRow.class));
        verifyNoMoreInteractions(queue);
    }

    /**
     * Test that the logger was set correctly
     */
    @Test
    public void rowLoggerSet() {
        appender.append(event);
        verify(queue).add(argument.capture());
        SimpleDBRow row = argument.getValue();
        assertTrue(row.getLogger().equals(loggerName));
    }

    /**
     * Test that the level was set correctly
     */
    @Test
    public void rowLevelSet() {
        appender.append(event);
        verify(queue).add(argument.capture());
        SimpleDBRow row = argument.getValue();
        assertTrue(row.getLevel().equals(level.toString()));
    }

    /**
     * Test that the time was set correctly
     */
    @Test
    public void rowTimeSet() {
        appender.append(event);
        verify(queue).add(argument.capture());
        SimpleDBRow row = argument.getValue();
        assertTrue(row.getTime() == 1500000000000L);
    }

    /**
     * Test that the context is null since we did not provide it
     */
    @Test
    public void rowNullContext() {
        appender.append(event);
        verify(queue).add(argument.capture());
        SimpleDBRow row = argument.getValue();
        assertNull(row.getContext());
    }

    /**
     * Test that the context is set correctly when we provide it
     */
    @Test
    public void rowNonNullContext() {
        appender.setContextName("com.kikini.test");
        appender.append(event);
        verify(queue).add(argument.capture());
        SimpleDBRow row = argument.getValue();
        assertTrue(row.getContext().equals("com.kikini.test"));
        appender.setContextName(null);
    }

    /**
     * Test that the instance-id was set correctly
     */
    @Test
    public void rowInstanceId() {
        appender.append(event);
        verify(queue).add(argument.capture());
        SimpleDBRow row = argument.getValue();
        assertTrue(row.getHost().equals(instanceId));
    }

    /**
     * Test that the MDC property map was set correctly
     */
    @Test
    public void rowMdcPropertyMapSet() {
        appender.append(event);
        verify(queue).add(argument.capture());
        SimpleDBRow row = argument.getValue();
        assertTrue(row.getMDCPropertyMap().containsKey("key"));
    }

}
