/*
 * Copyright 2013 Mozilla Foundation
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mozilla.bagheera.producer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import java.util.UUID;

import kafka.api.FetchRequest;
import kafka.api.FetchRequestBuilder;
import kafka.javaapi.FetchResponse;
import kafka.javaapi.consumer.SimpleConsumer;
import kafka.javaapi.message.ByteBufferMessageSet;
import kafka.producer.KeyedMessage;
import kafka.message.Message;
import kafka.message.MessageAndOffset;
import kafka.producer.ProducerConfig;
import kafka.server.KafkaConfig;
import kafka.server.KafkaServer;
import kafka.utils.SystemTime$;
import kafka.utils.Time;
import kafka.common.OffsetOutOfRangeException;
import org.apache.curator.test.TestingServer;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.mozilla.bagheera.BagheeraProto.BagheeraMessage;


public class ProducerTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private String KAFKA_DIR;
    private static final int BATCH_SIZE = 4;
    private static final int GOOD_MESSAGE_SIZE = 100; 
    private static final int MAX_MESSAGE_SIZE = BATCH_SIZE * GOOD_MESSAGE_SIZE;
    private static final int BAD_MESSAGE_SIZE = 1000;
    private static final int KAFKA_BROKER_ID = 0;
    private static final String KAFKA_BROKER_HOST = "localhost";
    private static final int KAFKA_BROKER_PORT = 9090;
    private static final String KAFKA_TOPIC = "test";
    private static final int KAFKA_PARTITION = 0;

    private int messageNumber = 0;

    private KafkaServer server;
    private TestingServer zkServer;

    @Before
    public void setup() throws IOException, InterruptedException {
        // Use an automatically-created folder for the kafka server
        KAFKA_DIR = folder.newFolder("kafka").getCanonicalPath();
        System.out.println("Using kafka temp dir: " + KAFKA_DIR);

        startServer();
    }

    private void startServer() throws IOException {
        stopServer();

        try {
          // setup local ZK
          zkServer = new TestingServer();
        } catch (Exception e) {
          throw new IOException(e);
        }

        Properties props = new Properties();
        props.setProperty("hostname", KAFKA_BROKER_HOST);
        props.setProperty("port", String.valueOf(KAFKA_BROKER_PORT));
        props.setProperty("broker.id", String.valueOf(KAFKA_BROKER_ID));
        props.setProperty("log.dir", KAFKA_DIR);
        props.setProperty("zookeeper.connect", zkServer.getConnectString());
        // flush every message.
        props.setProperty("log.flush.interval.messages", "1");

        // flush every 1ms
        props.setProperty("log.flush.scheduler.interval.ms", "1");
        props.setProperty("log.flush.interval.ms", "1");
        props.setProperty("message.max.bytes", String.valueOf(MAX_MESSAGE_SIZE));

        Time time = SystemTime$.MODULE$;
        server = new KafkaServer(new KafkaConfig(props), time);
        server.startup();
    }

    private void stopServer() {
        if (server != null) {
            server.shutdown();
            server.awaitShutdown();
            server = null;
        }
    }

    @After
    public void shutdown() {
        System.out.println("After tests, kafka dir still exists? " + new File(KAFKA_DIR).exists());
        stopServer();
    }

    @Test
    public void testAsyncBatch() throws IOException, InterruptedException {
        produceData(false);
        int messageCount = countMessages();
        System.out.println("Consumed " + messageCount + " messages");

        // We expect the batch size plus two extra messages:
        int goodExpectedCount = BATCH_SIZE + 2;
        assertEquals(goodExpectedCount, messageCount);

        produceData(true);
        messageCount = countMessages();

        // If the entire batch got wrecked, we should end up with 3 messages left over.
        // Since we re-consume the entire queue, we have to discount the messages we produced
        // above.  With batch size set to 10, we expect to see the whole first batch (1-12)
        // plus the 3 messages after the aborted batch (23, 24, 25).  Messages in the batch
        // of 13-22 are expected to be lost.
        // You should see this output:
//        Message 1 @177: 1.23456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789
//        Message 2 @354: 2.23456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789
//        Message 3 @531: 3.23456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789
//        Message 4 @708: 4.23456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789
//        Message 5 @885: 5.23456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789
//        Message 6 @1062: 6.23456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789
//        Message 7 @1239: 7.23456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789
//        Message 8 @1416: 8.23456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789
//        Message 9 @1593: 9.23456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789
//        Message 10 @1770: 10.3456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789
//        Message 11 @1947: 11.3456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789
//        Message 12 @2124: 12.3456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789
//        Message 13 @2301: 23.3456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789
//        Message 14 @2478: 24.3456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789
//        Message 15 @2655: 25.3456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789
        int badExpectedCount = goodExpectedCount + 3;
        System.err.println("cnts " + badExpectedCount + " " + messageCount);
        assertEquals(badExpectedCount, messageCount);
    }

    private int countMessages() throws InvalidProtocolBufferException {
        SimpleConsumer consumer = new SimpleConsumer(KAFKA_BROKER_HOST, KAFKA_BROKER_PORT, 1000, 1024, "test_consumer");
        long offset = 0l;
        int messageCount = 0, msgsRead = 0;
        FetchRequest req;
        FetchResponse response;

        do {
            req = new FetchRequestBuilder().clientId("test_consumer").addFetch(KAFKA_TOPIC, KAFKA_PARTITION, offset, 1024).build();
            response = consumer.fetch(req);

            msgsRead = 0;
            for (MessageAndOffset msgAndOff : response.messageSet(KAFKA_TOPIC, KAFKA_PARTITION)) {
                messageCount++;
                msgsRead++;
                offset = msgAndOff.offset();

                Message message2 = msgAndOff.message();
                BagheeraMessage bmsg = BagheeraMessage.parseFrom(ByteString.copyFrom(message2.payload()));
                
                String payload = new String(bmsg.getPayload().toByteArray());
                System.out.println(String.format("Message %d @%d: %s", messageCount, offset, payload));
                offset = msgAndOff.nextOffset();
            }
        } while (msgsRead > 0);
        
        consumer.close();
        return messageCount;
    }

    private void produceData(boolean includeBadRecord) throws InterruptedException {
        Properties props = getProperties();
        kafka.javaapi.producer.Producer<String,BagheeraMessage> producer = new kafka.javaapi.producer.Producer<String,BagheeraMessage>(new ProducerConfig(props));
        BagheeraMessage msg = getMessage(GOOD_MESSAGE_SIZE);

        assertEquals(GOOD_MESSAGE_SIZE, msg.getPayload().size());
        producer.send(getProducerData(msg));
        producer.send(getProducerData(getMessage(GOOD_MESSAGE_SIZE)));

        if (includeBadRecord) {
            producer.send(getProducerData(getMessage(BAD_MESSAGE_SIZE)));
        }

        for (int i = 0; i < BATCH_SIZE; i++) {
            producer.send(getProducerData(getMessage(GOOD_MESSAGE_SIZE)));
        }
        producer.close();

        // Wait for flush
        Thread.sleep(100);
    }

    private KeyedMessage<String,BagheeraMessage> getProducerData(BagheeraMessage msg) {
        return new KeyedMessage<String,BagheeraMessage>(msg.getNamespace(), msg);
    }

    private BagheeraMessage getMessage(int payloadSize) {
        BagheeraMessage.Builder bmsgBuilder = BagheeraMessage.newBuilder();
        bmsgBuilder.setNamespace(KAFKA_TOPIC);
        bmsgBuilder.setId(UUID.randomUUID().toString());
        bmsgBuilder.setIpAddr(ByteString.copyFrom("192.168.1.10".getBytes()));

        StringBuilder content = new StringBuilder(payloadSize);
        content.append(++messageNumber);
        content.append(".");
        for (int i = content.length(); i < payloadSize; i++) {
            content.append(i % 10);
        }
        bmsgBuilder.addPartition(String.valueOf(messageNumber));
        bmsgBuilder.setPayload(ByteString.copyFrom(content.toString().getBytes()));
        bmsgBuilder.setTimestamp(System.currentTimeMillis());
        return bmsgBuilder.build();
    }

    private Properties getProperties() {
        Properties props = new Properties();
        props.setProperty("producer.type","async");
        props.setProperty("request.required.acks","1");
        props.setProperty("batch.num.messages", String.valueOf(BATCH_SIZE));
        props.setProperty("metadata.broker.list", KAFKA_BROKER_HOST + ":" + KAFKA_BROKER_PORT);
        props.setProperty("serializer.class", "com.mozilla.bagheera.serializer.BagheeraEncoder");

        return props;
    }
}
