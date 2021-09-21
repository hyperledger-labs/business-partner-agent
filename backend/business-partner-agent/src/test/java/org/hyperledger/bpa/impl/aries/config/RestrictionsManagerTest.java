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
package org.hyperledger.bpa.impl.aries.config;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.exception.AriesException;
import org.hyperledger.aries.api.ledger.DidVerkeyResponse;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.controller.api.admin.TrustedIssuer;
import org.hyperledger.bpa.model.BPASchema;
import org.hyperledger.bpa.repository.BPASchemaRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

@MicronautTest
public class RestrictionsManagerTest {

    private final AriesClient ac = Mockito.mock(AriesClient.class);

    @Inject
    RestrictionsManager mgmt;

    @Inject
    BPASchemaRepository schemaRepo;

    @BeforeEach
    void setup() {
        mgmt.setAc(ac);
    }

    @Test
    void testAddRestrictionNoSchema() {
        Assertions.assertThrows(WrongApiUsageException.class,
                () -> mgmt.addRestriction(UUID.randomUUID(), "123", null));
    }

    @Test
    void testNoIssuerDidOnLedger() throws Exception {
        String schemaId = "schemaId";

        Mockito.when(ac.ledgerDidVerkey(Mockito.anyString()))
                .thenThrow(new AriesException(404, "test"));

        BPASchema dbSchema = schemaRepo.save(BPASchema.builder()
                .schemaId(schemaId)
                .seqNo(571)
                .build());

        Assertions.assertThrows(WrongApiUsageException.class,
                () -> mgmt.addRestriction(dbSchema.getId(), "5mwQSWnRePrZ3oF67C4Kqe", null));
    }

    @Test
    void testAddRestrictionSuccess() throws Exception {
        DidVerkeyResponse verKey = new DidVerkeyResponse();
        verKey.setVerkey("dummy");
        Mockito.when(ac.ledgerDidVerkey(Mockito.anyString()))
                .thenReturn(Optional.of(verKey));
        BPASchema dbSchema = schemaRepo.save(BPASchema.builder()
                .schemaId("1234")
                .seqNo(571)
                .build());
        Optional<TrustedIssuer> credDefId = mgmt
                .addRestriction(dbSchema.getId(), "5mwQSWnRePrZ3oF67C4KqD", null);
        Assertions.assertTrue(credDefId.isPresent());

        Optional<BPASchema> schemaReloaded = schemaRepo.findById(dbSchema.getId());
        Assertions.assertTrue(schemaReloaded.isPresent());
        Assertions.assertNotNull(schemaReloaded.get().getRestrictions());
        Assertions.assertEquals(1, schemaReloaded.get().getRestrictions().size());
    }

    @Test
    void testGetIssuerLabelByDid() throws Exception {
        DidVerkeyResponse verKey = new DidVerkeyResponse();
        verKey.setVerkey("dummy");
        Mockito.when(ac.ledgerDidVerkey(Mockito.anyString()))
                .thenReturn(Optional.of(verKey));
        BPASchema dbSchema = schemaRepo.save(BPASchema.builder()
                .schemaId("1234")
                .seqNo(571)
                .build());
        String label = "myLabel123";
        String issuerDid = "5mwQSWnRePrZ3oF67C4KqD";
        mgmt.addRestriction(dbSchema.getId(), issuerDid, label);

        Assertions.assertNull(mgmt.findIssuerLabelByDid(null));
        Assertions.assertNull(mgmt.findIssuerLabelByDid("something"));
        Assertions.assertEquals(label, mgmt.findIssuerLabelByDid(issuerDid));
        Assertions.assertEquals(label, mgmt.findIssuerLabelByDid("did:sov:" + issuerDid));
        Assertions.assertEquals(label, mgmt.findIssuerLabelByDid(issuerDid + ":3:CL:571:bank"));
    }
}
