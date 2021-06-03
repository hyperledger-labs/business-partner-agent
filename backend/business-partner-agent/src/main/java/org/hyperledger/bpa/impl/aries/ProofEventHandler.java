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

import io.micronaut.core.util.CollectionUtils;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.api.present_proof.PresentationExchangeInitiator;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRole;
import org.hyperledger.aries.api.present_proof.PresentationExchangeState;
import org.hyperledger.bpa.controller.api.WebSocketMessageBody;
import org.hyperledger.bpa.impl.MessageService;
import org.hyperledger.bpa.model.PartnerProof;
import org.hyperledger.bpa.repository.PartnerProofRepository;
import org.hyperledger.bpa.repository.PartnerRepository;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.UUID;

@Slf4j
@Singleton
public class ProofEventHandler {

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    PartnerProofRepository pProofRepo;

    @Inject
    ProofManager proofManager;

    @Inject
    MessageService messageService;

    void dispatch(PresentationExchangeRecord proof) {
        if (proof.isVerified() && PresentationExchangeRole.VERIFIER.equals(proof.getRole())
                || PresentationExchangeState.PRESENTATION_ACKED.equals(proof.getState())
                        && PresentationExchangeRole.PROVER.equals(proof.getRole())) {
            handleAckedOrVerified(proof);
        } else if (PresentationExchangeState.REQUEST_RECEIVED.equals(proof.getState())
                && PresentationExchangeRole.PROVER.equals(proof.getRole())) {
            handleProofRequest(proof);
        } else {
            handleAll(proof);
        }
    }

    /**
     * Default proof event handler that either stores or updates partner proofs
     * 
     * @param proof {@link PresentationExchangeRecord}
     */
    private void handleAll(PresentationExchangeRecord proof) {
        partnerRepo.findByConnectionId(proof.getConnectionId())
                .ifPresentOrElse(
                        p -> pProofRepo.findByPresentationExchangeId(proof.getPresentationExchangeId()).ifPresentOrElse(
                                pp -> pProofRepo.updateState(pp.getId(), proof.getState()),
                                () -> pProofRepo.save(defaultProof(p.getId(), proof))),
                        () -> log.warn("Received proof event that does not match any connection"));
    }

    /**
     * Handles all events that are either acked or verified connectionless proofs
     * are currently not handled
     * 
     * @param proof {@link PresentationExchangeRecord}
     */
    private void handleAckedOrVerified(PresentationExchangeRecord proof) {
        pProofRepo.findByPresentationExchangeId(proof.getPresentationExchangeId()).ifPresent(pp -> {
            if (CollectionUtils.isNotEmpty(proof.getIdentifiers())) {
                PartnerProof savedProof = proofManager.handleAckedOrVerifiedProofEvent(proof, pp);

                WebSocketMessageBody.WebSocketMessageState state = PresentationExchangeRole.VERIFIER
                        .equals(proof.getRole()) ? WebSocketMessageBody.WebSocketMessageState.RECEIVED
                                : WebSocketMessageBody.WebSocketMessageState.SENT;
                proofManager.sendMessage(
                        state,
                        WebSocketMessageBody.WebSocketMessageType.PROOF,
                        savedProof);
            } else {
                log.warn("Proof does not contain any identifiers event will not be persisted");
            }
        });
    }

    /**
     * Handles all proof request
     * 
     * @param proof {@link PresentationExchangeRecord}
     */
    private void handleProofRequest(@NonNull PresentationExchangeRecord proof) {
        partnerRepo.findByConnectionId(proof.getConnectionId()).ifPresent(
                p -> pProofRepo.findByPresentationExchangeId(proof.getPresentationExchangeId())
                        .ifPresentOrElse(pProof -> {
                            // case: this BPA sends proof to other BPA
                            // if --auto-respond-presentation-request is set to false and there is a
                            // preceding proof proposal event we can do a auto present
                            if (PresentationExchangeState.PROPOSAL_SENT.equals(pProof.getState())
                                    && PresentationExchangeInitiator.SELF.equals(proof.getInitiator())) {
                                log.info(
                                        "Present_Proof: state=request_received on PresentationExchange where " +
                                                "initator=self, responding immediately");
                                pProofRepo.updateState(pProof.getId(), proof.getState());
                                if (proof.getAutoPresent() == null || !proof.getAutoPresent()) {
                                    proofManager.presentProof(proof);
                                }
                            }
                        }, () -> {
                            // case: proof request from other BPA
                            final PartnerProof pp = defaultProof(p.getId(), proof)
                                    .setProofRequest(proof.getPresentationRequest());
                            pProofRepo.save(pp);
                            if (proof.getAutoPresent() == null || !proof.getAutoPresent()) {
                                proofManager.sendMessage(
                                        WebSocketMessageBody.WebSocketMessageState.RECEIVED,
                                        WebSocketMessageBody.WebSocketMessageType.PROOFREQUEST,
                                        pp);
                            }
                        }));

    }

    /**
     * Handle present proof problem report message
     * 
     * @param threadId    the thread id of the exchange
     * @param description the problem description
     */
    void handleProblemReport(@NonNull String threadId, @NonNull String description) {
        pProofRepo.findByThreadId(threadId).ifPresent(pp -> pProofRepo.updateProblemReport(pp.getId(), description));
    }

    /**
     * Build db proof representation with all mandatory fields that are required
     * 
     * @param partnerId the partner id
     * @param proof     {link PresentationExchangeRecord}
     * @return {@link PartnerProof}
     */
    private PartnerProof defaultProof(@NonNull UUID partnerId, @NonNull PresentationExchangeRecord proof) {
        return PartnerProof
                .builder()
                .partnerId(partnerId)
                .state(proof.getState())
                .presentationExchangeId(proof.getPresentationExchangeId())
                .threadId(proof.getThreadId())
                .role(proof.getRole())
                .build();
    }
}
