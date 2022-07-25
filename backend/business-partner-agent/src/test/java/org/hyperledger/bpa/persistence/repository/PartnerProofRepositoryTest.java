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
import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord;
import org.hyperledger.aries.api.present_proof.PresentationExchangeState;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.persistence.model.Partner;
import org.hyperledger.bpa.persistence.model.PartnerProof;
import org.hyperledger.bpa.persistence.model.converter.ExchangePayload;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
class PartnerProofRepositoryTest {

    @Inject
    PartnerProofRepository repo;

    @Inject
    PartnerRepository partnerRepo;

    @Test
    void testUpdateProof() {
        Partner dbP = partnerRepo
                .save(Partner.builder().did("dummy").alias("alias").ariesSupport(Boolean.FALSE).build());
        PartnerProof pp = PartnerProof
                .builder()
                .partner(dbP)
                .presentationExchangeId("pres-1")
                .type(CredentialType.INDY)
                .build();
        pp = repo.save(pp);
        long uc = repo.updateReceivedProof(pp.getId(), Boolean.TRUE, PresentationExchangeState.VERIFIED,
                ExchangePayload.indy(Map.of("testGroup", PresentationExchangeRecord.RevealedAttributeGroup.builder()
                        .revealedAttribute("testKey", "testValue").build())));
        assertEquals(1, uc);

        PartnerProof updated = repo.findById(pp.getId()).orElseThrow();
        assertEquals(PresentationExchangeState.VERIFIED, updated.getState());
        assertEquals(dbP.getId(), pp.getPartner().getId());
    }

    @Test
    void testExchangeStateDecorator() {
        Partner dbP = partnerRepo
                .save(Partner.builder().did("dummy").alias("alias").ariesSupport(Boolean.FALSE).build());
        PartnerProof pp = PartnerProof
                .builder()
                .type(CredentialType.INDY)
                .partner(dbP)
                .presentationExchangeId("pres-1")
                .pushStateChange(PresentationExchangeState.PROPOSAL_SENT, Instant.ofEpochMilli(1631760000000L))
                .build();
        repo.save(pp);

        pp = repo.findById(pp.getId()).orElseThrow();
        pp.pushStates(PresentationExchangeState.REQUEST_RECEIVED, Instant.ofEpochMilli(1631770000000L));
        pp.pushStates(PresentationExchangeState.PRESENTATION_ACKED, Instant.ofEpochMilli(1631780000000L));

        repo.update(pp);

        pp = repo.findById(pp.getId()).orElseThrow();

        assertEquals(PresentationExchangeState.PRESENTATION_ACKED, pp.getState());
        assertEquals(PresentationExchangeState.PRESENTATION_ACKED,
                pp.getStateToTimestamp().toApi().keySet().toArray()[2]);
    }

}
