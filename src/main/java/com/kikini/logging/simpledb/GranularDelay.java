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

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * An implementation of {@link Delayed} which rounds the delay up to the
 * granularity specified. The effect is to create a window of a particular
 * length of time, so that all {@link GranularDelay} objects created in that
 * time window will expire simultaneously.
 * 
 * @author Gabe Nell
 */
class GranularDelay implements Delayed {

    Clock clock;
    long expire;

    /**
     * Simple class to abstract getting the current time. Purpose is to make
     * testing easier.
     */
    class Clock {

        long getCurrentTimeMillis() {
            return System.currentTimeMillis();
        }
    }

    private void initialize(Clock clock, long granularityMillis) {
        if (granularityMillis < 1) {
            throw new IllegalArgumentException("granularity must be greater than 0");
        }
        this.clock = clock;
        expire = clock.getCurrentTimeMillis() / granularityMillis;
        expire = (expire + 1) * granularityMillis;
    }

    /** Package-private constructor for test */
    GranularDelay(Clock c, long granularityMillis) {
        initialize(c, granularityMillis);
    }

    /**
     * Create a new GranularDelay instance with the given granularity
     * 
     * @param granularityMillis
     *        The length of the time window. Must be greater than 0
     */
    public GranularDelay(long granularityMillis) {
        initialize(new Clock(), granularityMillis);
    }

    public long getDelay(TimeUnit unit) {
        long remaining = expire - clock.getCurrentTimeMillis();
        return unit.convert(remaining, TimeUnit.MILLISECONDS);
    }

    public int compareTo(Delayed o) {
        long diff = getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS);
        if (diff < 0)
            return -1;
        else if (diff > 0)
            return 1;
        else
            return 0;
    }
}
