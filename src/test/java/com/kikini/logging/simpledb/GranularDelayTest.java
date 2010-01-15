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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the {@link GranlurDelay} class
 * 
 * @author Gabe Nell
 */
public class GranularDelayTest {

    GranularDelay.Clock clock;
    Delayed granular;
    Delayed other;

    /**
     * Set up a 10s granularity delay somewhere mid-cycle between 120s and 130s
     */
    @Before
    public void setUp() {
        // the next time should be 130000
        long granularity = 10000L;
        clock = mock(GranularDelay.Clock.class);
        when(clock.getCurrentTimeMillis()).thenReturn(123456L);
        granular = new GranularDelay(clock, granularity);
        other = mock(Delayed.class);
    }

    /**
     * Tests that we return the right positive value
     */
    @Test
    public void getDelayPositiveTest() {
        assertTrue(granular.getDelay(TimeUnit.MILLISECONDS) == (130000L - 123456L));
    }

    /**
     * Test that we return zero when it's exactly time
     */
    @Test
    public void getDelayZeroTest() {
        when(clock.getCurrentTimeMillis()).thenReturn(130000L);
        assertTrue(granular.getDelay(TimeUnit.MILLISECONDS) == 0L);
    }

    /**
     * Test that we return the right negative value
     */
    @Test
    public void getDelayNegativeTest() {
        when(clock.getCurrentTimeMillis()).thenReturn(130000L + 100L);
        assertTrue(granular.getDelay(TimeUnit.MILLISECONDS) == -100L);
    }

    /**
     * Test that we properly convert to non-millisecond units
     */
    @Test
    public void getDelayNonMillisecondUnits() {
        when(clock.getCurrentTimeMillis()).thenReturn(130000L + 100L);
        assertTrue(granular.getDelay(TimeUnit.MICROSECONDS) == -100000L);
    }

    /**
     * Compare to a smaller value
     */
    @Test
    public void compareToSmallerTest() {
        when(other.getDelay(eq(TimeUnit.MILLISECONDS))).thenReturn(100L);
        assertTrue(granular.compareTo(other) == 1);
        when(other.getDelay(eq(TimeUnit.MILLISECONDS))).thenReturn(-8000L);
        assertTrue(granular.compareTo(other) == 1);
    }

    /**
     * Compare to an equal value
     */
    @Test
    public void compareToEqualTest() {
        when(other.getDelay(eq(TimeUnit.MILLISECONDS))).thenReturn(130000L - 123456L);
        assertTrue(granular.compareTo(other) == 0);
    }

    /**
     * Compare to a greater value
     */
    @Test
    public void compareToBiggerTest() {
        when(other.getDelay(eq(TimeUnit.MILLISECONDS))).thenReturn(8000L);
        assertTrue(granular.compareTo(other) == -1);
    }
}
