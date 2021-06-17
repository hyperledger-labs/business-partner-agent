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
package org.hyperledger.bpa.impl.rules;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.hyperledger.ModelBuilder;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.bpa.model.Partner;
import org.hyperledger.bpa.repository.PartnerRepository;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

@MicronautTest
public class RulesTest {

    @Inject
    RulesService rs;

    @Inject
    RulesEventHandler handler;

    @Inject
    PartnerRepository pr;

    @Test
    void testRunRule() {
        String connectionId = "123";

        Partner p = ModelBuilder.buildDefaultPartner().setConnectionId(connectionId);
        pr.save(p);

        rs.register(new RulesData.Trigger.ConnectionTrigger(), new RulesData.Action.TagConnection());

        ConnectionRecord rec = new ConnectionRecord();
        rec.setConnectionId(connectionId);
        rec.setState(ConnectionState.REQUEST);

        handler.handleConnection(rec);
    }
}
