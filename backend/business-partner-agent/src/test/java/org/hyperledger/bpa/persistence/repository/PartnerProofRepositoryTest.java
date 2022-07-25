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

    private Instant timestamp =  Instant.ofEpochMilli(1631760000000L);

    private Integer counter = 0;

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
        PartnerProof pp = PartnerProof
                .builder()
                .type(CredentialType.INDY)
                .partnerId(UUID.randomUUID())
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
    void testGetPresentationExchangeListByPartnerId(){
      PartnerProof pp = createRandomPartnerProof();
      int numberOfPresExRecords = 42;
      for(int i = 1; i<numberOfPresExRecords; i++) {
        repo.save(createDummyPresEx(pp));
      }

//      Sort newSort = new Sort.Order("presentationExchangeId", Sort.Order.Direction.DESC);
      Sort.Order newOrder = new Sort.Order("presentationExchangeId", Sort.Order.Direction.DESC, true);

      assertEquals(9, repo.findByPartnerId(
        pp.getPartnerId(), Pageable.from(2, 5)).getTotalPages());

      assertEquals(2, repo.findByPartnerId(
        pp.getPartnerId(), Pageable.from(8, 5)).getNumberOfElements());

      assertEquals("pres-1", repo.findByPartnerId(
        pp.getPartnerId(), Pageable.from(0,5).order("presentationExchangeId",
          Sort.Order.Direction.ASC))
        .getContent()
        .get(0)
        .getPresentationExchangeId());

      assertEquals("pres-9", repo.findByPartnerId(
          pp.getPartnerId(), Pageable.from(0,5).order(newOrder))
        .getContent()
        .get(0)
        .getPresentationExchangeId()
        .toString());

      assertEquals("pres-42", repo.findByPartnerId(
          pp.getPartnerId(), Pageable.from(1,5).order("presentationExchangeId",
            Sort.Order.Direction.DESC))
        .getContent()
        .get(0)
        .getPresentationExchangeId());

      assertEquals("pres-38", repo.findByPartnerId(
          pp.getPartnerId(), Pageable.from(2,5).order("presentationExchangeId",
            Sort.Order.Direction.DESC))
        .getContent()
        .get(0)
        .getPresentationExchangeId());

      assertEquals("pres-33", repo.findByPartnerId(
          pp.getPartnerId(), Pageable.from(3,5).order("presentationExchangeId",
            Sort.Order.Direction.DESC))
        .getContent()
        .get(0)
        .getPresentationExchangeId());

      assertEquals("pres-29", repo.findByPartnerId(
          pp.getPartnerId(), Pageable.from(4,5).order("presentationExchangeId",
            Sort.Order.Direction.DESC))
        .getContent()
        .get(0)
        .getPresentationExchangeId());

      assertEquals("pres-24", repo.findByPartnerId(
          pp.getPartnerId(), Pageable.from(5,5).order("presentationExchangeId",
            Sort.Order.Direction.DESC))
        .getContent()
        .get(0)
        .getPresentationExchangeId());

    }

    public PartnerProof createDummyPresEx(PartnerProof partnerProof) {
      counter += 1;
      return PartnerProof
        .builder()
        .id(partnerProof.getId())
        .partnerId(partnerProof.getPartnerId())
        .state(partnerProof.getState())
        .proofRequest(partnerProof.getProofRequest())
        .presentationExchangeId("pres-" + counter)
        .role(partnerProof.getRole())
        .type(partnerProof.getType())
        .problemReport(partnerProof.getProblemReport())
        .exchangeVersion(partnerProof.getExchangeVersion())
        .stateToTimestamp(partnerProof.getStateToTimestamp())
        .valid(partnerProof.getValid())
        .updatedAt(timestamp.plusSeconds(counter))
        .build();
    }

    public PartnerProof createRandomPartnerProof() {
      counter += 1;
      return repo.save(PartnerProof
        .builder()
        .type(CredentialType.INDY)
        .partnerId(UUID.randomUUID())
        .presentationExchangeId("pres-" + counter)
        .pushStateChange(PresentationExchangeState.PROPOSAL_SENT, timestamp)
        .build());
    }
}
