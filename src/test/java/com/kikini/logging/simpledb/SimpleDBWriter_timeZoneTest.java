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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.xerox.amazonws.sdb.Domain;
import com.xerox.amazonws.sdb.ItemAttribute;

/**
 * Tests of the {@link SimpleDBWriter} class related to time zones
 * 
 * @author Gabe Nell
 */
public class SimpleDBWriter_timeZoneTest {

    private Domain domain = mock(Domain.class);
    private SimpleDBWriter writer = new SimpleDBWriter(domain);
    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Map> argument = ArgumentCaptor.forClass(Map.class);
    private DateTime now = new DateTime(2010, 2, 1, 12, 0, 0, 0, DateTimeZone.UTC);
    private SimpleDBRow row = new SimpleDBRow("test", null, null, "level", now.getMillis(), 1);

    @Before
    public void setUp() {
        writer.setTimeZone(DateTimeZone.UTC);
    }

    private String getTimeValueFromSingleRow(Map<String, List<ItemAttribute>> rows) {
        Collection<List<ItemAttribute>> vals = rows.values();
        assertEquals(1, vals.size());
        List<ItemAttribute> columns = vals.iterator().next();
        for (ItemAttribute col : columns) {
            if ("time".equals(col.getName())) {
                return col.getValue();
            }
        }
        throw new AssertionError("Did not find time column");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void timeZoneUTC() throws Exception {
        writer.writeRows(Collections.singletonList(row));
        verify(domain).batchPutAttributes(argument.capture());
        assertEquals("2010-02-01T12:00:00.000Z", getTimeValueFromSingleRow(argument.getValue()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void changeTimeZone() throws Exception {
        writer.setTimeZone(DateTimeZone.forID("America/Los_Angeles"));
        writer.writeRows(Collections.singletonList(row));
        verify(domain).batchPutAttributes(argument.capture());
        assertEquals("2010-02-01T04:00:00.000-08:00", getTimeValueFromSingleRow(argument.getValue()));
    }

}
