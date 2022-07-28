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

import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.Sort;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.reactivex.rxjava3.annotations.NonNull;
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

    private final Instant timestamp = Instant.ofEpochMilli(1631760000000L);

    @Inject
    PartnerProofRepository repo;

    @Inject
    PartnerRepository partnerRepo;

    @Test
    void testUpdateProof() {
        Partner dbP = createDummyPartner();
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
        Partner dbP = createDummyPartner();
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

    @Test
    void testGetPresentationExchangeListByPartnerId() {
        PartnerProof pp = createRandomPartnerProof();
        repo.save(createDummyPresEx(pp, "pres-b"));
        repo.save(createDummyPresEx(pp, "pres-c"));
        repo.save(createDummyPresEx(pp, "pres-d"));
        repo.save(createDummyPresEx(pp, "pres-e"));
        repo.save(createDummyPresEx(pp, "pres-f"));
        repo.save(createDummyPresEx(pp, "pres-g"));
        repo.save(createDummyPresEx(pp, "pres-h"));
        repo.save(createDummyPresEx(pp, "pres-i"));
        repo.save(createDummyPresEx(pp, "pres-j"));
        repo.save(createDummyPresEx(pp, "pres-k"));

        assertEquals(3, repo.findByPartnerId(
                pp.getPartner().getId(), Pageable.from(0, 5)).getTotalPages());

        assertEquals("pres-k",
                repo.findByPartnerId(pp.getPartner().getId(),
                                Pageable.from(0, 5)
                                        .order("presentationExchangeId",
                                                Sort.Order.Direction.DESC))
                        .getContent()
                        .get(0)
                        .getPresentationExchangeId());

        assertEquals("pres-f",
                repo.findByPartnerId(pp.getPartner().getId(),
                                Pageable.from(1, 5)
                                        .order("presentationExchangeId",
                                                Sort.Order.Direction.DESC))
                        .getContent()
                        .get(0)
                        .getPresentationExchangeId());

        assertEquals("pres-a", repo.findByPartnerId(pp.getPartner().getId(),
                        Pageable.from(2, 5)
                                .order("presentationExchangeId",
                                        Sort.Order.Direction.DESC))
                .getContent()
                .get(0)
                .getPresentationExchangeId());
    }

    private PartnerProof createDummyPresEx(@NonNull PartnerProof partnerProof, @NonNull String pesId) {
        return PartnerProof
                .builder()
                .id(partnerProof.getId())
                .partner(partnerProof.getPartner())
                .state(partnerProof.getState())
                .proofRequest(partnerProof.getProofRequest())
                .presentationExchangeId(pesId)
                .role(partnerProof.getRole())
                .type(partnerProof.getType())
                .problemReport(partnerProof.getProblemReport())
                .exchangeVersion(partnerProof.getExchangeVersion())
                .stateToTimestamp(partnerProof.getStateToTimestamp())
                .valid(partnerProof.getValid())
                .updatedAt(timestamp.plusSeconds(1))
                .build();
    }

    private PartnerProof createRandomPartnerProof() {
        return repo.save(PartnerProof
                .builder()
                .type(CredentialType.INDY)
                .partner(createDummyPartner())
                .presentationExchangeId("pres-a")
                .pushStateChange(PresentationExchangeState.PROPOSAL_SENT, timestamp)
                .build());
    }

    private Partner createDummyPartner() {
        return partnerRepo.save(Partner.builder().did("dummy").alias("alias").ariesSupport(Boolean.FALSE).build());
    }
}
