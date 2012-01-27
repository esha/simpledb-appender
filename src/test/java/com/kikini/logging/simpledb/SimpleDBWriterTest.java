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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.model.BatchPutAttributesRequest;
import com.amazonaws.services.simpledb.model.ReplaceableAttribute;
import com.amazonaws.services.simpledb.model.ReplaceableItem;

import com.google.common.collect.ImmutableMap;

/**
 * Tests for the {@link SimpleDBWriter} class
 * 
 * @author Gabe Nell
 */
public class SimpleDBWriterTest {

    private AmazonSimpleDB sdb;    
    private String dom;
    private SimpleDBWriter writer;
    private List<SimpleDBRow> rows;
    @SuppressWarnings("unchecked")
    ArgumentCaptor<BatchPutAttributesRequest> argument;

    /**
     * Creates three rows and sets up a mock Domain and argument capture
     */
    @Before
    public void setUp() {
        DateTime now = new DateTime(2010, 2, 1, 12, 0, 0, 0, DateTimeZone.UTC);
        SimpleDBRow row1 = new SimpleDBRow("test msg 1", "i-001", "com.kikini.test", "logger", "level", now.getMillis(), 1, ImmutableMap.of("key", "value"));
        SimpleDBRow row2 = new SimpleDBRow("test msg 2", "i-001", "com.kikini.test", "logger", "level", now.plusMinutes(1).getMillis(), 1, ImmutableMap.of("key", "value"));
        SimpleDBRow row3 = new SimpleDBRow("test msg 3", "i-001", "com.kikini.test", "logger", "level", now.plusMinutes(2).getMillis(), 1, ImmutableMap.of("key", "value"));
        rows = Arrays.asList(row1, row2, row3);
        sdb = mock(AmazonSimpleDB.class);
        dom = "test";
        writer = new SimpleDBWriter(sdb, dom);
        argument = ArgumentCaptor.forClass(BatchPutAttributesRequest.class);
    }

    /**
     * Checks that the right number of rows were written
     */
    @SuppressWarnings("unchecked")
    @Test
    public void writerResultSizeTest() {
        writer.writeRows(rows);
        verify(sdb).batchPutAttributes(argument.capture());
        List<ReplaceableItem> items = argument.getValue().getItems();
        assertTrue(items.size() == 3);
    }

    /**
     * Checks that each row has the right number of columns
     */
    @SuppressWarnings("unchecked")
    @Test
    public void writerResultElementSizesTest() {
        writer.writeRows(rows);
        verify(sdb).batchPutAttributes(argument.capture());
        List<ReplaceableItem> items = argument.getValue().getItems();
        for (ReplaceableItem item : items) {
            List<ReplaceableAttribute> vals = item.getAttributes();
            assertTrue(vals.size() == 7);
        }
    }

    /**
     * Verify nothing happens when an empty list is given as an argument
     */
    @Test
    public void emptyArgumentTest() {
        writer.writeRows(new ArrayList<SimpleDBRow>());
        verifyZeroInteractions(sdb);
    }

    /**
     * Verifies that we truncate long values
     */
    @SuppressWarnings("unchecked")
    @Test
    public void longAttributesAreTruncatedTest() {
        Charset utf8 = Charset.forName("UTF-8");
        boolean checked = false;
        String longMsg = "";
        for (int i = 0; i < 1000; i++) {
            // this character works out to 3 bytes in UTF-8, so this string will
            // be 3000 bytes
            longMsg = longMsg + '花';
        }
        SimpleDBRow row1 = new SimpleDBRow(longMsg, "i-001", "com.kikini.test", "logger", "level", 1000000000000L, 1, ImmutableMap.of("key", "value"));
        writer.writeRows(Collections.singletonList(row1));
        verify(sdb).batchPutAttributes(argument.capture());
        List<ReplaceableItem> items = argument.getValue().getItems();
        for (ReplaceableItem item : items) {
            for (ReplaceableAttribute att : item.getAttributes()) {
                assertTrue(att.getName().getBytes(utf8).length <= 1024);
                assertTrue(att.getValue().getBytes(utf8).length <= 1024);
                checked = true;
            }
        }
        assertTrue(checked);
    }

    /**
     * Verifies we put at most 25 attributes at a time
     */
    @SuppressWarnings("unchecked")
    @Test
    public void putsAreBatchedTest() {
        List<SimpleDBRow> tooManyRows = new ArrayList<SimpleDBRow>();
        for (int i = 0; i < 30; i++) {
            tooManyRows.add(new SimpleDBRow("test msg " + i, "i-001", "com.kikini.test", "logger", "level", 1000000000000L, 1,  ImmutableMap.of("key", "value")));
        }
        writer.writeRows(tooManyRows);
        verify(sdb, times(2)).batchPutAttributes(argument.capture());
        List<BatchPutAttributesRequest> captured = argument.getAllValues();
        assertTrue(captured.size() == 2);
        List<ReplaceableItem> items1 = captured.get(0).getItems();
        assertTrue(items1.size() == 25);
        List<ReplaceableItem> items2 = captured.get(1).getItems();
        assertTrue(items2.size() == 5);
    }
}
