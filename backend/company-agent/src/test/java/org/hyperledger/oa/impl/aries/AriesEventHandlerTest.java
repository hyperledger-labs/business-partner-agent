/**
 * Copyright (c) 2020 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/organizational-agent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hyperledger.oa.impl.aries;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import javax.inject.Inject;

import org.hyperledger.aries.api.proof.PresentationExchangeRecord;
import org.hyperledger.aries.webhook.EventParser;
import org.hyperledger.oa.BaseTest;
import org.hyperledger.oa.model.Partner;
import org.hyperledger.oa.model.PartnerProof;
import org.hyperledger.oa.repository.PartnerProofRepository;
import org.hyperledger.oa.repository.PartnerRepository;
import org.junit.jupiter.api.Test;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

@MicronautTest
class AriesEventHandlerTest extends BaseTest {

    @Inject
    AriesEventHandler aeh;

    @Inject
    PartnerProofRepository pRepo;

    @Inject
    PartnerRepository partnerRepo;

    private EventParser ep = new EventParser();

    @Test
    void testHandleProofRequest() throws Exception {
        String reqSent = loader.load("files/request-proof/01-verifier-request-sent.json");
        String presRec = loader.load("files/request-proof/02-verifier-presentation-received.json");
        String verified = loader.load("files/request-proof/03-verifier-verified.json");
        PresentationExchangeRecord exReqSent = ep.parsePresentProof(reqSent).get();
        PresentationExchangeRecord exPresRec = ep.parsePresentProof(presRec).get();
        PresentationExchangeRecord exVerified = ep.parsePresentProof(verified).get();

        Partner p = Partner.builder()
                .connectionId(exReqSent.getConnectionId())
                .did("did:sov:iil:dummy")
                .ariesSupport(Boolean.TRUE)
                .build();
        p = partnerRepo.save(p);

        PartnerProof pp = PartnerProof
                .builder()
                .partnerId(p.getId())
                .presentationExchangeId(exReqSent.getPresentationExchangeId())
                .build();
        pRepo.save(pp);

        aeh.handleProof(exReqSent);
        aeh.handleProof(exPresRec);
        aeh.handleProof(exVerified);

        Optional<PartnerProof> dbProof = pRepo.findByPresentationExchangeId(exVerified.getPresentationExchangeId());
        assertTrue(dbProof.isPresent());
        assertEquals(Boolean.TRUE, dbProof.get().getValid());
        assertNotNull(dbProof.get().getProof());
        assertEquals("verified", dbProof.get().getState());
        assertNotNull(dbProof.get().getProof());
    }

    @Test
    void testHandleProofProposal() throws Exception {
        String propSent = loader.load("files/send-proof/01-prover-proposal-sent.json");
        String reqRec = loader.load("files/send-proof/02-prover-request-received.json");
        String presSent = loader.load("files/send-proof/03-prover-presentation-sent.json");
        String presAcked = loader.load("files/send-proof/04-prover-presentation-acked.json");
        PresentationExchangeRecord exPropSent = ep.parsePresentProof(propSent).get();
        PresentationExchangeRecord exReqRec = ep.parsePresentProof(reqRec).get();
        PresentationExchangeRecord exPresSent = ep.parsePresentProof(presSent).get();
        PresentationExchangeRecord exPresAcked = ep.parsePresentProof(presAcked).get();

        Partner p = Partner.builder()
                .connectionId(exPropSent.getConnectionId())
                .did("did:sov:iil:dummy")
                .ariesSupport(Boolean.TRUE)
                .build();
        p = partnerRepo.save(p);

        PartnerProof pp = PartnerProof
                .builder()
                .partnerId(p.getId())
                .presentationExchangeId(exPropSent.getPresentationExchangeId())
                .build();
        pRepo.save(pp);

        aeh.handleProof(exPropSent);
        aeh.handleProof(exReqRec);
        aeh.handleProof(exPresSent);
        aeh.handleProof(exPresAcked);

        Optional<PartnerProof> dbProof = pRepo.findByPresentationExchangeId(exPresSent.getPresentationExchangeId());
        assertTrue(dbProof.isPresent());
        assertEquals(Boolean.FALSE, dbProof.get().getValid());
        assertEquals("presentation_acked", dbProof.get().getState());
        assertNotNull(dbProof.get().getProof());
    }

}
