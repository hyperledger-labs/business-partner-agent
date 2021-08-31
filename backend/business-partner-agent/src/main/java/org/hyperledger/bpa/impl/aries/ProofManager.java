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

import io.micronaut.context.annotation.Value;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.CollectionUtils;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.acy_py.generated.model.V10PresentationProblemReportRequest;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.credentials.Credential;
import org.hyperledger.aries.api.exception.AriesException;
import org.hyperledger.aries.api.present_proof.*;
import org.hyperledger.aries.api.schema.SchemaSendResponse.Schema;
import org.hyperledger.bpa.api.aries.AriesProofExchange;
import org.hyperledger.bpa.api.aries.ExchangeVersion;
import org.hyperledger.bpa.api.exception.NetworkException;
import org.hyperledger.bpa.api.exception.PartnerException;
import org.hyperledger.bpa.api.exception.PresentationConstructionException;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.controller.api.partner.RequestProofRequest;
import org.hyperledger.bpa.controller.api.proof.PresentationRequestCredentials;
import org.hyperledger.bpa.impl.MessageService;
import org.hyperledger.bpa.impl.activity.DidResolver;
import org.hyperledger.bpa.impl.aries.config.SchemaService;
import org.hyperledger.bpa.impl.notification.PresentationRequestDeclinedEvent;
import org.hyperledger.bpa.impl.notification.PresentationRequestSentEvent;
import org.hyperledger.bpa.impl.prooftemplates.ProofTemplateConversion;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.model.BPAProofTemplate;
import org.hyperledger.bpa.model.Partner;
import org.hyperledger.bpa.model.PartnerProof;
import org.hyperledger.bpa.repository.MyCredentialRepository;
import org.hyperledger.bpa.repository.PartnerProofRepository;
import org.hyperledger.bpa.repository.PartnerRepository;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class ProofManager {

    private static final String ACA_PY_ERROR_MSG = "aca-py not available";

    @Value("${bpa.did.prefix}")
    String didPrefix;

    @Inject
    AriesClient ac;

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    PartnerProofRepository pProofRepo;

    @Inject
    MyCredentialRepository credRepo;

    @Inject
    Converter conv;

    @Inject
    SchemaService schemaService;

    @Inject
    DidResolver didRes;

    @Inject
    MessageService messageService;

    @Inject
    ApplicationEventPublisher eventPublisher;

    @Inject
    ProofTemplateConversion proofTemplateConversion;

    @Inject
    CredentialInfoResolver credentialInfoResolver;

    // request proof from partner via proof template
    public void sendPresentProofRequest(@NonNull UUID partnerId, @NonNull @Valid BPAProofTemplate proofTemplate) {
        try {
            PresentProofRequest proofRequest = proofTemplateConversion.proofRequestViaVisitorFrom(partnerId,
                    proofTemplate);
            // the proofTemplate does not contain the proof request Non-Revocation value, if
            // that was not part of the template and set during proof request creation.
            ac.presentProofSendRequest(proofRequest).ifPresent(
                    // using null for issuerId and schemaId because the template could have multiple
                    // of each.
                    persistProof(partnerId, proofTemplate));
        } catch (IOException e) {
            throw new NetworkException(ACA_PY_ERROR_MSG, e);
        }
    }

    // request proof from partner
    public void sendPresentProofRequest(@NonNull UUID partnerId, @NonNull RequestProofRequest req) {
        try {
            final Partner partner = partnerRepo.findById(partnerId)
                    .orElseThrow(() -> new PartnerException("Partner not found"));
            if (!partner.hasConnectionId()) {
                throw new PartnerException("Partner has no aca-py connection");
            }
            if (req.isRequestBySchema()) {
                String schemaId = req.getRequestBySchema().getSchemaId();
                final Schema schema = ac.schemasGetById(schemaId)
                        .orElseThrow(() -> new PartnerException(
                                "Could not find any schema on the ledger for id: " + schemaId));
                PresentProofRequest proofRequest = PresentProofRequestHelper
                        .buildForAllAttributes(partner.getConnectionId(),
                                schema.getAttrNames(), req.buildRestrictions());
                ac.presentProofSendRequest(proofRequest).ifPresent(
                        persistProof(partnerId, null));
            } else {
                ac.presentProofSendRequest(req.getRequestRaw().toString()).ifPresent(
                        persistProof(partnerId, null));
            }
        } catch (IOException e) {
            throw new NetworkException(ACA_PY_ERROR_MSG, e);
        }
    }

    // send presentation offer to partner based on a wallet credential
    public void sendProofProposal(@NonNull UUID partnerId, @NonNull UUID myCredentialId) {
        partnerRepo.findById(partnerId).ifPresent(p -> credRepo.findById(myCredentialId).ifPresent(c -> {
            Credential cred = conv.fromMap(Objects.requireNonNull(c.getCredential()), Credential.class);
            final PresentProofProposal req = PresentProofProposalBuilder.fromCredential(p.getConnectionId(), cred);
            try {
                ac.presentProofSendProposal(req).ifPresent(persistProof(partnerId, null));
            } catch (IOException e) {
                throw new NetworkException(ACA_PY_ERROR_MSG, e);
            }
        }));
    }

    private Consumer<PresentationExchangeRecord> persistProof(
            @NonNull UUID partnerId, @Nullable BPAProofTemplate proofTemplate) {
        return exchange -> {
            final PartnerProof pp = PartnerProof
                    .builder()
                    .partnerId(partnerId)
                    .state(exchange.getState())
                    .presentationExchangeId(exchange.getPresentationExchangeId())
                    .role(exchange.getRole())
                    .threadId(exchange.getThreadId())
                    .proofRequest(exchange.getPresentationRequest())
                    .proofTemplate(proofTemplate)
                    .exchangeVersion(ExchangeVersion.V1)
                    .pushStateChange(exchange.getState(), Instant.now())
                    .build();
            pProofRepo.save(pp);
            eventPublisher.publishEventAsync(PresentationRequestSentEvent.builder()
                    .partnerProof(pp)
                    .build());
        };
    }

    // manual proof request flow
    public Optional<List<PresentationRequestCredentials>> getMatchingCredentials(@NonNull UUID partnerProofId) {
        Optional<PartnerProof> partnerProof = pProofRepo.findById(partnerProofId);
        if (partnerProof.isPresent()) {
            try {
                return ac.presentProofRecordsCredentials(partnerProof.get().getPresentationExchangeId())
                        .map(pres -> pres.stream().map(rec -> PresentationRequestCredentials
                                .from(rec, credentialInfoResolver.populateCredentialInfo(rec.getCredentialInfo())))
                                .collect(Collectors.toList()));
            } catch (IOException e) {
                throw new NetworkException(ACA_PY_ERROR_MSG, e);
            }
        }
        return Optional.empty();
    }

    // manual proof request flow
    public void declinePresentProofRequest(@NotNull PartnerProof proofEx, String explainString) {
        if (PresentationExchangeState.REQUEST_RECEIVED.equals(proofEx.getState())) {
            try {
                proofEx.pushStateChange(PresentationExchangeState.DECLINED, Instant.now());
                proofEx.setState(PresentationExchangeState.DECLINED);
                pProofRepo.update(proofEx);
                sendPresentProofProblemReport(proofEx.getPresentationExchangeId(), explainString);
                eventPublisher
                        .publishEventAsync(PresentationRequestDeclinedEvent.builder().partnerProof(proofEx).build());
            } catch (IOException e) {
                throw new NetworkException(ACA_PY_ERROR_MSG, e);
            }
        } else {
            throw new WrongApiUsageException("PresentationExchangeState != 'request-received'");
        }
    }

    // manual proof request flow
    public void presentProof(@NotNull PartnerProof proofEx, @Nullable PresentationRequest req) {
        if (PresentationExchangeRole.PROVER.equals(proofEx.getRole())
                && PresentationExchangeState.REQUEST_RECEIVED.equals(proofEx.getState())) {
            try {
                if (req == null) {
                    ac.presentProofRecordsGetById(proofEx.getPresentationExchangeId())
                            .ifPresent(this::presentProofAcceptAll);
                } else {
                    ac.presentProofRecordsSendPresentation(proofEx.getPresentationExchangeId(), req);
                }
            } catch (IOException e) {
                throw new NetworkException(ACA_PY_ERROR_MSG, e);
            }
        } else {
            throw new WrongApiUsageException(
                    "PresentationExchangeRole!= 'prover' or PresentationExchangeState != 'request-received'");
        }
    }

    // manual proof request flow, internal accept all
    void presentProofAcceptAll(@NonNull PresentationExchangeRecord presentationExchangeRecord) {
        if (PresentationExchangeState.REQUEST_RECEIVED.equals(presentationExchangeRecord.getState())) {
            try {
                ac.presentProofRecordsCredentials(presentationExchangeRecord.getPresentationExchangeId())
                        .ifPresentOrElse(creds -> {
                            if (CollectionUtils.isNotEmpty(creds)) {
                                PresentationRequestBuilder.acceptAll(presentationExchangeRecord, creds)
                                        .ifPresent(pr -> {
                                            try {
                                                ac.presentProofRecordsSendPresentation(
                                                        presentationExchangeRecord.getPresentationExchangeId(),
                                                        pr);
                                            } catch (IOException e) {
                                                log.error(ACA_PY_ERROR_MSG, e);
                                            }
                                        });
                            } else {
                                String msg = "No matching credentials found for proof request: "
                                        + presentationExchangeRecord.getPresentationExchangeId();
                                log.warn(msg);
                                pProofRepo.findByPresentationExchangeId(
                                        presentationExchangeRecord.getPresentationExchangeId())
                                        .ifPresent(pp -> pProofRepo.updateProblemReport(pp.getId(), msg));
                                throw new PresentationConstructionException(msg);
                            }
                        }, () -> log.error("Could not load matching credentials from aca-py"));
            } catch (IOException e) {
                throw new NetworkException(ACA_PY_ERROR_MSG, e);
            }
        }
    }

    PartnerProof handleAckedOrVerifiedProofEvent(@NonNull PresentationExchangeRecord proof, @NonNull PartnerProof pp) {
        Map<String, PresentationExchangeRecord.RevealedAttributeGroup> revealedAttributeGroups = proof
                .findRevealedAttributeGroups();
        pp
                .setValid(proof.isVerified())
                .setState(proof.getState())
                .pushStateChange(proof.getState(), Instant.now())
                .setProofRequest(proof.getPresentationRequest())
                .setProof(CollectionUtils.isNotEmpty(revealedAttributeGroups)
                        ? conv.toMap(proof.findRevealedAttributeGroups())
                        : proof.findRevealedAttributes());
        final PartnerProof savedProof = pProofRepo.update(pp);
        didRes.resolveDid(savedProof, proof.getIdentifiers());
        return savedProof;
    }

    private void sendPresentProofProblemReport(@NonNull String PresentationExchangeId, @NonNull String problemString)
            throws IOException {
        V10PresentationProblemReportRequest request = V10PresentationProblemReportRequest.builder()
                .description(problemString)
                .build();
        ac.presentProofRecordsProblemReport(PresentationExchangeId, request);
    }

    // CRUD methods

    public List<AriesProofExchange> listPartnerProofs(@NonNull UUID partnerId) {
        return pProofRepo.findByPartnerIdOrderByRole(partnerId).stream()
                .map(conv::toAPIObject)
                .collect(Collectors.toList());
    }

    public Optional<AriesProofExchange> getPartnerProofById(@NonNull UUID id) {
        return pProofRepo.findById(id).map(conv::toAPIObject);
    }

    public void deletePartnerProof(@NonNull UUID id) {
        pProofRepo.findById(id).ifPresent(pp -> {
            try {
                ac.presentProofRecordsRemove(pp.getPresentationExchangeId());
            } catch (IOException e) {
                log.error(ACA_PY_ERROR_MSG, e);
            } catch (AriesException e) {
                if (e.getCode() == 404) {
                    log.warn("ACA-py PresentationExchange not found, still deleting BPA Partner Proof");
                } else {
                    throw e;
                }
            }
            pProofRepo.deleteById(id);
        });
    }
}
