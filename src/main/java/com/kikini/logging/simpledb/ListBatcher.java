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

import java.util.List;

/**
 * Class to aid in iterating through a {@link List} in batches of a particular
 * size
 * 
 * @author Gabe Nell
 * @param <E>
 *        the type of {@link List} to batch
 */
class ListBatcher<E> {

    private final int batchSize;
    private final List<E> list;
    private int idx = 0;

    /**
     * Constructor to batch-iterate over the given {@link List}. The given
     * {@link List} is used directly, so any modifications to it will affect the
     * operation of this class.
     * 
     * @param list
     *        the {@link List} to iterate over.
     * @param batchSize
     *        the size of the sublist to return when calling
     *        {@link ListBatcher#nextBatch()}
     */
    ListBatcher(List<E> list, int batchSize) {
        if (batchSize < 1) throw new IllegalArgumentException("Must choose a batch size greater than 0");
        this.list = list;
        this.batchSize = batchSize;
    }

    /**
     * Returns a {@link List} of the batch size specified during construction.
     * May return a smaller list if too few elements remain.
     * 
     * @return the next batch, or null if we have already iterated over the list
     */
    public List<E> nextBatch() {
        if (idx >= list.size()) {
            return null;
        }
        int nextIdx = ((idx + batchSize) < list.size()) ? (idx + batchSize) : list.size();
        List<E> subList = list.subList(idx, nextIdx);
        idx = nextIdx;
        return subList;
    }
}
