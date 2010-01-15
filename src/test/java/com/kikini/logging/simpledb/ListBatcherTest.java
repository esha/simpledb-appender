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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

/**
 * Tests for the {@link ListBatcher} class
 * 
 * @author Gabe Nell
 */
public class ListBatcherTest {

    /**
     * Validates we cannot provide an invalid batch size
     */
    @Test(expected = IllegalArgumentException.class)
    public void zeroSizeBatch() {
        List<Integer> smallList = Collections.singletonList(1);
        @SuppressWarnings("unused")
        ListBatcher<Integer> batcher = new ListBatcher<Integer>(smallList, 0);
    }

    /**
     * Validates behavior on an empty input
     */
    @Test
    public void emptyList() {
        List<String> empty = Collections.emptyList();
        ListBatcher<String> batcher = new ListBatcher<String>(empty, 10);
        assertTrue(batcher.nextBatch() == null);
        assertTrue(batcher.nextBatch() == null);
    }

    /**
     * Validate behavior on a list smaller than the batch size
     */
    @Test
    public void listSmallerThanBatchSize() {
        List<Integer> smallList = Arrays.asList(1, 2, 3);
        ListBatcher<Integer> batcher = new ListBatcher<Integer>(smallList, 10);
        assertTrue(batcher.nextBatch().equals(smallList));
        assertTrue(batcher.nextBatch() == null);
    }

    /**
     * Validate behavior on a list equal to batch size
     */
    @Test
    public void listEqualToBatchSize() {
        List<Integer> equalList = new ArrayList<Integer>();
        for (int i = 1; i <= 25; i++) {
            equalList.add(i);
        }
        ListBatcher<Integer> batcher = new ListBatcher<Integer>(equalList, 25);
        assertTrue(batcher.nextBatch().equals(equalList));
        assertTrue(batcher.nextBatch() == null);
    }

    /**
     * Validate behavior on a list bigger than the batch size
     */
    @Test
    public void listBiggerThanBatchSize() {
        List<Integer> bigList = new ArrayList<Integer>();
        for (int i = 1; i <= 30; i++) {
            bigList.add(i);
        }
        // first batch
        List<Integer> firstBatch = new ArrayList<Integer>();
        for (int i = 1; i <= 25; i++) {
            firstBatch.add(i);
        }
        // second batch
        List<Integer> secondBatch = new ArrayList<Integer>();
        for (int i = 26; i <= 30; i++) {
            secondBatch.add(i);
        }
        ListBatcher<Integer> batcher = new ListBatcher<Integer>(bigList, 25);
        assertTrue(batcher.nextBatch().equals(firstBatch));
        assertTrue(batcher.nextBatch().equals(secondBatch));
        assertTrue(batcher.nextBatch() == null);
    }
}
