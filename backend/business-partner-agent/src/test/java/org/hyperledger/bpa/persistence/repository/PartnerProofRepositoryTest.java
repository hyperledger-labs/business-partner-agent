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
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord;
import org.hyperledger.aries.api.present_proof.PresentationExchangeState;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.persistence.model.PartnerProof;
import org.hyperledger.bpa.persistence.model.converter.ExchangePayload;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
class PartnerProofRepositoryTest {

    Instant timestamp =  Instant.ofEpochMilli(1631770000000L);

    Integer counter = 0;

    @Inject
    PartnerProofRepository repo;

    @Test
    void testUpdateProof() {
        PartnerProof pp = PartnerProof
                .builder()
                .partnerId(UUID.randomUUID())
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
    }

    @Test
    void testExchangeStateDecorator() {
        PartnerProof pp = createRandomPartnerProof();
        repo.save(pp);

        pp = repo.findById(pp.getId()).orElseThrow();
        pp.pushStates(PresentationExchangeState.REQUEST_RECEIVED, timestamp);
        pp.pushStates(PresentationExchangeState.PRESENTATION_ACKED, timestamp);

        repo.update(pp);

        pp = repo.findById(pp.getId()).orElseThrow();

        assertEquals(PresentationExchangeState.PRESENTATION_ACKED, pp.getState());
        assertEquals(PresentationExchangeState.PRESENTATION_ACKED,
                pp.getStateToTimestamp().toApi().keySet().toArray()[2]);
    }

    @Test
    void testGetPresentationExchangeListByPartnerId(){
      PartnerProof pp = createRandomPartnerProof();
      int numberOfPresExRecords = 42;
      for(int i = 1; i<numberOfPresExRecords; i++) {
        repo.save(createDummyPresEx(pp));
      }

      assertEquals(42, repo.findByPartnerId(
        pp.getPartnerId(), Pageable.unpaged()).getNumberOfElements());

      assertEquals(9, repo.findByPartnerId(
        pp.getPartnerId(), Pageable.from(0, 5)).getTotalPages());

      assertEquals(2, repo.findByPartnerId(
        pp.getPartnerId(), Pageable.from(8, 5)).getNumberOfElements());

//      assertEquals(2, repo.findByPartnerId(
//        pp.getPartnerId(), Pageable.from(8, 5)));

    }

    public PartnerProof createDummyPresEx(PartnerProof partnerProof) {
      return PartnerProof
        .builder()
        .id(partnerProof.getId())
        .partnerId(partnerProof.getPartnerId())
        .state(partnerProof.getState())
        .proofRequest(partnerProof.getProofRequest())
        .presentationExchangeId(UUID.randomUUID().toString())
        .role(partnerProof.getRole())
        .type(partnerProof.getType())
        .problemReport(partnerProof.getProblemReport())
        .exchangeVersion(partnerProof.getExchangeVersion())
        .stateToTimestamp(partnerProof.getStateToTimestamp())
        .valid(partnerProof.getValid())
        .updatedAt(timestamp)
        .build();
    }

    public PartnerProof createRandomPartnerProof() {
      return repo.save(PartnerProof
        .builder()
        .type(CredentialType.INDY)
        .partnerId(UUID.randomUUID())
        .presentationExchangeId("pres-" + counter++)
        .pushStateChange(PresentationExchangeState.PROPOSAL_SENT, timestamp)
        .build());
    }
}
