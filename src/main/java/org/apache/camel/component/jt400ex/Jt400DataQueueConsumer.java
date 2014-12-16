/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.jt400ex;

import com.ibm.as400.access.BaseDataQueue;
import com.ibm.as400.access.DataQueue;
import com.ibm.as400.access.DataQueueEntry;
import com.ibm.as400.access.KeyedDataQueue;
import com.ibm.as400.access.KeyedDataQueueEntry;
import org.apache.camel.Exchange;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.component.jt400ex.Jt400DataQueueEndpoint.Format;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.PollingConsumerSupport;

/**
 * {@link org.apache.camel.PollingConsumer} that polls a data queue for data
 *
 * This version of the jt400 consumer has been modified to allow it to recieve from a
 * number of different keys instead of just one. This is useful when you don't know
 * the key name before hand, or need to read from a number of keys that can't be matched
 * using operations like LT, GT, LE, GE, NE etc.
 */
public class Jt400DataQueueConsumer extends PollingConsumerSupport {

    /**
     * We match all keys exactly
     */
    private static final String EQ_SEARCH_TYPE = "EQ";

    private final Jt400DataQueueEndpoint endpoint;
    
    /**
     * Performs the lifecycle logic of this consumer.
     */
    private final Jt400DataQueueService queueService;

    /**
     * Used to get the list of keys this consumer should read from a keyed
     * queue.
     */
    private final SearchKeysProvider searchKeysProvider;

    /**
     * Creates a new consumer instance
     */
    protected Jt400DataQueueConsumer(final Jt400DataQueueEndpoint endpoint) {
        super(endpoint);
        this.endpoint = endpoint;
        this.queueService = new Jt400DataQueueService(endpoint);
        this.searchKeysProvider = (SearchKeysProvider)endpoint.getCamelContext().getRegistry().lookup(endpoint.getSearchKeysProvider());
    }

    @Override
    protected void doStart() throws Exception {
        queueService.start();
    }

    @Override
    protected void doStop() throws Exception {
        queueService.stop();
    }

    public Exchange receive() {
        // -1 to indicate a blocking read from data queue
        return receive(-1);
    }

    public Exchange receiveNoWait() {
        return receive(0);
    }

    /**
     * Receives an entry from a data queue and returns an {@link Exchange} to
     * send this data If the endpoint's format is set to {@link Format#binary},
     * the data queue entry's data will be received/sent as a
     * <code>byte[]</code>. If the endpoint's format is set to
     * {@link Format#text}, the data queue entry's data will be received/sent as
     * a <code>String</code>.
     * <p/>
     * The following message headers may be set by the receiver
     * <ul>
     * <li>SENDER_INFORMATION: The Sender Information from the Data Queue</li>
     * <li>KEY: The message key if the endpoint is configured to connect to a <code>KeyedDataQueue</code></li>
     * </ul>
     *
     * @param timeout time to wait when reading from data queue. A value of -1
     *                indicates a blocking read.
     */
    public Exchange receive(final long timeout) {
        final BaseDataQueue queue = queueService.getDataQueue();
        try {
            if (endpoint.isKeyed()) {
                return receive((KeyedDataQueue) queue, timeout);
            } else {
                return receive((DataQueue) queue, timeout);
            }
        } catch (Exception e) {
            throw new RuntimeCamelException("Unable to read from data queue: " + queue.getName(), e);
        }
    }

    private Exchange receive(final DataQueue queue, final long timeout) throws Exception {
        DataQueueEntry entry = null;
        if (timeout >= 0) {
            int seconds = (int) timeout / 1000;
            log.trace("Reading from data queue: {} with {} seconds timeout", queue.getName(), seconds);
            entry = queue.read(seconds);
        } else {
            log.trace("Reading from data queue: {} with no timeout", queue.getName());
            entry = queue.read(-1);
        }

        final Exchange exchange = new DefaultExchange(endpoint.getCamelContext());
        if (entry != null) {
            exchange.getIn().setHeader(Jt400DataQueueEndpoint.SENDER_INFORMATION, entry.getSenderInformation());
            if (endpoint.getFormat() == Format.binary) {
                exchange.getIn().setBody(entry.getData());
            } else {
                exchange.getIn().setBody(entry.getString());
            }
            return exchange;
        }
        return null;
    }

    private Exchange receive(final KeyedDataQueue queue, final long timeout) throws Exception {

        KeyedDataQueueEntry entry = null;

        for (final String key : searchKeysProvider.getKeys()) {
            log.trace("Reading from data queue: {} with no timeout", queue.getName());
            entry = queue.read(key, 0, EQ_SEARCH_TYPE);

            /*
                We have a message from our list of keys, so return an exchange
             */
            if (entry != null) {
                final Exchange exchange = new DefaultExchange(endpoint.getCamelContext());
                exchange.getIn().setHeader(Jt400DataQueueEndpoint.SENDER_INFORMATION, entry.getSenderInformation());
                if (endpoint.getFormat() == Format.binary) {
                    exchange.getIn().setBody(entry.getData());
                    exchange.getIn().setHeader(Jt400DataQueueEndpoint.KEY, entry.getKey());
                } else {
                    exchange.getIn().setBody(entry.getString());
                    exchange.getIn().setHeader(Jt400DataQueueEndpoint.KEY, entry.getKeyString());
                }

                return exchange;
            }
        }

        /*
            No messages found
         */
        return null;
    }
}
