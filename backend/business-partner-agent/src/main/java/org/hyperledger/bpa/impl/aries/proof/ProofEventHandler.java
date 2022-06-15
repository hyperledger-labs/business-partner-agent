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
package org.hyperledger.bpa.impl.aries.proof;

import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.api.present_proof.BasePresExRecord;
import org.hyperledger.aries.api.present_proof.PresentProofRequest;
import org.hyperledger.aries.api.present_proof.PresentationExchangeState;
import org.hyperledger.aries.api.present_proof_v2.V20PresExRecord;
import org.hyperledger.aries.api.present_proof_v2.V2DIFProofRequest;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.api.notification.PresentationRequestCompletedEvent;
import org.hyperledger.bpa.api.notification.PresentationRequestDeclinedEvent;
import org.hyperledger.bpa.api.notification.PresentationRequestReceivedEvent;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.impl.util.TimeUtil;
import org.hyperledger.bpa.persistence.model.PartnerProof;
import org.hyperledger.bpa.persistence.model.converter.ExchangePayload;
import org.hyperledger.bpa.persistence.repository.PartnerProofRepository;
import org.hyperledger.bpa.persistence.repository.PartnerRepository;

import java.time.Instant;
import java.util.Objects;
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
    ApplicationEventPublisher eventPublisher;

    @Inject
    BPAMessageSource.DefaultMessageSource msg;

    public void dispatch(BasePresExRecord proof) {
        // TODO separate into holder and verifier, order by state
        if (proof.roleIsVerifierAndStateIsVerifiedOrDone() || proof.roleIsProverAndStateIsPresentationAckedOrDone()) {
            handleAckedOrVerified(proof);
        } else if (proof.roleIsProverAndRequestReceived()) {
            handleProofRequest(proof);
        } else if (proof.roleIsVerifier() && proof.stateIsPresentationReceived()) {
            proofManager.handleVerifierPresentationReceived(proof.getVersion(), proof.getPresentationExchangeId());
            handleAll(proof);
        } else if (StringUtils.isNotEmpty(proof.getErrorMsg())) {
            handleProblemReport(proof);
        } else {
            // if not handled in the manager e.g. when sending the request
            if (!proof.roleIsProverAndProposalSent()
                    && !(proof.roleIsVerifierAndRequestSent() && proof.initiatorIsSelf())) {
                handleAll(proof);
            }
        }
    }

    /**
     * Default presentation exchange event handler that either stores or updates
     * partner proofs
     *
     * @param exchange {@link BasePresExRecord}
     */
    private void handleAll(BasePresExRecord exchange) {
        pProofRepo.findByPresentationExchangeId(exchange.getPresentationExchangeId()).ifPresentOrElse(
                pp -> {
                    if (exchange.getState() != null) {
                        pp.pushStates(exchange.getState(), exchange.getUpdatedAt());
                        pProofRepo.update(pp);
                    }
                },
                () -> partnerRepo.findByConnectionId(exchange.getConnectionId())
                        .ifPresentOrElse(
                                p -> pProofRepo.save(defaultProof(p.getId(), exchange)),
                                () -> log.warn("Received exchange event that does not match any connection")));
    }

    /**
     * Handles all events that are either acked or verified connectionless proofs
     * are currently not handled
     *
     * @param p {@link BasePresExRecord}
     */
    private void handleAckedOrVerified(BasePresExRecord p) {
        pProofRepo.findByPresentationExchangeId(p.getPresentationExchangeId()).ifPresent(pp -> {
            PartnerProof savedProof = proofManager.handleAckedOrVerifiedProofEvent(p, pp);
            eventPublisher.publishEventAsync(PresentationRequestCompletedEvent.builder()
                    .partnerProof(savedProof)
                    .build());
        });
    }

    /**
     * Handles all proof request
     *
     * @param proof {@link BasePresExRecord}
     */
    private void handleProofRequest(@NonNull BasePresExRecord proof) {
        partnerRepo.findByConnectionId(proof.getConnectionId()).ifPresent(
                p -> pProofRepo.findByPresentationExchangeId(proof.getPresentationExchangeId())
                        .ifPresentOrElse(pProof -> {
                            // case: this BPA sends proof to other BPA
                            // if --auto-respond-presentation-request is set to false and there is a
                            // preceding proof proposal event we can do an auto present
                            if (pProof.stateIsProposalSent() && proof.initiatorIsSelf()) {
                                log.info(
                                        "Present_Proof: state=request_received on PresentationExchange where " +
                                                "initiator=self, responding immediately");
                                pProof.pushStates(proof.getState(), proof.getUpdatedAt());
                                if (proof instanceof V20PresExRecord dif) {
                                    pProof.setProofRequest(ExchangePayload.buildForProofRequest(dif));
                                }
                                pProofRepo.update(pProof);
                                if (proof.getAutoPresent() == null || !proof.getAutoPresent()) {
                                    proofManager.presentProofAcceptSelected(proof, null, pProof.getExchangeVersion());
                                }
                            }
                        }, () -> {
                            // case: proof request from other BPA
                            final PartnerProof pp = defaultProof(p.getId(), proof);
                            pProofRepo.save(pp);
                            eventPublisher.publishEventAsync(PresentationRequestReceivedEvent.builder()
                                    .partnerProof(pp)
                                    .build());

                        }));

    }

    /**
     * Handle present proof problem report event message
     *
     * @param exchange {@link BasePresExRecord}
     */
    private void handleProblemReport(@NonNull BasePresExRecord exchange) {
        pProofRepo.findByPresentationExchangeId(exchange.getPresentationExchangeId()).ifPresent(pp -> {
            String errorMsg = org.apache.commons.lang3.StringUtils.truncate(exchange.getErrorMsg(), 255);
            // not a useful response, but this is what we get and what it means
            if ("abandoned: abandoned".equals(errorMsg)) {
                errorMsg = msg.getMessage("api.proof.exchange.abandoned");
            }
            pp.pushStates(PresentationExchangeState.DECLINED, exchange.getUpdatedAt());
            pp.setProblemReport(errorMsg);
            pProofRepo.update(pp);
            eventPublisher.publishEventAsync(
                    PresentationRequestDeclinedEvent.builder().partnerProof(pp).build());
        });
    }

    /**
     * Build db proof representation with all mandatory fields that are required
     *
     * @param partnerId the partner id
     * @param proof     {@link BasePresExRecord}
     * @return {@link PartnerProof}
     */
    private PartnerProof defaultProof(@NonNull UUID partnerId, @NonNull BasePresExRecord proof) {
        Instant ts = TimeUtil.fromISOInstant(proof.getUpdatedAt());
        ExchangePayload<PresentProofRequest.ProofRequest, V2DIFProofRequest> pr = ExchangePayload
                .buildForProofRequest(proof);
        CredentialType type = Objects.requireNonNull(pr).getType();
        return PartnerProof
                .builder()
                .partnerId(partnerId)
                .state(proof.getState())
                .presentationExchangeId(proof.getPresentationExchangeId())
                .threadId(proof.getThreadId())
                .role(proof.getRole())
                .proofRequest(pr)
                .type(type)
                .exchangeVersion(proof.getVersion())
                .pushStateChange(proof.getState(), ts != null ? ts : Instant.now())
                .build();
    }
}
