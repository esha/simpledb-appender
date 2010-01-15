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
import java.util.Locale;
import java.util.Map;

import org.joda.time.chrono.ISOChronology;
import org.joda.time.format.DateTimeParser;
import org.joda.time.format.DateTimeParserBucket;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.xerox.amazonws.sdb.Domain;
import com.xerox.amazonws.sdb.ItemAttribute;
import com.xerox.amazonws.sdb.SDBException;

/**
 * Tests for the {@link SimpleDBWriter} class
 * 
 * @author Gabe Nell
 */
public class SimpleDBWriterTest {

    private Domain dom;
    private SimpleDBWriter writer;
    private List<SimpleDBRow> rows;
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map> argument;

    /**
     * Creates three rows and sets up a mock Domain and argument capture
     */
    @Before
    public void setUp() {
        // The three times correspond to 2001-09-08T18:46:40.000-07:00,
        // 2009-09-04T15:00:00.000-07:00, and 2009-11-04T15:00:00.000-08:00
        SimpleDBRow row1 = new SimpleDBRow("test msg 1", "i-001", "com.kikini.test", "level", 1000000000000L, 1);
        SimpleDBRow row2 = new SimpleDBRow("test msg 2", "i-001", "com.kikini.test", "level", 1252101600000L, 1);
        SimpleDBRow row3 = new SimpleDBRow("test msg 3", "i-001", "com.kikini.test", "level", 1257375600000L, 1);
        rows = Arrays.asList(row1, row2, row3);
        dom = mock(Domain.class);
        writer = new SimpleDBWriter(dom);
        argument = ArgumentCaptor.forClass(Map.class);
    }

    /**
     * Checks that the right number of rows were written
     * 
     * @throws SDBException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void writerResultSizeTest() throws SDBException {
        writer.writeRows(rows);
        verify(dom).batchPutAttributes(argument.capture());
        Map<String, List<ItemAttribute>> map = argument.getValue();
        assertTrue(map.size() == 3);
    }

    /**
     * Checks that each row has the right number of columns
     * 
     * @throws SDBException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void writerResultElementSizesTest() throws SDBException {
        writer.writeRows(rows);
        verify(dom).batchPutAttributes(argument.capture());
        Collection<List<ItemAttribute>> vals = argument.getValue().values();
        for (List<ItemAttribute> val : vals) {
            assertTrue(val.size() == 5);
        }
    }

    /**
     * Verify nothing happens when an empty list is given as an argument
     */
    @Test
    public void emptyArgumentTest() {
        writer.writeRows(new ArrayList<SimpleDBRow>());
        verifyZeroInteractions(dom);
    }

    private void verifyTimeWithOffset(Collection<List<ItemAttribute>> vals, int offset) {
        boolean checked = false; // make sure we actually checked
        DateTimeParser parser = ISODateTimeFormat.dateTimeParser().getParser();
        for (List<ItemAttribute> val : vals) {
            for (ItemAttribute att : val) {
                if (att.getName().equals("time")) {
                    DateTimeParserBucket bucket = new DateTimeParserBucket(0, ISOChronology.getInstance(), Locale.US);
                    parser.parseInto(bucket, att.getValue(), 0);
                    assertTrue(bucket.getOffset() == offset);
                    checked = true;
                }
            }
        }
        assertTrue(checked);
    }

    /**
     * Verifies the offset is correct for pacific time when daylight savings is
     * in effect
     * 
     * @throws SDBException
     */
    @Test
    @SuppressWarnings("unchecked")
    public void timezonePDTTest() throws SDBException {
        // rows[1] should be in DST
        writer.writeRows(Collections.singletonList(rows.get(1)));
        verify(dom).batchPutAttributes(argument.capture());
        Collection<List<ItemAttribute>> vals = argument.getValue().values();
        // -7h = -25200000
        verifyTimeWithOffset(vals, -25200000);
    }

    /**
     * Verifies the offset is correct for pacific time when daylight savings is
     * not in effect
     * 
     * @throws SDBException
     */
    @Test
    @SuppressWarnings("unchecked")
    public void timezonePSTTest() throws SDBException {
        // rows[2] should be in Standard time
        writer.writeRows(Collections.singletonList(rows.get(2)));
        verify(dom).batchPutAttributes(argument.capture());
        Collection<List<ItemAttribute>> vals = argument.getValue().values();
        // -8h = -28800000
        verifyTimeWithOffset(vals, -28800000);
    }

    /**
     * Verifies that we truncate long values
     * 
     * @throws SDBException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void longAttributesAreTruncatedTest() throws SDBException {
        Charset utf8 = Charset.forName("UTF-8");
        boolean checked = false;
        String longMsg = "";
        for (int i = 0; i < 1000; i++) {
            // this character works out to 3 bytes in UTF-8, so this string will
            // be 3000 bytes
            longMsg = longMsg + '花';
        }
        SimpleDBRow row1 = new SimpleDBRow(longMsg, "i-001", "com.kikini.test", "level", 1000000000000L, 1);
        writer.writeRows(Collections.singletonList(row1));
        verify(dom).batchPutAttributes(argument.capture());
        Collection<List<ItemAttribute>> vals = argument.getValue().values();
        for (List<ItemAttribute> val : vals) {
            for (ItemAttribute att : val) {
                assertTrue(att.getName().getBytes(utf8).length <= 1024);
                assertTrue(att.getValue().getBytes(utf8).length <= 1024);
                checked = true;
            }
        }
        assertTrue(checked);
    }

    /**
     * Verifies we put at most 25 attributes at a time
     * 
     * @throws SDBException
     */
    @SuppressWarnings("unchecked")
    @Test
    public void putsAreBatchedTest() throws SDBException {
        List<SimpleDBRow> tooManyRows = new ArrayList<SimpleDBRow>();
        for (int i = 0; i < 30; i++) {
            tooManyRows.add(new SimpleDBRow("test msg " + i, "i-001", "com.kikini.test", "level", 1000000000000L, 1));
        }
        writer.writeRows(tooManyRows);
        verify(dom, times(2)).batchPutAttributes(argument.capture());
        List<Map> captured = argument.getAllValues();
        assertTrue(captured.size() == 2);
        Collection<List<ItemAttribute>> vals1 = captured.get(0).values();
        assertTrue(vals1.size() == 25);
        Collection<List<ItemAttribute>> vals2 = captured.get(1).values();
        assertTrue(vals2.size() == 5);
    }
}
