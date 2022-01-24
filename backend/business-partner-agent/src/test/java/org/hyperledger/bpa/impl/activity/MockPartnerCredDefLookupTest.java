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
package org.hyperledger.bpa.impl.activity;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.client.LedgerExplorerClient;
import org.hyperledger.bpa.controller.api.partner.PartnerCredentialType;
import org.hyperledger.bpa.model.BPASchema;
import org.hyperledger.bpa.model.Partner;
import org.hyperledger.bpa.repository.BPARestrictionsRepository;
import org.hyperledger.bpa.repository.BPASchemaRepository;
import org.hyperledger.bpa.repository.PartnerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@MicronautTest
@ExtendWith(MockitoExtension.class)
class MockPartnerCredDefLookupTest {

    @Mock
    LedgerExplorerClient ledger;

    @Mock
    BPASchemaRepository schemaRepo;

    @Inject
    PartnerCredDefLookup lookup;

    @Inject
    PartnerRepository pRepo;

    @Inject
    BPARestrictionsRepository restrictionsRepo;

    @BeforeEach
    public void setup() {
        lookup.setLedger(Optional.of(ledger));
        lookup.setSchemaRepo(schemaRepo);
        lookup.setDidPrefix("");
        lookup.setRestrictionsRepo(restrictionsRepo);
    }

    @Test
    void testLookup() {
        String did1 = "did-1";
        String did2 = "did-2";

        pRepo.save(Partner
                .builder()
                .ariesSupport(Boolean.TRUE)
                .did(did1)
                .connectionId(did1)
                .build());

        pRepo.save(Partner
                .builder()
                .ariesSupport(Boolean.TRUE)
                .did(did2)
                .connectionId(did2)
                .build());

        when(schemaRepo.findAll()).thenReturn(List.of(
                BPASchema.builder().seqNo(1077).type(CredentialType.INDY).build(),
                BPASchema.builder().seqNo(977).type(CredentialType.INDY).build(),
                BPASchema.builder().seqNo(9999).type(CredentialType.INDY).build()));

        when(ledger.queryCredentialDefinitions(anyString()))
                .thenReturn(Optional.of(List.of(
                        PartnerCredentialType.fromCredDefId("did-1:3:CL:1077:commercial register entry"),
                        PartnerCredentialType.fromCredDefId("did-1:3:CL:1077:commereg test"),
                        PartnerCredentialType.fromCredDefId("other:3:CL:1077:commreg"))))
                .thenReturn(Optional.of(List.of(
                        PartnerCredentialType.fromCredDefId("did-1:3:CL:977:bank"),
                        PartnerCredentialType.fromCredDefId("did-2:3:CL:977:my-bank"))))
                .thenReturn(Optional.empty());

        lookup.lookupTypesForAllPartners();

        List<Partner> partners = pRepo.findBySupportedCredential("1077");
        assertEquals(1, partners.size());
        assertEquals(did1, partners.get(0).getDid());

        partners = pRepo.findBySupportedCredential("977");
        assertEquals(2, partners.size());
        final List<String> pList = partners.stream().map(Partner::getDid).toList();
        assertTrue(pList.contains(did1));
        assertTrue(pList.contains(did2));

        partners = pRepo.findBySupportedCredential("9999");
        assertEquals(0, partners.size());
    }
}
