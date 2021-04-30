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
                .label("a950b832-59ac-480c-8135-e76ba76f03ba")
                .alias("Alice")
                .state(ConnectionState.INIT)
                .did("dummy")
                .build());
        final ConnectionRecord invite = gson.fromJson(createInvite, ConnectionRecord.class);
        eventHandler.handleConnection(invite);

        Optional<Partner> p = repo.findByConnectionId(invite.getConnectionId());
        assertTrue(p.isPresent());
        assertEquals(ConnectionState.INVITATION, p.get().getState());

        final ConnectionRecord active = gson.fromJson(createActive, ConnectionRecord.class);
        eventHandler.handleConnection(active);

        p = repo.findByConnectionId(active.getConnectionId());
        assertTrue(p.isPresent());
        assertEquals(ConnectionState.ACTIVE, p.get().getState());
    }

    @Test
    void testReceiveIncomingConnectionEvents() {
        final ConnectionRecord invite = gson.fromJson(receiveRequest, ConnectionRecord.class);
        eventHandler.handleConnection(invite);

        Optional<Partner> p = repo.findByConnectionId(invite.getConnectionId());
        assertTrue(p.isPresent());
        assertEquals(ConnectionState.REQUEST, p.get().getState());

        final ConnectionRecord active = gson.fromJson(receiveActive, ConnectionRecord.class);
        eventHandler.handleConnection(active);

        p = repo.findByConnectionId(active.getConnectionId());
        assertTrue(p.isPresent());
        assertEquals(ConnectionState.ACTIVE, p.get().getState());
    }

    @Test
    void testCreateInvitation() {
        final ConnectionRecord invite = gson.fromJson(inviteReceive, ConnectionRecord.class);
        eventHandler.handleConnection(invite);

        Optional<Partner> p = repo.findByConnectionId(invite.getConnectionId());
        assertTrue(p.isPresent());
        assertEquals(ConnectionState.INVITATION, p.get().getState());
        assertEquals("Partner Invitation", p.get().getAlias());
        assertTrue(p.get().getDid().endsWith("unknown"));
        assertNull(p.get().getLabel());

        final ConnectionRecord response = gson.fromJson(inviteResponse, ConnectionRecord.class);
        eventHandler.handleConnection(response);

        p = repo.findByConnectionId(response.getConnectionId());
        assertTrue(p.isPresent());
        assertEquals(ConnectionState.RESPONSE, p.get().getState());
        assertEquals("Wallet", p.get().getAlias());
        assertTrue(p.get().getDid().endsWith("QjqxU2wnrBGwLJnW585QWp"));
        assertEquals("Wallet", p.get().getLabel());
    }

    private final String createInvite = "{\n" +
            "    \"accept\": \"auto\",\n" +
            "    \"connection_id\": \"9275a52f-5733-4951-a54f-7ebd7332922c\",\n" +
            "    \"updated_at\": \"2021-04-28 08:32:03.980218Z\",\n" +
            "    \"their_label\": \"a950b832-59ac-480c-8135-e76ba76f03ba\",\n" +
            "    \"alias\": \"Bob AG\",\n" +
            "    \"routing_state\": \"none\",\n" +
            "    \"created_at\": \"2021-04-28 08:32:03.980218Z\",\n" +
            "    \"their_role\": \"inviter\",\n" +
            "    \"state\": \"invitation\",\n" +
            "    \"invitation_mode\": \"once\",\n" +
            "    \"rfc23_state\": \"invitation-received\"\n" +
            "}";

    private final String createActive = "{\n" +
            "    \"accept\": \"auto\",\n" +
            "    \"connection_id\": \"9275a52f-5733-4951-a54f-7ebd7332922c\",\n" +
            "    \"updated_at\": \"2021-04-28 08:32:04.910398Z\",\n" +
            "    \"their_label\": \"a950b832-59ac-480c-8135-e76ba76f03ba\",\n" +
            "    \"alias\": \"Bob AG\",\n" +
            "    \"their_did\": \"3BDeZkKRCkLMW812rxkYha\",\n" +
            "    \"routing_state\": \"none\",\n" +
            "    \"created_at\": \"2021-04-28 08:32:03.980218Z\",\n" +
            "    \"their_role\": \"inviter\",\n" +
            "    \"state\": \"active\",\n" +
            "    \"my_did\": \"WrZgFxyJm1Ty5z1PTiiKN4\",\n" +
            "    \"invitation_mode\": \"once\",\n" +
            "    \"request_id\": \"18eecaa0-9393-4eeb-adaf-46e63a5fc47f\",\n" +
            "    \"rfc23_state\": \"completed\"\n" +
            "}";

    private final String receiveRequest = "{\n" +
            "    \"accept\": \"auto\",\n" +
            "    \"connection_id\": \"c8c7ef29-fd4f-4786-a277-9fd512a47497\",\n" +
            "    \"updated_at\": \"2021-04-28 08:37:47.412662Z\",\n" +
            "    \"their_label\": \"bob\",\n" +
            "    \"their_did\": \"6u1vB2CRUL1z85wdK3WNV1\",\n" +
            "    \"routing_state\": \"none\",\n" +
            "    \"invitation_key\": \"CqDPur9zQWRv3ronzRmSFpRYXtdsPHoMVHhcKFmEHapx\",\n" +
            "    \"created_at\": \"2021-04-28 08:37:47.412662Z\",\n" +
            "    \"their_role\": \"inviter\",\n" +
            "    \"state\": \"request\",\n" +
            "    \"my_did\": \"8Ry1L98XdZazJwiDPoqcVA\",\n" +
            "    \"invitation_mode\": \"once\",\n" +
            "    \"rfc23_state\": \"request-sent\"\n" +
            "}";

    private final String receiveActive = "{\n" +
            "    \"accept\": \"auto\",\n" +
            "    \"connection_id\": \"c8c7ef29-fd4f-4786-a277-9fd512a47497\",\n" +
            "    \"updated_at\": \"2021-04-28 08:37:47.727279Z\",\n" +
            "    \"their_label\": \"bob\",\n" +
            "    \"their_did\": \"6u1vB2CRUL1z85wdK3WNV1\",\n" +
            "    \"routing_state\": \"none\",\n" +
            "    \"invitation_key\": \"CqDPur9zQWRv3ronzRmSFpRYXtdsPHoMVHhcKFmEHapx\",\n" +
            "    \"created_at\": \"2021-04-28 08:37:47.412662Z\",\n" +
            "    \"their_role\": \"inviter\",\n" +
            "    \"state\": \"active\",\n" +
            "    \"my_did\": \"8Ry1L98XdZazJwiDPoqcVA\",\n" +
            "    \"invitation_mode\": \"once\",\n" +
            "    \"rfc23_state\": \"completed\"\n" +
            "}";

    private final String inviteReceive = "{\n" +
            "    \"accept\": \"auto\",\n" +
            "    \"connection_id\": \"5d41c1cb-2856-4026-984e-24d2976a05ba\",\n" +
            "    \"updated_at\": \"2021-04-28 08:20:17.034908Z\",\n" +
            "    \"alias\": \"Partner Invitation\",\n" +
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
            "    \"alias\": \"Partner Invitation\",\n" +
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
