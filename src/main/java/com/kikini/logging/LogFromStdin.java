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

import java.io.BufferedInputStream;
import java.io.InputStreamReader;

import org.slf4j.Logger;

import com.kikini.logging.LineLogger.LogAtLevel;

/**
 * Reads from {@code stdin} and directs to writes to a {@link Logger} at the
 * specified level. The typical use case (besides testing) is piping the output
 * of another process to this process.
 * 
 * @author Gabe Nell
 */
public class LogFromStdin {

    /**
     * Parses the requested level and starts the logger at the given level, or
     * {@code LogAtLevel.INFO} if none is specified.
     * 
     * @param levelRequested
     *        String value of a {@link LogAtLevel} Enum. If {@code null} is
     *        given, {@code INFO} is used.
     */
    public void runLogger(String levelRequested) {
        LogAtLevel level = LogAtLevel.INFO;
        if (levelRequested != null) {
            level = LogAtLevel.valueOf(levelRequested);
        }
        InputStreamReader is = new InputStreamReader(new BufferedInputStream(System.in));
        LineLogger lineLogger = new LineLogger(is, level);
        lineLogger.run();
    }

    /** Main */
    public static void main(String[] args) {
        if (args.length > 1) {
            System.err.println("Usage: LogFromStdin logAtLevel");
            System.err.println("(logAtLevel is optional, a value of the LogAtLevel Enum)");
        }
        new LogFromStdin().runLogger(args[0]);
    }

}
