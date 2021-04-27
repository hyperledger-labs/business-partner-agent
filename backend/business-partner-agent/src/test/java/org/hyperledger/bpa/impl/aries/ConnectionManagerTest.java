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
    ConnectionManager mgmt;

    @Inject
    PartnerRepository repo;

    @Test
    void testCreateConnectionEvents() {
        repo.save(Partner
                .builder()
                .ariesSupport(Boolean.TRUE)
                .label("33eb7945-a8c8-4cff-8046-727457dc4272")
                .alias("Alice")
                .state(ConnectionState.INIT)
                .did("dummy")
                .build());
        final ConnectionRecord invite = gson.fromJson(createInvite, ConnectionRecord.class);
        mgmt.handleConnectionEvent(invite);

        Optional<Partner> p = repo.findByConnectionId(invite.getConnectionId());
        assertTrue(p.isPresent());

        final ConnectionRecord active = gson.fromJson(createActive, ConnectionRecord.class);
        mgmt.handleConnectionEvent(active);

        p = repo.findByConnectionId(active.getConnectionId());
        assertTrue(p.isPresent());
        assertEquals(ConnectionState.ACTIVE, p.get().getState());
    }

    @Test
    void testReceiveConnectionEvents() {
        final ConnectionRecord invite = gson.fromJson(receiveInvite, ConnectionRecord.class);
        mgmt.handleConnectionEvent(invite);

        Optional<Partner> p = repo.findByConnectionId(invite.getConnectionId());
        assertTrue(p.isPresent());

        final ConnectionRecord active = gson.fromJson(receiveActive, ConnectionRecord.class);
        mgmt.handleConnectionEvent(active);

        p = repo.findByConnectionId(active.getConnectionId());
        assertTrue(p.isPresent());
        assertEquals(ConnectionState.ACTIVE, p.get().getState());
    }

    private final String createInvite = "{\n" +
            "    \"accept\": \"auto\",\n" +
            "    \"created_at\": \"2020-09-17 08:03:09.370827Z\",\n" +
            "    \"connection_id\": \"19a61365-894a-42b6-bdd1-e747ec9321fd\",\n" +
            "    \"updated_at\": \"2020-09-17 08:03:09.370827Z\",\n" +
            "    \"initiator\": \"external\",\n" +
            "    \"routing_state\": \"none\",\n" +
            "    \"their_label\": \"33eb7945-a8c8-4cff-8046-727457dc4272\",\n" +
            "    \"invitation_mode\": \"once\",\n" +
            "    \"state\": \"invitation\",\n" +
            "    \"alias\": \"Alice\"\n" +
            "}";

    private final String createActive = "{\n" +
            "    \"accept\": \"auto\",\n" +
            "    \"created_at\": \"2020-09-17 08:03:09.370827Z\",\n" +
            "    \"connection_id\": \"19a61365-894a-42b6-bdd1-e747ec9321fd\",\n" +
            "    \"updated_at\": \"2020-09-17 08:03:10.632125Z\",\n" +
            "    \"initiator\": \"external\",\n" +
            "    \"routing_state\": \"none\",\n" +
            "    \"their_label\": \"33eb7945-a8c8-4cff-8046-727457dc4272\",\n" +
            "    \"my_did\": \"KkjJTaRczRtMDAEUoS1tDJ\",\n" +
            "    \"invitation_mode\": \"once\",\n" +
            "    \"state\": \"active\",\n" +
            "    \"alias\": \"Alice\",\n" +
            "    \"their_did\": \"2mWTTjMyFrUX1ApNN2zUe1\"\n" +
            "}";

    private final String receiveInvite = " {\n" +
            "    \"created_at\": \"2020-09-17 08:03:10.123103Z\",\n" +
            "    \"my_did\": \"2mWTTjMyFrUX1ApNN2zUe1\",\n" +
            "    \"their_label\": \"Bob's Agent\",\n" +
            "    \"initiator\": \"external\",\n" +
            "    \"their_did\": \"KkjJTaRczRtMDAEUoS1tDJ\",\n" +
            "    \"routing_state\": \"none\",\n" +
            "    \"updated_at\": \"2020-09-17 08:03:10.123103Z\",\n" +
            "    \"invitation_key\": \"GhSykMRnxSdtERYB6AjGTXCG4vhqXP6j4mMXPYyhNxw3\",\n" +
            "    \"invitation_mode\": \"once\",\n" +
            "    \"connection_id\": \"d8e5caff-e0eb-4995-ac43-59854de96773\",\n" +
            "    \"state\": \"request\",\n" +
            "    \"accept\": \"auto\"\n" +
            "}";

    private final String receiveActive = "{\n" +
            "    \"created_at\": \"2020-09-17 08:03:10.123103Z\",\n" +
            "    \"my_did\": \"2mWTTjMyFrUX1ApNN2zUe1\",\n" +
            "    \"their_label\": \"Bob's Agent\",\n" +
            "    \"initiator\": \"external\",\n" +
            "    \"their_did\": \"KkjJTaRczRtMDAEUoS1tDJ\",\n" +
            "    \"routing_state\": \"none\",\n" +
            "    \"updated_at\": \"2020-09-17 08:03:10.553556Z\",\n" +
            "    \"invitation_key\": \"GhSykMRnxSdtERYB6AjGTXCG4vhqXP6j4mMXPYyhNxw3\",\n" +
            "    \"invitation_mode\": \"once\",\n" +
            "    \"connection_id\": \"d8e5caff-e0eb-4995-ac43-59854de96773\",\n" +
            "    \"state\": \"active\",\n" +
            "    \"accept\": \"auto\"\n" +
            "}";

}
