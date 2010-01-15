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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.Test;

import com.kikini.logging.LineLogger.LogAtLevel;

/**
 * Tests for the {@link LineLogger} class
 * 
 * @author Gabe Nell
 */
public class LineLoggerTest {

    private static final String CR = "\r";
    private static final String LF = "\n";
    private static final String CRLF = "\r\n";
    private static final String STRING1 = "hello";
    private static final String STRING2 = "there is nothing more I love in this wonderful world than testing string functions!!!!";
    private static final String STRING3 = "OR NOT.";

    /**
     * @throws IOException
     */
    @Test
    public void simpleCRStringTest() throws IOException {
        String s = STRING1 + CR + STRING2 + CR + CR + STRING3;
        InputStream is = new ByteArrayInputStream(s.getBytes());
        LineLogger lineLogger = new LineLogger(new InputStreamReader(is), LogAtLevel.INFO);
        assertTrue(lineLogger.readLine().equals(STRING1));
        assertTrue(lineLogger.readLine().equals(STRING2));
        assertTrue(lineLogger.readLine().equals(STRING3));
        assertNull(lineLogger.readLine());
    }

    /**
     * @throws IOException
     */
    @Test
    public void simpleLFStringTest() throws IOException {
        String s = STRING1 + LF + STRING2 + LF + LF + STRING3;
        InputStream is = new ByteArrayInputStream(s.getBytes());
        LineLogger lineLogger = new LineLogger(new InputStreamReader(is), LogAtLevel.INFO);
        assertTrue(lineLogger.readLine().equals(STRING1));
        assertTrue(lineLogger.readLine().equals(STRING2));
        assertTrue(lineLogger.readLine().equals(STRING3));
        assertNull(lineLogger.readLine());
    }

    /**
     * @throws IOException
     */
    @Test
    public void simpleCRLFStringTest() throws IOException {
        String s = STRING1 + CRLF + STRING2 + CRLF + CRLF + STRING3;
        InputStream is = new ByteArrayInputStream(s.getBytes());
        LineLogger lineLogger = new LineLogger(new InputStreamReader(is), LogAtLevel.INFO);
        assertTrue(lineLogger.readLine().equals(STRING1));
        assertTrue(lineLogger.readLine().equals(STRING2));
        assertTrue(lineLogger.readLine().equals(STRING3));
        assertNull(lineLogger.readLine());
    }

    /**
     * @throws IOException
     */
    @Test
    public void stringOnlyTest() throws IOException {
        String s = STRING2;
        InputStream is = new ByteArrayInputStream(s.getBytes());
        LineLogger lineLogger = new LineLogger(new InputStreamReader(is), LogAtLevel.INFO);
        assertTrue(lineLogger.readLine().equals(STRING2));
        assertNull(lineLogger.readLine());
    }

    /**
     * @throws IOException
     */
    @Test
    public void leadingCRLFTest() throws IOException {
        String s = CRLF + CRLF + STRING1;
        InputStream is = new ByteArrayInputStream(s.getBytes());
        LineLogger lineLogger = new LineLogger(new InputStreamReader(is), LogAtLevel.INFO);
        assertTrue(lineLogger.readLine().equals(STRING1));
        assertNull(lineLogger.readLine());
    }

    /**
     * @throws IOException
     */
    @Test
    public void leadingCRTest() throws IOException {
        String s = CR + CR + STRING1;
        InputStream is = new ByteArrayInputStream(s.getBytes());
        LineLogger lineLogger = new LineLogger(new InputStreamReader(is), LogAtLevel.INFO);
        assertTrue(lineLogger.readLine().equals(STRING1));
        assertNull(lineLogger.readLine());
    }

    /**
     * @throws IOException
     */
    @Test
    public void leadingLFTest() throws IOException {
        String s = LF + LF + STRING1;
        InputStream is = new ByteArrayInputStream(s.getBytes());
        LineLogger lineLogger = new LineLogger(new InputStreamReader(is), LogAtLevel.INFO);
        assertTrue(lineLogger.readLine().equals(STRING1));
        assertNull(lineLogger.readLine());
    }

    /**
     * @throws IOException
     */
    @Test
    public void trailingCRLFTest() throws IOException {
        String s = STRING1 + CRLF;
        InputStream is = new ByteArrayInputStream(s.getBytes());
        LineLogger lineLogger = new LineLogger(new InputStreamReader(is), LogAtLevel.INFO);
        assertTrue(lineLogger.readLine().equals(STRING1));
        assertNull(lineLogger.readLine());
    }

    /**
     * @throws IOException
     */
    @Test
    public void trailingLFTest() throws IOException {
        String s = STRING1 + LF;
        InputStream is = new ByteArrayInputStream(s.getBytes());
        LineLogger lineLogger = new LineLogger(new InputStreamReader(is), LogAtLevel.INFO);
        assertTrue(lineLogger.readLine().equals(STRING1));
        assertNull(lineLogger.readLine());
    }

    /**
     * @throws IOException
     */
    @Test
    public void trailingCRTest() throws IOException {
        String s = STRING1 + CR;
        InputStream is = new ByteArrayInputStream(s.getBytes());
        LineLogger lineLogger = new LineLogger(new InputStreamReader(is), LogAtLevel.INFO);
        assertTrue(lineLogger.readLine().equals(STRING1));
        assertNull(lineLogger.readLine());
    }

    /**
     * @throws IOException
     */
    @Test
    public void surroundedStringTest() throws IOException {
        String s = CRLF + STRING1 + CR;
        InputStream is = new ByteArrayInputStream(s.getBytes());
        LineLogger lineLogger = new LineLogger(new InputStreamReader(is), LogAtLevel.INFO);
        assertTrue(lineLogger.readLine().equals(STRING1));
        assertNull(lineLogger.readLine());
    }

    /**
     * @throws IOException
     */
    @Test
    public void onlyCRTest() throws IOException {
        String s = CR;
        InputStream is = new ByteArrayInputStream(s.getBytes());
        LineLogger lineLogger = new LineLogger(new InputStreamReader(is), LogAtLevel.INFO);
        assertNull(lineLogger.readLine());
    }

    /**
     * @throws IOException
     */
    @Test
    public void onlyLFTest() throws IOException {
        String s = LF;
        InputStream is = new ByteArrayInputStream(s.getBytes());
        LineLogger lineLogger = new LineLogger(new InputStreamReader(is), LogAtLevel.INFO);
        assertNull(lineLogger.readLine());
    }

    /**
     * @throws IOException
     */
    @Test
    public void onlyCRLFTest() throws IOException {
        String s = CRLF;
        InputStream is = new ByteArrayInputStream(s.getBytes());
        LineLogger lineLogger = new LineLogger(new InputStreamReader(is), LogAtLevel.INFO);
        assertNull(lineLogger.readLine());
    }
}
