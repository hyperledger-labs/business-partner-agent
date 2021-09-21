/*
 * Copyright (c) 2020-2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hyperledger.bpa.impl.aries;

import com.google.gson.Gson;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.aries.config.GsonConfig;
import org.hyperledger.bpa.BaseTest;
import org.hyperledger.bpa.model.Partner;
import org.hyperledger.bpa.repository.PartnerRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
class ConnectionManagerTest extends BaseTest {

    private final Gson gson = GsonConfig.defaultConfig();

    @Inject
    AriesEventHandler eventHandler;

    @Inject
    PartnerRepository repo;

    @Test
    void testReceiveOutgoingConnectionEvents() {
        repo.save(Partner
                .builder()
                .ariesSupport(Boolean.TRUE)
                .connectionId("de0d51e8-4c7f-4dc9-8b7b-a8f57182d8a5")
                .alias("Alice")
                .state(ConnectionState.INIT)
                .did("dummy")
                .build());
        final ConnectionRecord invite = gson.fromJson(connInvitationInvite, ConnectionRecord.class);
        eventHandler.handleConnection(invite);

        Optional<Partner> p = repo.findByConnectionId(invite.getConnectionId());
        assertTrue(p.isPresent());
        assertEquals(ConnectionState.REQUEST, p.get().getState());
        assertEquals("Alice", p.get().getAlias());
        assertNotNull(p.get().getConnectionId());

        final ConnectionRecord active = gson.fromJson(connInvitationActive, ConnectionRecord.class);
        eventHandler.handleConnection(active);

        p = repo.findByConnectionId(active.getConnectionId());
        assertTrue(p.isPresent());
        assertEquals(ConnectionState.COMPLETED, p.get().getState());
    }

    @Test
    void testReceiveIncomingConnectionEvents() {
        final ConnectionRecord invite = gson.fromJson(receiveRequest, ConnectionRecord.class);
        eventHandler.handleConnection(invite);

        Optional<Partner> p = repo.findByConnectionId(invite.getConnectionId());
        assertTrue(p.isPresent());
        assertEquals(ConnectionState.REQUEST, p.get().getState());
        assertEquals("bob", p.get().getLabel());

        final ConnectionRecord active = gson.fromJson(receiveActive, ConnectionRecord.class);
        eventHandler.handleConnection(active);

        p = repo.findByConnectionId(active.getConnectionId());
        assertTrue(p.isPresent());
        assertEquals(ConnectionState.COMPLETED, p.get().getState());
    }

    @Test
    void testCreateInvitation() {
        final ConnectionRecord invite = gson.fromJson(inviteReceive, ConnectionRecord.class);
        eventHandler.handleConnection(invite);

        // not handled here
        Optional<Partner> p = repo.findByConnectionId(invite.getConnectionId());
        assertFalse(p.isPresent());

        final ConnectionRecord response = gson.fromJson(inviteResponse, ConnectionRecord.class);
        eventHandler.handleConnection(response);

        p = repo.findByConnectionId(response.getConnectionId());
        assertTrue(p.isPresent());
        assertEquals(ConnectionState.RESPONSE, p.get().getState());
        assertTrue(p.get().getDid().endsWith("QjqxU2wnrBGwLJnW585QWp"));
        assertEquals("Wallet", p.get().getLabel());
    }

    @Test
    void testCreateOOBInvitation() {
        final ConnectionRecord invite = gson.fromJson(oobInvitationReceive, ConnectionRecord.class);
        eventHandler.handleConnection(invite);

        // not handled here
        Optional<Partner> p = repo.findByInvitationMsgId(invite.getInvitationMsgId());
        assertFalse(p.isPresent());

        repo.save(Partner.builder()
                .invitationMsgId(invite.getInvitationMsgId())
                .ariesSupport(Boolean.TRUE)
                .did(invite.getTheirDid())
                .build());

        final ConnectionRecord response = gson.fromJson(oobInvitationCompleted, ConnectionRecord.class);
        eventHandler.handleConnection(response);

        p = repo.findByInvitationMsgId(response.getInvitationMsgId());
        assertTrue(p.isPresent());
        assertEquals(ConnectionState.COMPLETED, p.get().getState());
        assertTrue(p.get().getDid().endsWith("XYQQ6f1VDkDEDzWLRKzMc2"));
        assertEquals("bob", p.get().getLabel());
    }

    private final String connInvitationInvite = """
            {
                "invitation_mode": "once",
                "their_role": "inviter",
                "connection_protocol": "didexchange/1.0",
                "rfc23_state": "request-sent",
                "accept": "manual",
                "routing_state": "none",
                "alias": "a950b832-59ac-480c-8135-e76ba76f03ba",
                "updated_at": "2021-07-05 13:31:09.179622Z",
                "their_did": "did:sov:EraYCDJUPsChbkw7S1vV96",
                "their_public_did": "did:sov:EraYCDJUPsChbkw7S1vV96",
                "state": "request",
                "my_did": "F6dB7dMVHUQSC64qemnBi7",
                "request_id": "e7b668eb-2a26-4dc0-84e5-73b0f2c0fe05",
                "connection_id": "de0d51e8-4c7f-4dc9-8b7b-a8f57182d8a5",
                "created_at": "2021-07-05 13:31:09.179622Z"
            }""";

    private final String connInvitationActive = """
            {
                "invitation_mode": "once",
                "their_role": "inviter",
                "connection_protocol": "didexchange/1.0",
                "rfc23_state": "completed",
                "accept": "manual",
                "routing_state": "none",
                "alias": "a950b832-59ac-480c-8135-e76ba76f03ba",
                "updated_at": "2021-07-05 13:32:29.689513Z",
                "their_did": "8hXCW94BRYSm2PQeFHFcV1",
                "their_public_did": "did:sov:EraYCDJUPsChbkw7S1vV96",
                "state": "completed",
                "my_did": "F6dB7dMVHUQSC64qemnBi7",
                "request_id": "e7b668eb-2a26-4dc0-84e5-73b0f2c0fe05",
                "connection_id": "de0d51e8-4c7f-4dc9-8b7b-a8f57182d8a5",
                "created_at": "2021-07-05 13:31:09.179622Z"
            }""";

    private final String receiveRequest = """
            {
                "their_role": "invitee",
                "their_label": "bob",
                "request_id": "1c6e7e67-3b34-4fa5-83ff-34b1af853499",
                "my_did": "MZMurMqVFiS24oxYBPZ3C7",
                "invitation_key": "8gdhRLtvJHzKoJGyuEqgdN1QZGYfai4wMHFGgtfDXg3D",
                "state": "request",
                "accept": "auto",
                "invitation_mode": "once",
                "routing_state": "none",
                "created_at": "2021-07-05 14:31:59.321382Z",
                "connection_protocol": "didexchange/1.0",
                "rfc23_state": "request-received",
                "their_did": "EraYCDJUPsChbkw7S1vV96",
                "connection_id": "b8d4f176-7967-4af7-9686-60a59b35f122",
                "updated_at": "2021-07-05 14:31:59.321382Z"
            }""";

    private final String receiveActive = """
            {
                "their_role": "invitee",
                "their_label": "bob",
                "request_id": "1c6e7e67-3b34-4fa5-83ff-34b1af853499",
                "my_did": "MZMurMqVFiS24oxYBPZ3C7",
                "invitation_key": "8gdhRLtvJHzKoJGyuEqgdN1QZGYfai4wMHFGgtfDXg3D",
                "state": "completed",
                "accept": "auto",
                "invitation_mode": "once",
                "routing_state": "none",
                "created_at": "2021-07-05 14:31:59.321382Z",
                "connection_protocol": "didexchange/1.0",
                "rfc23_state": "completed",
                "their_did": "EraYCDJUPsChbkw7S1vV96",
                "connection_id": "b8d4f176-7967-4af7-9686-60a59b35f122",
                "updated_at": "2021-07-05 14:31:59.735196Z"
            }""";

    private final String inviteReceive = """
            {
                "accept": "auto",
                "connection_id": "5d41c1cb-2856-4026-984e-24d2976a05ba",
                "connection_protocol": "connections/1.0",
                "updated_at": "2021-04-28 08:20:17.034908Z",
                "alias": "Invitation 1",
                "routing_state": "none",
                "invitation_key": "J9CHkDjr3oG7nq3enrojCRvsY9Cxq1Z7W1Y56GHNZe29",
                "created_at": "2021-04-28 08:20:17.034908Z",
                "their_role": "invitee",
                "state": "invitation",
                "invitation_mode": "once",
                "rfc23_state": "invitation-sent"
            }""";

    private final String inviteResponse = """
            {
                "accept": "auto",
                "connection_id": "5d41c1cb-2856-4026-984e-24d2976a05ba",
                "updated_at": "2021-04-28 08:20:43.237710Z",
                "their_label": "Wallet",
                "alias": "Invitation 1",
                "their_did": "QjqxU2wnrBGwLJnW585QWp",
                "routing_state": "none",
                "invitation_key": "J9CHkDjr3oG7nq3enrojCRvsY9Cxq1Z7W1Y56GHNZe29",
                "created_at": "2021-04-28 08:20:17.034908Z",
                "their_role": "invitee",
                "state": "response",
                "my_did": "37FY6gGZWATtKv8ywwJjdi",
                "invitation_mode": "once",
                "rfc23_state": "response-sent"
            }""";

    private final String oobInvitationReceive = """
            {
                "alias": "sxxx",
                "rfc23_state": "request-received",
                "invitation_key": "8gdhRLtvJHzKoJGyuEqgdN1QZGYfai4wMHFGgtfDXg3D",
                "connection_protocol": "didexchange/1.0",
                "invitation_mode": "once",
                "updated_at": "2021-07-19 12:16:04.851837Z",
                "their_did": "XYQQ6f1VDkDEDzWLRKzMc2",
                "accept": "auto",
                "connection_id": "3f4da8f3-04e9-49dd-9dd8-fc09183efc83",
                "their_role": "invitee",
                "state": "request",
                "routing_state": "none",
                "invitation_msg_id": "2d1f1bd7-5f87-48d6-929e-104297c24173",
                "their_label": "bob",
                "created_at": "2021-07-19 12:15:48.001375Z",
                "request_id": "ab682132-8c45-48aa-ae32-babaae435f6d"
            }""";

    private final String oobInvitationCompleted = """
            {
                "alias": "sxxx",
                "rfc23_state": "completed",
                "invitation_key": "8gdhRLtvJHzKoJGyuEqgdN1QZGYfai4wMHFGgtfDXg3D",
                "connection_protocol": "didexchange/1.0",
                "invitation_mode": "once",
                "updated_at": "2021-07-19 12:16:05.291960Z",
                "their_did": "XYQQ6f1VDkDEDzWLRKzMc2",
                "accept": "auto",
                "connection_id": "3f4da8f3-04e9-49dd-9dd8-fc09183efc83",
                "my_did": "SUaF5tYZGHB4cu4iBJ1Aav",
                "their_role": "invitee",
                "state": "completed",
                "routing_state": "none",
                "invitation_msg_id": "2d1f1bd7-5f87-48d6-929e-104297c24173",
                "their_label": "bob",
                "created_at": "2021-07-19 12:15:48.001375Z",
                "request_id": "ab682132-8c45-48aa-ae32-babaae435f6d"
            }""";
}
