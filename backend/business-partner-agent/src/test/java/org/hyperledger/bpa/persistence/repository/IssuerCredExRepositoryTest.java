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
package org.hyperledger.bpa.persistence.repository;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.hyperledger.aries.api.credentials.Credential;
import org.hyperledger.aries.api.credentials.CredentialAttributes;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;
import org.hyperledger.bpa.persistence.model.BPACredentialExchange;
import org.hyperledger.bpa.persistence.model.Partner;
import org.hyperledger.bpa.persistence.model.converter.ExchangePayload;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
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
                .attrs(List.of(new CredentialAttributes("attr1", "val1", null)))
                .build());

        exchange = issuerCredExRepo.findById(exchange.getId()).orElseThrow();
        Assertions.assertNotNull(exchange.getIndyCredential());
        Assertions.assertEquals("val1", exchange.getIndyCredential().getAttrs().stream()
                .filter(attr -> attr.getName().equals("attr1")).findFirst().get().getValue());
    }

    @Test
    void testSaveWithCredentialProposal() {
        Partner p = partnerRepo.save(Partner.builder()
                .did("did-1")
                .ariesSupport(Boolean.TRUE)
                .build());

        BPACredentialExchange exchange = issuerCredExRepo.save(BPACredentialExchange
                .builder()
                .threadId(UUID.randomUUID().toString())
                .credentialExchangeId(UUID.randomUUID().toString())
                .credentialProposal(ExchangePayload
                        .indy(V1CredentialExchange.CredentialProposalDict.CredentialProposal.builder()
                                .attributes(CredentialAttributes.from(Map.of("attr1", "value1")))
                                .build()))
                .state(CredentialExchangeState.PROPOSAL_SENT)
                .partner(p)
                .build());

        BPACredentialExchange saved = issuerCredExRepo.save(exchange);
        saved = issuerCredExRepo.findById(saved.getId()).orElseThrow();
        Assertions.assertNotNull(saved.getCredentialProposal());
        Assertions.assertEquals("value1", saved.getCredentialProposal().getIndy().getAttributes().get(0).getValue());
    }
}
