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
package com.kikini.logging;

import java.io.IOException;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link Runnable} to efficiently read lines from the an
 * {@link InputStreamReader} (preferably buffered) and commit non-empty lines to
 * the SLF4J logger at the specified logging level. The thread will return when
 * the underlying {@code InputStreamReader} returns {@code EOF}.
 * 
 * @author Gabe Nell
 */
public class LineLogger implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(LineLogger.class);

    private final InputStreamReader input;
    private final LogAtLevel logAtLevel;

    /** Constructor */
    public LineLogger(InputStreamReader input, LogAtLevel level) {
        this.input = input;
        this.logAtLevel = level;
    }

    /**
     * Representation of SLF4J logging levels
     */
    public enum LogAtLevel {

        /** Debug level */
        DEBUG {

            @Override
            public void log(String msg) {
                LOGGER.debug(msg);
            }

        },

        /** Error level */
        ERROR {

            @Override
            public void log(String msg) {
                LOGGER.error(msg);
            }
        },

        /** Info level */
        INFO {

            @Override
            public void log(String msg) {
                LOGGER.info(msg);
            }

        },

        /** Trace level */
        TRACE {

            @Override
            public void log(String msg) {
                LOGGER.trace(msg);
            }

        },

        /** Warn level */
        WARN {

            @Override
            public void log(String msg) {
                LOGGER.warn(msg);
            }

        };

        /**
         * Log the message
         * 
         * @param msg
         */
        public abstract void log(String msg);
    }

    /**
     * Blocking call to read a line, or return null for EOF
     * 
     * @return the line, or null if EOF
     * @throws IOException
     */
    String readLine() throws IOException {
        // This could be optimized to do more than one character at a
        // time, but since the underlying stream is buffered it is probably OK
        String s = "";
        int c;
        while ((c = input.read()) != -1) {
            if ((c == '\r') || (c == '\n')) {
                if (s.length() > 0) {
                    return s;
                }
            } else {
                s = s + (char)c;
            }
        }
        if (s.length() == 0) {
            // EOF, and no buffer to return
            return null;
        }
        return s;
    }

    @Override
    public void run() {
        String s;
        try {
            s = readLine();
            while (s != null) {
                logAtLevel.log(s);
                s = readLine();
            }
        } catch (IOException e) {
            return;
        }
    }
}
