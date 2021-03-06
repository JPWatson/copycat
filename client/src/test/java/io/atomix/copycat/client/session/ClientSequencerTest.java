/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package io.atomix.copycat.client.session;

import io.atomix.copycat.protocol.CommandResponse;
import io.atomix.copycat.protocol.PublishRequest;
import io.atomix.copycat.protocol.QueryResponse;
import io.atomix.copycat.protocol.Response;
import org.testng.annotations.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.*;

/**
 * Client sequencer test.
 *
 * @author <a href="http://github.com/kuujo>Jordan Halterman</a>
 */
@Test
public class ClientSequencerTest {

  /**
   * Tests sequencing an event that arrives before a command response.
   */
  public void testSequenceEventBeforeCommand() throws Throwable {
    ClientSequencer sequencer = new ClientSequencer(new ClientSessionState(UUID.randomUUID()));
    long sequence = sequencer.nextRequest();

    PublishRequest request = PublishRequest.builder()
      .withSession(1)
      .withEventIndex(1)
      .withPreviousIndex(0)
      .build();

    CommandResponse response = CommandResponse.builder()
      .withStatus(Response.Status.OK)
      .withIndex(2)
      .withEventIndex(1)
      .build();

    AtomicInteger run = new AtomicInteger();
    sequencer.sequenceEvent(request, () -> assertEquals(run.getAndIncrement(), 0));
    sequencer.sequenceResponse(sequence, response, () -> assertEquals(run.getAndIncrement(), 1));
    assertEquals(run.get(), 2);
  }

  /**
   * Tests sequencing an event that arrives before a command response.
   */
  public void testSequenceEventAfterCommand() throws Throwable {
    ClientSequencer sequencer = new ClientSequencer(new ClientSessionState(UUID.randomUUID()));
    long sequence = sequencer.nextRequest();

    PublishRequest request = PublishRequest.builder()
      .withSession(1)
      .withEventIndex(1)
      .withPreviousIndex(0)
      .build();

    CommandResponse response = CommandResponse.builder()
      .withStatus(Response.Status.OK)
      .withIndex(2)
      .withEventIndex(1)
      .build();

    AtomicInteger run = new AtomicInteger();
    sequencer.sequenceResponse(sequence, response, () -> assertEquals(run.getAndIncrement(), 1));
    sequencer.sequenceEvent(request, () -> assertEquals(run.getAndIncrement(), 0));
    assertEquals(run.get(), 2);
  }

  /**
   * Tests sequencing an event that arrives before a command response.
   */
  public void testSequenceEventAtCommand() throws Throwable {
    ClientSequencer sequencer = new ClientSequencer(new ClientSessionState(UUID.randomUUID()));
    long sequence = sequencer.nextRequest();

    PublishRequest request = PublishRequest.builder()
      .withSession(1)
      .withEventIndex(2)
      .withPreviousIndex(0)
      .build();

    CommandResponse response = CommandResponse.builder()
      .withStatus(Response.Status.OK)
      .withIndex(2)
      .withEventIndex(2)
      .build();

    AtomicInteger run = new AtomicInteger();
    sequencer.sequenceResponse(sequence, response, () -> assertEquals(run.getAndIncrement(), 1));
    sequencer.sequenceEvent(request, () -> assertEquals(run.getAndIncrement(), 0));
    assertEquals(run.get(), 2);
  }

  /**
   * Tests sequencing an event that arrives before a command response.
   */
  public void testSequenceEventAfterAllCommands() throws Throwable {
    ClientSequencer sequencer = new ClientSequencer(new ClientSessionState(UUID.randomUUID()));
    long sequence = sequencer.nextRequest();

    PublishRequest request1 = PublishRequest.builder()
      .withSession(1)
      .withEventIndex(2)
      .withPreviousIndex(0)
      .build();

    PublishRequest request2 = PublishRequest.builder()
      .withSession(1)
      .withEventIndex(3)
      .withPreviousIndex(2)
      .build();

    CommandResponse response = CommandResponse.builder()
      .withStatus(Response.Status.OK)
      .withIndex(2)
      .withEventIndex(2)
      .build();

    AtomicInteger run = new AtomicInteger();
    sequencer.sequenceEvent(request1, () -> assertEquals(run.getAndIncrement(), 0));
    sequencer.sequenceEvent(request2, () -> assertEquals(run.getAndIncrement(), 2));
    sequencer.sequenceResponse(sequence, response, () -> assertEquals(run.getAndIncrement(), 1));
    assertEquals(run.get(), 3);
  }

  /**
   * Tests sequencing an event that arrives before a command response.
   */
  public void testSequenceEventAbsentCommand() throws Throwable {
    ClientSequencer sequencer = new ClientSequencer(new ClientSessionState(UUID.randomUUID()));

    PublishRequest request1 = PublishRequest.builder()
      .withSession(1)
      .withEventIndex(2)
      .withPreviousIndex(0)
      .build();

    PublishRequest request2 = PublishRequest.builder()
      .withSession(1)
      .withEventIndex(3)
      .withPreviousIndex(2)
      .build();

    AtomicInteger run = new AtomicInteger();
    sequencer.sequenceEvent(request1, () -> assertEquals(run.getAndIncrement(), 0));
    sequencer.sequenceEvent(request2, () -> assertEquals(run.getAndIncrement(), 1));
    assertEquals(run.get(), 2);
  }

  /**
   * Tests sequencing callbacks with the sequencer.
   */
  public void testSequenceResponses() throws Throwable {
    ClientSequencer sequencer = new ClientSequencer(new ClientSessionState(UUID.randomUUID()));
    long sequence1 = sequencer.nextRequest();
    long sequence2 = sequencer.nextRequest();
    assertTrue(sequence2 == sequence1 + 1);

    CommandResponse commandResponse = CommandResponse.builder()
      .withStatus(Response.Status.OK)
      .withIndex(2)
      .withEventIndex(0)
      .build();

    QueryResponse queryResponse = QueryResponse.builder()
      .withStatus(Response.Status.OK)
      .withIndex(2)
      .withEventIndex(0)
      .build();

    AtomicBoolean run = new AtomicBoolean();
    sequencer.sequenceResponse(sequence2, queryResponse, () -> run.set(true));
    sequencer.sequenceResponse(sequence1, commandResponse, () -> assertFalse(run.get()));
    assertTrue(run.get());
  }

}
