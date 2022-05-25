/*
 * Copyright 2022 Netflix, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.netflix.conductor.contribs.queue.kafka;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.mockito.stubbing.Answer;

import com.netflix.conductor.core.config.Configuration;
import com.netflix.conductor.core.events.queue.Message;
import com.netflix.conductor.core.execution.ApplicationException;

import com.google.common.util.concurrent.Uninterruptibles;
import rx.Observable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestKafkaObservableQueue {
    @Test
    public void testSubscribe() {

        List<Message> messages = new LinkedList<>();
        Observable.range(0, 10)
                .forEach(
                        (Integer x) ->
                                messages.add(
                                        new Message("testTopic:01:" + x, "payload: " + x, null)));
        assertEquals(10, messages.size());

        KafkaObservableQueue queue = mock(KafkaObservableQueue.class);

        Answer<?> answer = (Answer<List<Message>>) invocation -> Collections.emptyList();
        when(queue.receiveMessages()).thenReturn(messages).thenAnswer(answer);
        when(queue.getOnSubscribe()).thenCallRealMethod();
        when(queue.observe()).thenCallRealMethod();
        List<Message> found = new LinkedList<>();
        Observable<Message> observable = queue.observe();
        assertNotNull(observable);
        observable.subscribe(found::add);

        Uninterruptibles.sleepUninterruptibly(1000, TimeUnit.MILLISECONDS);

        assertEquals(messages.size(), found.size());
        assertEquals(messages, found);
    }

    @Test(expected = Test.None.class /* no exception expected */)
    public void testPublish() {

        List<Message> messages = new LinkedList<>();
        Observable.range(0, 10)
                .forEach(
                        (Integer x) ->
                                messages.add(
                                        new Message("testTopic:01:" + x, "payload: " + x, null)));
        assertEquals(10, messages.size());

        KafkaObservableQueue queue = mock(KafkaObservableQueue.class);
        Answer<?> answer =
                (invocation) -> {
                    return null;
                };
        doAnswer(answer).when(queue).publishMessages(messages);
        doCallRealMethod().when(queue).publish(messages);
        queue.publish(messages);
    }

    @Test(expected = ApplicationException.class)
    public void testPublishWithException() {

        List<Message> messages = new LinkedList<>();
        Observable.range(0, 10)
                .forEach(
                        (Integer x) ->
                                messages.add(
                                        new Message("testTopic:01:" + x, "payload: " + x, null)));
        assertEquals(10, messages.size());
        KafkaObservableQueue queue = mock(KafkaObservableQueue.class);
        doThrow(ApplicationException.class).when(queue).publishMessages(messages);
        doCallRealMethod().when(queue).publish(messages);
        queue.publish(messages);
    }

    @Test(expected = RuntimeException.class)
    public void testInitializationWithoutConfig() {
        Configuration config = mock(Configuration.class);
        @SuppressWarnings("unused")
        KafkaObservableQueue queue = new KafkaObservableQueue("kafka:testTopi02", config);
    }

    @Test(expected = RuntimeException.class)
    public void testInitializationWithoutConsumerConfig() {
        Configuration config = mock(Configuration.class);
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("kafka.bootstrap.servers", "locahost:9999");
        configMap.put(
                "kafka.producer.key.serializer",
                "org.apache.kafka.common.serialization.StringSerializer.class");
        configMap.put(
                "kafka.producer.value.serializer",
                "org.apache.kafka.common.serialization.StringSerializer.class");
        when(config.getAll()).thenReturn(configMap);
        @SuppressWarnings("unused")
        KafkaObservableQueue queue = new KafkaObservableQueue("kafka:testTopi02", config);
    }

    @Test(expected = RuntimeException.class)
    public void testInitializationWithoutProducerConfig() {
        Configuration config = mock(Configuration.class);
        Map<String, Object> configMap = new HashMap<>();
        configMap.put("kafka.bootstrap.servers", "locahost:9999");
        configMap.put(
                "kafka.consumer.key.deserializer",
                "org.apache.kafka.common.serialization.StringDeSerializer.class");
        configMap.put(
                "kafka.consumer.value.deserializer",
                "org.apache.kafka.common.serialization.StringDeSerializer.class");
        when(config.getAll()).thenReturn(configMap);
        @SuppressWarnings("unused")
        KafkaObservableQueue queue = new KafkaObservableQueue("kafka:testTopi02", config);
    }
}
