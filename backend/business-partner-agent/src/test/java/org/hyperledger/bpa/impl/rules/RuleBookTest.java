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
import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord;
import org.hyperledger.bpa.impl.rules.definitions.AssertVerifiedConnection;
import org.hyperledger.bpa.model.Partner;
import org.hyperledger.bpa.repository.PartnerRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.UUID;

@MicronautTest
public class RuleBookTest {

    @Inject
    RulesService ts;

    @Inject
    RulesEventHandler eo;

    @Inject
    PartnerRepository pr;

    @Test
    void testSimpleRule() {
        String connectionId = "123";

        Partner p = ModelBuilder.buildDefaultPartner().setConnectionId(connectionId);
        p = pr.save(p);

        AssertVerifiedConnection rule = new AssertVerifiedConnection(p.getId(), UUID.randomUUID());
        ts.register(p.getId(), rule);
        Assertions.assertTrue(ts.getActive(p.getId()).isPresent());

        ConnectionRecord rec = new ConnectionRecord();
        rec.setConnectionId(connectionId);
        rec.setState(ConnectionState.REQUEST);
        eo.handleConnection(rec);

        PresentationExchangeRecord ex = new PresentationExchangeRecord();
        ex.setConnectionId(connectionId);
        eo.handleProof(ex);

        Assertions.assertFalse(ts.getActive(p.getId()).isPresent());
    }
}
