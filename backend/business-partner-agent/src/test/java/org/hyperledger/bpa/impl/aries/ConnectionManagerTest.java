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
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.aries.config.GsonConfig;
import org.hyperledger.bpa.BaseTest;
import org.hyperledger.bpa.model.Partner;
import org.hyperledger.bpa.repository.PartnerRepository;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
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
        final ConnectionRecord invite = gson.fromJson(createInvite, ConnectionRecord.class);
        eventHandler.handleConnection(invite);

        Optional<Partner> p = repo.findByConnectionId(invite.getConnectionId());
        assertTrue(p.isPresent());
        assertEquals(ConnectionState.REQUEST, p.get().getState());
        assertEquals("Alice", p.get().getAlias());
        assertNotNull(p.get().getConnectionId());

        final ConnectionRecord active = gson.fromJson(createActive, ConnectionRecord.class);
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

    private final String createInvite = "{\n" +
            "    \"invitation_mode\": \"once\",\n" +
            "    \"their_role\": \"inviter\",\n" +
            "    \"connection_protocol\": \"didexchange/1.0\",\n" +
            "    \"rfc23_state\": \"request-sent\",\n" +
            "    \"accept\": \"manual\",\n" +
            "    \"routing_state\": \"none\",\n" +
            "    \"alias\": \"a950b832-59ac-480c-8135-e76ba76f03ba\",\n" +
            "    \"updated_at\": \"2021-07-05 13:31:09.179622Z\",\n" +
            "    \"their_did\": \"did:sov:EraYCDJUPsChbkw7S1vV96\",\n" +
            "    \"their_public_did\": \"did:sov:EraYCDJUPsChbkw7S1vV96\",\n" +
            "    \"state\": \"request\",\n" +
            "    \"my_did\": \"F6dB7dMVHUQSC64qemnBi7\",\n" +
            "    \"request_id\": \"e7b668eb-2a26-4dc0-84e5-73b0f2c0fe05\",\n" +
            "    \"connection_id\": \"de0d51e8-4c7f-4dc9-8b7b-a8f57182d8a5\",\n" +
            "    \"created_at\": \"2021-07-05 13:31:09.179622Z\"\n" +
            "}";

    private final String createActive = "{\n" +
            "    \"invitation_mode\": \"once\",\n" +
            "    \"their_role\": \"inviter\",\n" +
            "    \"connection_protocol\": \"didexchange/1.0\",\n" +
            "    \"rfc23_state\": \"completed\",\n" +
            "    \"accept\": \"manual\",\n" +
            "    \"routing_state\": \"none\",\n" +
            "    \"alias\": \"a950b832-59ac-480c-8135-e76ba76f03ba\",\n" +
            "    \"updated_at\": \"2021-07-05 13:32:29.689513Z\",\n" +
            "    \"their_did\": \"8hXCW94BRYSm2PQeFHFcV1\",\n" +
            "    \"their_public_did\": \"did:sov:EraYCDJUPsChbkw7S1vV96\",\n" +
            "    \"state\": \"completed\",\n" +
            "    \"my_did\": \"F6dB7dMVHUQSC64qemnBi7\",\n" +
            "    \"request_id\": \"e7b668eb-2a26-4dc0-84e5-73b0f2c0fe05\",\n" +
            "    \"connection_id\": \"de0d51e8-4c7f-4dc9-8b7b-a8f57182d8a5\",\n" +
            "    \"created_at\": \"2021-07-05 13:31:09.179622Z\"\n" +
            "}";

    private final String receiveRequest = "{\n" +
            "    \"their_role\": \"invitee\",\n" +
            "    \"their_label\": \"bob\",\n" +
            "    \"request_id\": \"1c6e7e67-3b34-4fa5-83ff-34b1af853499\",\n" +
            "    \"my_did\": \"MZMurMqVFiS24oxYBPZ3C7\",\n" +
            "    \"invitation_key\": \"8gdhRLtvJHzKoJGyuEqgdN1QZGYfai4wMHFGgtfDXg3D\",\n" +
            "    \"state\": \"request\",\n" +
            "    \"accept\": \"auto\",\n" +
            "    \"invitation_mode\": \"once\",\n" +
            "    \"routing_state\": \"none\",\n" +
            "    \"created_at\": \"2021-07-05 14:31:59.321382Z\",\n" +
            "    \"connection_protocol\": \"didexchange/1.0\",\n" +
            "    \"rfc23_state\": \"request-received\",\n" +
            "    \"their_did\": \"EraYCDJUPsChbkw7S1vV96\",\n" +
            "    \"connection_id\": \"b8d4f176-7967-4af7-9686-60a59b35f122\",\n" +
            "    \"updated_at\": \"2021-07-05 14:31:59.321382Z\"\n" +
            "}";

    private final String receiveActive = "{\n" +
            "    \"their_role\": \"invitee\",\n" +
            "    \"their_label\": \"bob\",\n" +
            "    \"request_id\": \"1c6e7e67-3b34-4fa5-83ff-34b1af853499\",\n" +
            "    \"my_did\": \"MZMurMqVFiS24oxYBPZ3C7\",\n" +
            "    \"invitation_key\": \"8gdhRLtvJHzKoJGyuEqgdN1QZGYfai4wMHFGgtfDXg3D\",\n" +
            "    \"state\": \"completed\",\n" +
            "    \"accept\": \"auto\",\n" +
            "    \"invitation_mode\": \"once\",\n" +
            "    \"routing_state\": \"none\",\n" +
            "    \"created_at\": \"2021-07-05 14:31:59.321382Z\",\n" +
            "    \"connection_protocol\": \"didexchange/1.0\",\n" +
            "    \"rfc23_state\": \"completed\",\n" +
            "    \"their_did\": \"EraYCDJUPsChbkw7S1vV96\",\n" +
            "    \"connection_id\": \"b8d4f176-7967-4af7-9686-60a59b35f122\",\n" +
            "    \"updated_at\": \"2021-07-05 14:31:59.735196Z\"\n" +
            "}";

    private final String inviteReceive = "{\n" +
            "    \"accept\": \"auto\",\n" +
            "    \"connection_id\": \"5d41c1cb-2856-4026-984e-24d2976a05ba\",\n" +
            "    \"connection_protocol\": \"connections/1.0\",\n" +
            "    \"updated_at\": \"2021-04-28 08:20:17.034908Z\",\n" +
            "    \"alias\": \"Invitation 1\",\n" +
            "    \"routing_state\": \"none\",\n" +
            "    \"invitation_key\": \"J9CHkDjr3oG7nq3enrojCRvsY9Cxq1Z7W1Y56GHNZe29\",\n" +
            "    \"created_at\": \"2021-04-28 08:20:17.034908Z\",\n" +
            "    \"their_role\": \"invitee\",\n" +
            "    \"state\": \"invitation\",\n" +
            "    \"invitation_mode\": \"once\",\n" +
            "    \"rfc23_state\": \"invitation-sent\"\n" +
            "}";

    private final String inviteResponse = "{\n" +
            "    \"accept\": \"auto\",\n" +
            "    \"connection_id\": \"5d41c1cb-2856-4026-984e-24d2976a05ba\",\n" +
            "    \"updated_at\": \"2021-04-28 08:20:43.237710Z\",\n" +
            "    \"their_label\": \"Wallet\",\n" +
            "    \"alias\": \"Invitation 1\",\n" +
            "    \"their_did\": \"QjqxU2wnrBGwLJnW585QWp\",\n" +
            "    \"routing_state\": \"none\",\n" +
            "    \"invitation_key\": \"J9CHkDjr3oG7nq3enrojCRvsY9Cxq1Z7W1Y56GHNZe29\",\n" +
            "    \"created_at\": \"2021-04-28 08:20:17.034908Z\",\n" +
            "    \"their_role\": \"invitee\",\n" +
            "    \"state\": \"response\",\n" +
            "    \"my_did\": \"37FY6gGZWATtKv8ywwJjdi\",\n" +
            "    \"invitation_mode\": \"once\",\n" +
            "    \"rfc23_state\": \"response-sent\"\n" +
            "}";
}
