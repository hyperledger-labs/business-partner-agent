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
package org.hyperledger.bpa.impl.aries;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.NonNull;
import org.hyperledger.aries.api.message.ProblemReport;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord;
import org.hyperledger.aries.api.present_proof.PresentationExchangeState;
import org.hyperledger.aries.config.GsonConfig;
import org.hyperledger.aries.webhook.EventParser;
import org.hyperledger.bpa.BaseTest;
import org.hyperledger.bpa.model.Partner;
import org.hyperledger.bpa.model.PartnerProof;
import org.hyperledger.bpa.repository.PartnerProofRepository;
import org.hyperledger.bpa.repository.PartnerRepository;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
class AriesEventHandlerTest extends BaseTest {

    @Inject
    PartnerProofRepository proofRepo;

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    AriesEventHandler aeh;

    private final EventParser ep = new EventParser();

    @Test
    void testHandleProofRequestAsVerifierSelf() {
        String reqSent = loader.load("files/self-request-proof/01-verifier-request-sent.json");
        String presRec = loader.load("files/self-request-proof/02-verifier-presentation-received.json");
        String verified = loader.load("files/self-request-proof/03-verifier-verified.json");
        PresentationExchangeRecord exReqSent = ep.parsePresentProof(reqSent).get();
        PresentationExchangeRecord exPresRec = ep.parsePresentProof(presRec).get();
        PresentationExchangeRecord exVerified = ep.parsePresentProof(verified).get();

        String presentationExchangeId = exVerified.getPresentationExchangeId();
        createDefaultPartner(exReqSent);

        aeh.handleProof(exReqSent);

        Optional<PartnerProof> dbProof = proofRepo.findByPresentationExchangeId(presentationExchangeId);
        assertTrue(dbProof.isPresent());
        assertEquals(PresentationExchangeState.REQUEST_SENT, dbProof.get().getState());

        aeh.handleProof(exPresRec);

        dbProof = proofRepo.findByPresentationExchangeId(presentationExchangeId);
        assertTrue(dbProof.isPresent());
        assertEquals(PresentationExchangeState.PRESENTATION_RECEIVED, dbProof.get().getState());

        aeh.handleProof(exVerified);

        dbProof = proofRepo.findByPresentationExchangeId(presentationExchangeId);
        assertTrue(dbProof.isPresent());
        assertEquals(Boolean.TRUE, dbProof.get().getValid());
        assertNotNull(dbProof.get().getProof());
        assertEquals(PresentationExchangeState.VERIFIED, dbProof.get().getState());
        assertEquals("M6Mbe3qx7vB4wpZF4sBRjt:2:bank_account:1.0", dbProof.get().getSchemaId());
        assertEquals("M6Mbe3qx7vB4wpZF4sBRjt:3:CL:571:bank_account_no_revoc",
                dbProof.get().getCredentialDefinitionId());
    }

    @Test
    void testHandleProofProposalAsProverSelf() {
        String propSent = loader.load("files/self-send-proof/01-prover-proposal-sent.json");
        String reqRec = loader.load("files/self-send-proof/02-prover-request-received.json");
        String presSent = loader.load("files/self-send-proof/03-prover-presentation-sent.json");
        String presAcked = loader.load("files/self-send-proof/04-prover-presentation-acked.json");
        PresentationExchangeRecord exPropSent = ep.parsePresentProof(propSent).get();
        PresentationExchangeRecord exReqRec = ep.parsePresentProof(reqRec).get();
        PresentationExchangeRecord exPresSent = ep.parsePresentProof(presSent).get();
        PresentationExchangeRecord exPresAcked = ep.parsePresentProof(presAcked).get();

        String presentationExchangeId = exPresSent.getPresentationExchangeId();
        createDefaultPartner(exPropSent);

        aeh.handleProof(exPropSent);

        Optional<PartnerProof> dbProof = proofRepo.findByPresentationExchangeId(presentationExchangeId);
        assertTrue(dbProof.isPresent());
        assertEquals(PresentationExchangeState.PROPOSAL_SENT, dbProof.get().getState());

        aeh.handleProof(exReqRec);

        dbProof = proofRepo.findByPresentationExchangeId(presentationExchangeId);
        assertTrue(dbProof.isPresent());
        assertEquals(PresentationExchangeState.REQUEST_RECEIVED, dbProof.get().getState());

        aeh.handleProof(exPresSent);

        dbProof = proofRepo.findByPresentationExchangeId(presentationExchangeId);
        assertTrue(dbProof.isPresent());
        assertEquals(PresentationExchangeState.PRESENTATIONS_SENT, dbProof.get().getState());

        aeh.handleProof(exPresAcked);

        dbProof = proofRepo.findByPresentationExchangeId(presentationExchangeId);
        assertTrue(dbProof.isPresent());
        assertEquals(Boolean.FALSE, dbProof.get().getValid());
        assertEquals(PresentationExchangeState.PRESENTATION_ACKED, dbProof.get().getState());
        assertNotNull(dbProof.get().getProof());
        assertEquals("M6Mbe3qx7vB4wpZF4sBRjt:2:bank_account:1.0", dbProof.get().getSchemaId());
        assertEquals("M6Mbe3qx7vB4wpZF4sBRjt:3:CL:571:bank_account_no_revoc",
                dbProof.get().getCredentialDefinitionId());
    }

