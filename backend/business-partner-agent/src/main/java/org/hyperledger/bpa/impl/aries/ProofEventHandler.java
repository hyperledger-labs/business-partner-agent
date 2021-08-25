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

import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.core.util.CollectionUtils;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.api.present_proof.PresentationExchangeInitiator;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord;
import org.hyperledger.aries.api.present_proof.PresentationExchangeState;
import org.hyperledger.bpa.impl.MessageService;
import org.hyperledger.bpa.impl.notification.PresentationRequestCompletedEvent;
import org.hyperledger.bpa.impl.notification.PresentationRequestDeclinedEvent;
import org.hyperledger.bpa.impl.notification.PresentationRequestReceivedEvent;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.model.PartnerProof;
import org.hyperledger.bpa.repository.PartnerProofRepository;
import org.hyperledger.bpa.repository.PartnerRepository;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Instant;
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

    @Inject
    Converter conv;

    @Inject
    ApplicationEventPublisher eventPublisher;

    void dispatch(PresentationExchangeRecord proof) {
        if (proof.isVerified() && proof.roleIsVerifier() || proof.roleIsProverAndPresentationAcked()) {
            handleAckedOrVerified(proof);
        } else if (proof.roleIsProverAndRequestReceived()) {
            handleProofRequest(proof);
        } else {
            // if not handled in the manager e.g. when sending the request
            if (!proof.roleIsProverAndPresentationSent() && !proof.roleIsVerifierAndRequestSent()) {
                handleAll(proof);
            }
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
                                pp -> {
                                    if (proof.getState() != null) {
                                        pp.setState(proof.getState());
                                        pp.pushStateChange(proof.getState(), Instant.now());
                                        pProofRepo.update(pp);
                                    }
                                    if (proof.getErrorMsg() != null
                                            && PresentationExchangeState.DECLINED.equals(proof.getState())) {
                                        pProofRepo.updateProblemReport(pp.getId(), proof.getErrorMsg());
                                        eventPublisher.publishEventAsync(
                                                PresentationRequestDeclinedEvent.builder().partnerProof(pp).build());
                                    }
                                },
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
                eventPublisher.publishEventAsync(PresentationRequestCompletedEvent.builder()
                        .partnerProof(savedProof)
                        .build());
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
                            // preceding proof proposal event we can do an auto present
                            if (PresentationExchangeState.PROPOSAL_SENT.equals(pProof.getState())
                                    && PresentationExchangeInitiator.SELF.equals(proof.getInitiator())) {
                                log.info(
                                        "Present_Proof: state=request_received on PresentationExchange where " +
                                                "initator=self, responding immediately");
                                pProof.setState(proof.getState());
                                pProof.pushStateChange(proof.getState(), Instant.now());
                                pProofRepo.update(pProof);
                                if (proof.getAutoPresent() == null || !proof.getAutoPresent()) {
                                    proofManager.presentProofAcceptAll(proof);
                                }
                            }
                        }, () -> {
                            // case: proof request from other BPA
                            final PartnerProof pp = defaultProof(p.getId(), proof)
                                    .setProofRequest(proof.getPresentationRequest());
                            pProofRepo.save(pp);
                            eventPublisher.publishEventAsync(PresentationRequestReceivedEvent.builder()
                                    .partnerProof(pp)
                                    .build());

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
                .pushStateChange(proof.getState(), Instant.now())
                .build();
    }
}
