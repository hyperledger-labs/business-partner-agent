/**
 * Copyright (c) 2020 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/organizational-agent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hyperledger.oa.impl.aries;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.connection.ConnectionFilter;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.aries.api.message.PingEvent;
import org.hyperledger.aries.api.message.PingRequest;
import org.hyperledger.aries.api.message.PingResponse;
import org.hyperledger.oa.model.Partner;
import org.hyperledger.oa.repository.PartnerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PingManagerTest {

    @Mock
    private AriesClient aries;
    @Mock
    private PartnerRepository repo;

    @InjectMocks
    private PingManager ping;

    @Test
    void testHappyFlow() throws Exception {
        when(aries.connectionIds(any(ConnectionFilter.class))).thenReturn(List.of("1", "2"));
        when(aries.connectionsSendPing(anyString(), any(PingRequest.class)))
                .thenReturn(Optional.of(new PingResponse("a")))
                .thenReturn(Optional.of(new PingResponse("b")))
                .thenReturn(Optional.of(new PingResponse("a")))
                .thenReturn(Optional.of(new PingResponse("b")))
                .thenReturn(Optional.of(new PingResponse("a")))
                .thenReturn(Optional.of(new PingResponse("b")));

        ping.handlePingEvent(PingEvent.of("a", "response_received"));
        assertEquals(1, ping.getReceivedSize());

        ping.checkConnections();

        verify(repo, never()).updateStateByConnectionId(anyString(), anyString());

        assertEquals(2, ping.getSentSize());
        assertEquals(0, ping.getReceivedSize());

        ping.checkConnections();

        verify(repo, times(1)).updateStateByConnectionId("1", ConnectionState.inactive.toString());
        verify(repo, times(1)).updateStateByConnectionId("2", ConnectionState.inactive.toString());

        assertEquals(2, ping.getSentSize());
        assertEquals(0, ping.getReceivedSize());

        ping.handlePingEvent(PingEvent.of("a", "response_received"));
        ping.handlePingEvent(PingEvent.of("b", "response_received"));
        ping.handlePingEvent(PingEvent.of("comment", "sent"));

        assertEquals(2, ping.getReceivedSize());

        ping.checkConnections();

        verify(repo, times(1)).updateStateByConnectionId("1", ConnectionState.active.toString());
        verify(repo, times(1)).updateStateByConnectionId("2", ConnectionState.active.toString());

        assertEquals(2, ping.getSentSize());
    }

    @Test
    void testInitialState() throws Exception {
        when(aries.connectionIds(any(ConnectionFilter.class))).thenReturn(List.of("1", "2"));

        ping.checkConnections();
        verify(repo, never()).updateStateByConnectionId(anyString(), anyString());

        assertEquals(0, ping.getSentSize());
    }

    @Test
    void testRemoveStale() throws Exception {
        when(aries.connectionIds()).thenReturn(List.of("1", "2", "3"));
        when(repo.findAll()).thenReturn(List.of(Partner.builder().connectionId("1").build()));

        ping.deleteStaleConnections();

        verify(aries, times(1)).connectionsRemove("2");
        verify(aries, times(1)).connectionsRemove("3");
    }

    @Test
    void testRemoveStaleOnlyBpa() throws Exception {
        when(aries.connectionIds()).thenReturn(List.of());
        when(repo.findAll()).thenReturn(List.of(Partner.builder().connectionId("1").build()));

        ping.deleteStaleConnections();

        verify(aries, never()).connectionsRemove(anyString());
    }

    @Test
    void testRemoveStaleBothEmpty() throws Exception {
        when(aries.connectionIds()).thenReturn(List.of());
        when(repo.findAll()).thenReturn(List.of());

        ping.deleteStaleConnections();

        verify(aries, never()).connectionsRemove(anyString());
    }

    @Test
    void testRemoveStaleBothSame() throws Exception {
        when(aries.connectionIds()).thenReturn(List.of("1", "2"));
        when(repo.findAll()).thenReturn(List.of(
                Partner.builder().connectionId("1").build(),
                Partner.builder().connectionId("2").build()));

        ping.deleteStaleConnections();

        verify(aries, never()).connectionsRemove(anyString());
    }

}