    @Test
    void testHandleProofRequestAsProver() {
        String reqReceived = loader.load("files/external-request-proof/01-prover-request-received.json");
        String presSent = loader.load("files/external-request-proof/02-prover-presentation-sent.json");
        String acked = loader.load("files/external-request-proof/03-prover-presentation-acked.json");
        PresentationExchangeRecord exReqReceived = ep.parsePresentProof(reqReceived).get();
        PresentationExchangeRecord exPresSent = ep.parsePresentProof(presSent).get();
        PresentationExchangeRecord exPresAcked = ep.parsePresentProof(acked).get();

        String presentationExchangeId = exReqReceived.getPresentationExchangeId();
        createDefaultPartner(exReqReceived);

        aeh.handleProof(exReqReceived);

        Optional<PartnerProof> dbProof = proofRepo.findByPresentationExchangeId(presentationExchangeId);
        assertTrue(dbProof.isPresent());
        assertEquals(PresentationExchangeState.REQUEST_RECEIVED, dbProof.get().getState());

        aeh.handleProof(exPresSent);

        dbProof = proofRepo.findByPresentationExchangeId(presentationExchangeId);
        assertTrue(dbProof.isPresent());
        assertEquals(PresentationExchangeState.PRESENTATIONS_SENT, dbProof.get().getState());

        aeh.handleProof(exPresAcked);

        dbProof = proofRepo.findByPresentationExchangeId(presentationExchangeId);
        assertTrue(dbProof.isPresent());
        assertEquals(Boolean.FALSE, dbProof.get().getValid());
        assertEquals(PresentationExchangeState.PRESENTATION_ACKED, dbProof.get().getState());
        assertNotNull(dbProof.get().getProof());
        assertEquals("M6Mbe3qx7vB4wpZF4sBRjt:2:bank_account:1.0", dbProof.get().getSchemaId());
        assertEquals("M6Mbe3qx7vB4wpZF4sBRjt:3:CL:571:Bank Account V2",
                dbProof.get().getCredentialDefinitionId());
    }

    @Test
    void testHandleProblemReport() {
        String reqSent = loader.load("files/self-request-proof/01-verifier-request-sent.json");
        String probReport = loader.load("files/self-request-proof/04-problem-report.json");
        PresentationExchangeRecord exReqSent = ep.parsePresentProof(reqSent).get();
        ProblemReport exProblem = GsonConfig.defaultConfig().fromJson(probReport, ProblemReport.class);

        createDefaultPartner(exReqSent);

        aeh.handleProof(exReqSent);
        aeh.handleProblemReport(exProblem);

        Optional<PartnerProof> dbProof = proofRepo.findByThreadId(exProblem.getThread().getThid());
        assertTrue(dbProof.isPresent());
        assertEquals(PresentationExchangeState.REQUEST_SENT, dbProof.get().getState());
        assertTrue(dbProof.get().getProblemReport().startsWith("no matching"));

    }

    private Partner createDefaultPartner(@NonNull PresentationExchangeRecord exReqSent) {
        Partner p = Partner.builder()
                .connectionId(exReqSent.getConnectionId())
                .did("did:sov:iil:dummy")
                .ariesSupport(Boolean.TRUE)
                .build();
        return partnerRepo.save(p);
    }
}
