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
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;

import org.joda.time.DateTimeZone;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.status.ErrorStatus;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpledb.AmazonSimpleDB;
import com.amazonaws.services.simpledb.AmazonSimpleDBClient;
import com.amazonaws.services.simpledb.model.CreateDomainRequest;
import com.amazonaws.services.simpledb.model.ListDomainsResult;

import com.google.common.collect.ImmutableMap;

/**
 * Logback {@link Appender} to write log data to SimpleDB
 * <p>
 * To configure, add an appender element referring to this class in your {@code
 * logback.xml} file. The following properties must be set:
 * <ul>
 * <li>DomainName: The name of the SimpleDB domain where the logs events are to
 * be written
 * <li>AccessId: Your AWS access ID
 * <li>SecretKey: Your AWS secret key
 * </ul>
 * 
 * @author Gabe Nell
 */
public class SimpleDBAppender extends AppenderBase<LoggingEvent> {

    private AmazonSimpleDB sdb = null;
    private String dom = null;
    private SimpleDBConsumer consumer = null;
    private SimpleDBWriter writer = null;
    private BlockingQueue<SimpleDBRow> queue = null;

    // optional properties
    private String contextName = null;
    private String host = null;
    private long loggingPeriodMillis = 10000;
    private String timeZone = null;

    // required properties
    private String domainName;
    private String accessId;
    private String secretKey;

    /**
     * Set properties common to the system from a Java {@link Properties} file
     * containing any of the following keys:
     * <ul>
     * <li>domainName
     * <li>host
     * <li>accessId
     * <li>secretKey
     * <li>timeZone
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
        String dom = props.getProperty("domainName");
        String host = props.getProperty("host");
        String accessId = props.getProperty("accessId");
        String secretKey = props.getProperty("secretKey");
        String timeZone = props.getProperty("timeZone");

        if (null != dom) setDomainName(dom);
        if (null != host) setHost(host);
        if (null != accessId) setAccessId(accessId);
        if (null != secretKey) setSecretKey(secretKey);
        if (null != timeZone) setTimeZone(timeZone);
    }

    /**
     * Sets the name of the context.
     * 
     * @param contextName
     */
    public void setContextName(String contextName) {
        this.contextName = contextName;
    }

    /**
     * Set the name of the host. This could be any string, such as an IP
     * address, DNS name, friendly name, cloud instance-id, etc. The purpose is
     * to uniquely identify a machine. If not specified, the host column is not
     * written.
     * 
     * @param host
     *        the host name to set
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Sets the SimpleDB domain to use
     * 
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
     * Set the time zone to use when writing the time column to SimpleDB. The
     * time zone should be specified in the long format. See
     * {@link DateTimeZone#forID(String)} for the expected format. The time will
     * be written in ISO 8601 format. If not set, the system time zone will be
     * used.
     * 
     * @param timeZone
     *        the time zone to set
     */
    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
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
    public SimpleDBAppender(AmazonSimpleDB sdb, String dom, SimpleDBConsumer consumer, SimpleDBWriter writer,
            BlockingQueue<SimpleDBRow> queue, String instanceId) {
        this.sdb = sdb;
        this.dom = dom;
        this.consumer = consumer;
        this.writer = writer;
        this.queue = queue;
        this.host = instanceId;
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
            try {
                final AWSCredentials credentials =
                    new BasicAWSCredentials(accessId, secretKey);
                sdb = new AmazonSimpleDBClient(credentials);

                // See if the domain exists
                boolean found = false;
                ListDomainsResult result = sdb.listDomains();
                for (String domainName : result.getDomainNames()) {
                    if (dom.equals(domainName)) {
                        found = true;
                        break;
                    }
                }
                // Didn't find it, so create it
                if (!found) {
                    sdb.createDomain(new CreateDomainRequest(dom));
                }
            } catch (AmazonClientException e) {
                addStatus(new ErrorStatus("Could not get access SimpleDB", this, e));
                return;
            }
        }

        if (queue == null) {
            this.queue = new DelayQueue<SimpleDBRow>();
        }

        if (writer == null) {
            this.writer = new SimpleDBWriter(sdb, dom);
        }

        if (timeZone != null) {
            writer.setTimeZone(DateTimeZone.forID(timeZone));
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

    private void queueForProcessing(String msg, String context, String logger, String level, long time, Map<String, String> mdcPropertyMap) {
        SimpleDBRow row = new SimpleDBRow(msg, host, context, logger, level, time, loggingPeriodMillis, mdcPropertyMap);
        queue.add(row);
    }

    @Override
    public void append(LoggingEvent event) {
        Map<String, String> mdcPropertyMap = ImmutableMap.copyOf(event.getMDCPropertyMap());
        queueForProcessing(event.getFormattedMessage(), contextName, event.getLoggerName(), event.getLevel().toString(), event.getTimeStamp(), mdcPropertyMap);
    }
}