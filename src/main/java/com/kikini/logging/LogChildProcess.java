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
import java.io.IOException;
import java.io.InputStreamReader;

import org.slf4j.Logger;

import com.kikini.logging.LineLogger.LogAtLevel;

/**
 * Command line wrapper to launch a process and log console output to SLF4J,
 * directing {@code stdout} to {@link Logger#info(String)} and {@code stderr} to
 * {@link Logger#error(String)}
 * 
 * @author Gabe Nell
 */
public class LogChildProcess {

    /** Main */
    public static void main(String[] args) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(args);
        Process p = pb.start();
        InputStreamReader stdout = new InputStreamReader(new BufferedInputStream(p.getInputStream()));
        new Thread(new LineLogger(stdout, LogAtLevel.INFO)).start();
        InputStreamReader stderr = new InputStreamReader(new BufferedInputStream(p.getErrorStream()));
        new Thread(new LineLogger(stderr, LogAtLevel.ERROR)).start();
    }

}
