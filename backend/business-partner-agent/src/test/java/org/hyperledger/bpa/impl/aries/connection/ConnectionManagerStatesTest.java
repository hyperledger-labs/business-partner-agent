/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
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
package org.hyperledger.bpa.impl.aries.connection;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.connection.ConnectionTheirRole;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@MicronautTest
public class ConnectionManagerStatesTest {

    @Inject
    ConnectionManager m;

    @Test
    void testDidParsingByEventType() {
        ConnectionRecord r = new ConnectionRecord();

        r.setConnectionProtocol(ConnectionRecord.ConnectionProtocol.CONNECTION_V1)
                .setTheirRole(ConnectionTheirRole.INVITEE)
                .setTheirPublicDid(null)
                .setTheirDid("1");
        Assertions.assertEquals("did:peer:1", m.resolveDidFromRecord(r));

        r.setConnectionProtocol(ConnectionRecord.ConnectionProtocol.CONNECTION_V1)
                .setTheirRole(ConnectionTheirRole.INVITER)
                .setInvitationMsgId("2")
                .setTheirPublicDid(null)
                .setTheirDid("1");
        Assertions.assertEquals("did:peer:1", m.resolveDidFromRecord(r));

        r.setConnectionProtocol(ConnectionRecord.ConnectionProtocol.DID_EXCHANGE_V1)
                .setTheirRole(ConnectionTheirRole.REQUESTER)
                .setInvitationMsgId("2")
                .setTheirPublicDid(null)
                .setTheirDid("1");
        Assertions.assertEquals("did:peer:1", m.resolveDidFromRecord(r));

        r.setConnectionProtocol(ConnectionRecord.ConnectionProtocol.DID_EXCHANGE_V1)
                .setTheirRole(ConnectionTheirRole.RESPONDER)
                .setInvitationMsgId("2")
                .setTheirPublicDid("3")
                .setTheirDid("1");
        Assertions.assertEquals("did:sov:3", m.resolveDidFromRecord(r));

        r.setConnectionProtocol(ConnectionRecord.ConnectionProtocol.DID_EXCHANGE_V1)
                .setTheirRole(ConnectionTheirRole.REQUESTER)
                .setInvitationMsgId(null)
                .setTheirPublicDid(null)
                .setTheirDid("1");
        Assertions.assertEquals("did:sov:1", m.resolveDidFromRecord(r));

        r.setConnectionProtocol(ConnectionRecord.ConnectionProtocol.DID_EXCHANGE_V1)
                .setTheirRole(ConnectionTheirRole.RESPONDER)
                .setInvitationMsgId(null)
                .setTheirPublicDid("3")
                .setTheirDid("1");
        Assertions.assertEquals("did:sov:3", m.resolveDidFromRecord(r));
    }
}
