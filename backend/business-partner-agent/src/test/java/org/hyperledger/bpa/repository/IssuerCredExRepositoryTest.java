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
package org.hyperledger.bpa.repository;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.hyperledger.aries.api.credentials.Credential;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.bpa.model.BPACredentialExchange;
import org.hyperledger.bpa.model.Partner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

@MicronautTest
public class IssuerCredExRepositoryTest {

    @Inject
    IssuerCredExRepository issuerCredExRepo;

    @Inject
    PartnerRepository partnerRepo;

    @Test
    void testUpdateCredential() {

        Partner p = partnerRepo.save(Partner.builder()
                .did("did-1")
                .ariesSupport(Boolean.TRUE)
                .build());

        BPACredentialExchange exchange = issuerCredExRepo.save(BPACredentialExchange
                .builder()
                .threadId(UUID.randomUUID().toString())
                .credentialExchangeId(UUID.randomUUID().toString())
                .state(CredentialExchangeState.PROPOSAL_SENT)
                .partner(p)
                .build());

        issuerCredExRepo.updateCredential(exchange.getId(), Credential.builder()
                .attrs(Map.of("attr1", "val1"))
                .build());

        exchange = issuerCredExRepo.findById(exchange.getId()).orElseThrow();
        Assertions.assertNotNull(exchange.getCredential());
        Assertions.assertEquals("val1", exchange.getCredential().getAttrs().get("attr1"));
    }
}
