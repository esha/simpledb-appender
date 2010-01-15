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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;

import ch.qos.logback.classic.spi.CallerData;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.status.ErrorStatus;

import com.xerox.amazonws.sdb.Domain;
import com.xerox.amazonws.sdb.SDBException;
import com.xerox.amazonws.sdb.SimpleDB;

/**
 * Logback {@link Appender} to write log data to SimpleDB
 * <p>
 * To configure, add an appender element referring to this class in your
 * logback.xml file.
 * 
 * @author Gabe Nell
 */
public class SimpleDBAppender extends AppenderBase<LoggingEvent> {

    private SimpleDB sdb = null;
    private Domain dom = null;
    private SimpleDBConsumer consumer = null;
    private SimpleDBWriter writer = null;
    private BlockingQueue<SimpleDBRow> queue = null;

    // optional properties
    private String componentName = null;
    private String instanceId = null;
    private long loggingPeriodMillis = 10000;

    // required properties
    private String domainName;
    private String accessId;
    private String secretKey;

    /**
     * Set properties common to the system from a Java {@link Properties} file
     * containing any of the following keys:
     * <ul>
     * <li>domainName
     * <li>instanceId
     * <li>accessId
     * <li>secretKey
     * </ul>
     * 
     * @param filename
     *        path to a {@link Properties} file containing the desired
     *        properties
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void setCommonProperties(String filename) throws FileNotFoundException, IOException {
        setCommonProperties(new FileInputStream(filename));
    }

    void setCommonProperties(InputStream propStream) throws IOException {
        Properties props = new Properties();
        props.load(propStream);
        String domainName = props.getProperty("domainName");
        String instanceId = props.getProperty("instanceId");
        String accessId = props.getProperty("accessId");
        String secretKey = props.getProperty("secretKey");

        if (null != domainName) setDomainName(domainName);
        if (null != instanceId) setInstanceId(instanceId);
        if (null != accessId) setAccessId(accessId);
        if (null != secretKey) setSecretKey(secretKey);
    }

    /**
     * Sets the name of the component. If none is specified, the Java class of
     * the caller is used.
     * 
     * @param componentName
     */
    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    /**
     * @param instanceId
     *        the instanceId to set
     */
    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    /**
     * @param domainName
     *        the domainName to set
     */
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    /**
     * @param loggingPeriodMillis
     *        the loggingPeriodMillis to set
     */
    public void setLoggingPeriodMillis(long loggingPeriodMillis) {
        this.loggingPeriodMillis = loggingPeriodMillis;
    }

    /**
     * @param accessId
     *        the accessId to set
     */
    public void setAccessId(String accessId) {
        this.accessId = accessId;
    }

    /**
     * @param secretKey
     *        the secretKey to set
     */
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    /**
     * Dependency-Injection constructor
     */
    public SimpleDBAppender(SimpleDB sdb, Domain dom, SimpleDBConsumer consumer, SimpleDBWriter writer,
            BlockingQueue<SimpleDBRow> queue, String instanceId) {
        this.sdb = sdb;
        this.dom = dom;
        this.consumer = consumer;
        this.writer = writer;
        this.queue = queue;
        this.instanceId = instanceId;
    }

    /**
     * Default constructor does nothing
     */
    public SimpleDBAppender() {
    }

    /**
     * Obtain a SimpleDB instance from the Factory and get the logging domain
     * 
     * @see ch.qos.logback.core.AppenderBase#start()
     */
    @Override
    public void start() {
        boolean requiredPropsSet = true;
        if (null == accessId) {
            addStatus(new ErrorStatus("Access ID not set", this));
            requiredPropsSet = false;
        }
        if (null == secretKey) {
            addStatus(new ErrorStatus("Secret key not set", this));
            requiredPropsSet = false;
        }
        if (null == domainName) {
            addStatus(new ErrorStatus("Domain name not set", this));
            requiredPropsSet = false;
        }
        if (!requiredPropsSet) return;

        if (sdb == null) {
            sdb = new SimpleDB(accessId, secretKey, true);
        }

        if (dom == null) {
            try {
                dom = sdb.getDomain(domainName);
            } catch (SDBException e) {
                addStatus(new ErrorStatus("Could not get domain for SimpleDBAppender", this, e));
                return;
            }
        }

        if (queue == null) {
            this.queue = new DelayQueue<SimpleDBRow>();
        }

        if (writer == null) {
            this.writer = new SimpleDBWriter(dom);
        }

        if (consumer == null) {
            consumer = new SimpleDBConsumer(queue, writer);
        }

        Thread consumerThread = new Thread(consumer);
        Runnable shutdown = new SimpleDBShutdownHook(queue, writer, consumerThread);
        Runtime.getRuntime().addShutdownHook(new Thread(shutdown));
        consumerThread.setDaemon(true);
        consumerThread.start();
        super.start();
    }

    private void queueForProcessing(String msg, String component, String level, long time) {
        SimpleDBRow row = new SimpleDBRow(msg, instanceId, component, level, time, loggingPeriodMillis);
        queue.add(row);
    }

    @Override
    public void append(LoggingEvent event) {
        String component = componentName;

        // Use component name specified in the configuration, or use the class
        // name
        if (component == null) {
            // TODO: why is CallerData an array? Should we be calling this at
            // this time?
            List<CallerData> data = Arrays.asList(event.getCallerData());
            if (!data.isEmpty()) {
                component = data.get(0).getClassName();
            }
        }

        queueForProcessing(event.getFormattedMessage(), component, event.getLevel().toString(), event.getTimeStamp());
    }
}