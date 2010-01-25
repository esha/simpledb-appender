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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * Data representation of a row to be written to SimpleDB. Implements
 * {@link Delayed} so it may be added to a {@link BlockingQueue}.
 * 
 * @author Gabe Nell
 */
class SimpleDBRow implements Delayed {

    private final Delayed delayed;

    // Properties
    private String msg;
    private String host;
    private String component;
    private String level;
    private long time;

    SimpleDBRow(String msg, String host, String component, String level, long time, long granularity) {
        this.msg = msg;
        this.host = host;
        this.component = component;
        this.level = level;
        this.time = time;
        this.delayed = new GranularDelay(granularity);
    }

    public String getMsg() {
        return msg;
    }

    public String getHost() {
        return host;
    }

    public String getComponent() {
        return component;
    }

    public String getLevel() {
        return level;
    }

    public long getTime() {
        return time;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return delayed.getDelay(unit);
    }

    @Override
    public int compareTo(Delayed o) {
        return delayed.compareTo(o);
    }
}
