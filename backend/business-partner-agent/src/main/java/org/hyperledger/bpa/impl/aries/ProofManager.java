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
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.acy_py.generated.model.V10PresentationProblemReportRequest;
import org.hyperledger.acy_py.generated.model.V20PresProblemReportRequest;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.ExchangeVersion;
import org.hyperledger.aries.api.credentials.Credential;
import org.hyperledger.aries.api.exception.AriesException;
import org.hyperledger.aries.api.present_proof.*;
import org.hyperledger.aries.api.present_proof_v2.V20PresExRecordToV1Converter;
import org.hyperledger.aries.api.present_proof_v2.V20PresSendRequestRequest;
import org.hyperledger.aries.api.present_proof_v2.V20PresSpecByFormatRequest;
import org.hyperledger.aries.api.schema.SchemaSendResponse.Schema;
import org.hyperledger.bpa.api.aries.AriesProofExchange;
import org.hyperledger.bpa.api.exception.*;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.controller.api.partner.ApproveProofRequest;
import org.hyperledger.bpa.controller.api.partner.RequestProofRequest;
import org.hyperledger.bpa.controller.api.proof.PresentationRequestCredentials;
import org.hyperledger.bpa.impl.activity.DidResolver;
import org.hyperledger.bpa.impl.notification.PresentationRequestDeclinedEvent;
import org.hyperledger.bpa.impl.notification.PresentationRequestSentEvent;
import org.hyperledger.bpa.impl.prooftemplates.ProofTemplateConversion;
import org.hyperledger.bpa.impl.util.AriesStringUtil;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.model.BPAProofTemplate;
import org.hyperledger.bpa.model.Partner;
import org.hyperledger.bpa.model.PartnerProof;
import org.hyperledger.bpa.repository.HolderCredExRepository;
import org.hyperledger.bpa.repository.PartnerProofRepository;
import org.hyperledger.bpa.repository.PartnerRepository;

import javax.validation.Valid;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class ProofManager {

    @Value("${bpa.did.prefix}")
    String didPrefix;

    @Inject
    AriesClient ac;

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    PartnerProofRepository pProofRepo;

    @Inject
    HolderCredExRepository holderCredExRepo;

    @Inject
    Converter conv;

    @Inject
    DidResolver didRes;

    @Inject
    ApplicationEventPublisher eventPublisher;

    @Inject
    ProofTemplateConversion proofTemplateConversion;

    @Inject
    CredentialInfoResolver credentialInfoResolver;

    @Inject
    BPAMessageSource.DefaultMessageSource ms;

    // request proof from partner via proof template with exchange version 1
    public void sendPresentProofRequest(@NonNull UUID partnerId, @NonNull @Valid BPAProofTemplate proofTemplate) {
        sendPresentProofRequest(partnerId, proofTemplate, ExchangeVersion.V1);
    }

    // request proof from partner via proof template
    public void sendPresentProofRequest(@NonNull UUID partnerId, @NonNull @Valid BPAProofTemplate proofTemplate,
            @NonNull ExchangeVersion version) {
        try {
            PresentProofRequest proofRequest = proofTemplateConversion.proofRequestViaVisitorFrom(partnerId,
                    proofTemplate);
            // the proofTemplate does not contain the proof request Non-Revocation value, if
            // that was not part of the template and set during proof request creation.
            // using null for issuerId and schemaId because the template could have multiple
            // of each.
            if (version.isV1()) {
                ac.presentProofSendRequest(proofRequest).ifPresent(persistProof(partnerId, proofTemplate));
            } else {
                ac.presentProofV2SendRequest(V20PresSendRequestRequest
                        .builder()
                        .connectionId(proofRequest.getConnectionId())
                        .presentationRequest(V20PresSendRequestRequest.V20PresRequestByFormat.builder()
                                .indy(proofRequest.getProofRequest())
                                .build())
                        .build())
                        .map(V20PresExRecordToV1Converter::toV1)
                        .ifPresent(persistProof(partnerId, proofTemplate));
            }
        } catch (IOException e) {
            throw new NetworkException(ms.getMessage("acapy.unavailable"), e);
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
            throw new NetworkException(ms.getMessage("acapy.unavailable"), e);
        }
    }

    // send presentation offer to partner based on a wallet credential
    public void sendProofProposal(@NonNull UUID partnerId, @NonNull UUID myCredentialId,
            @Nullable ExchangeVersion version) {
        partnerRepo.findById(partnerId).ifPresent(p -> holderCredExRepo.findById(myCredentialId).ifPresent(c -> {
            ExchangeVersion v = version != null ? version : ExchangeVersion.V1;
            Credential cred = Objects.requireNonNull(c.getCredential());
            try {
                if (v.isV1()) {
                    ac.presentProofSendProposal(PresentProofProposalBuilder.fromCredential(p.getConnectionId(), cred))
                            .ifPresent(persistProof(partnerId, null));
                } else {
                    ac.presentProofV2SendProposal(PresentProofProposalBuilder.v2IndyFromCredential(
                            p.getConnectionId(), cred, AriesStringUtil.schemaGetName(cred.getSchemaId())))
                            .map(V20PresExRecordToV1Converter::toV1)
                            .ifPresent(persistProof(partnerId, null));
                }
            } catch (IOException e) {
                throw new NetworkException(ms.getMessage("acapy.unavailable"), e);
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
                    .exchangeVersion(exchange.getVersion() != null ? exchange.getVersion() : ExchangeVersion.V1)
                    .pushStateChange(exchange.getState(), Instant.now())
                    .build();
            pProofRepo.save(pp);
            eventPublisher.publishEventAsync(PresentationRequestSentEvent.builder()
                    .partnerProof(pp)
                    .build());
        };
    }

    // manual proof request flow
    public List<PresentationRequestCredentials> getMatchingCredentials(@NonNull UUID partnerProofId) {
        PartnerProof partnerProof = pProofRepo.findById(partnerProofId).orElseThrow(EntityNotFoundException::new);
        return getMatchingCredentials(partnerProof.getPresentationExchangeId(), partnerProof.getExchangeVersion())
                .map(pres -> pres.stream().map(rec -> PresentationRequestCredentials
                        .from(rec, credentialInfoResolver.populateCredentialInfo(rec.getCredentialInfo())))
                        .collect(Collectors.toList()))
                .orElse(List.of());
    }

    private Optional<List<org.hyperledger.aries.api.present_proof.PresentationRequestCredentials>> getMatchingCredentials(
            @NonNull String presentationExchangeId, @NonNull ExchangeVersion version) {
        try {
            Optional<List<org.hyperledger.aries.api.present_proof.PresentationRequestCredentials>> matches;
            if (version.isV1()) {
                matches = ac.presentProofRecordsCredentials(presentationExchangeId);
            } else {
                matches = ac.presentProofV2RecordsCredentials(presentationExchangeId, null);
            }
            return matches;
        } catch (IOException e) {
            throw new NetworkException(ms.getMessage("acapy.unavailable"), e);
        } catch (AriesException | NoSuchElementException e) {
            log.warn("No matching credentials found");
        }
        return Optional.of(List.of());
    }

    // manual proof request flow
    public void declinePresentProofRequest(@NonNull UUID partnerProofId, String explainString) {
        PartnerProof proofEx = pProofRepo.findById(partnerProofId).orElseThrow(EntityNotFoundException::new);
        if (proofEx.stateIsRequestReceived()) {
            try {
                proofEx.pushStates(PresentationExchangeState.DECLINED);
                pProofRepo.update(proofEx);
                sendPresentProofProblemReport(proofEx.getPresentationExchangeId(), explainString,
                        proofEx.getExchangeVersion());
                eventPublisher
                        .publishEventAsync(PresentationRequestDeclinedEvent.builder().partnerProof(proofEx).build());
            } catch (IOException e) {
                throw new NetworkException(ms.getMessage("acapy.unavailable"), e);
            }
        } else {
            throw new WrongApiUsageException("PresentationExchangeState != 'request-received'");
        }
    }

    // manual proof request flow
    public void presentProof(@NonNull UUID partnerProofId, @Nullable ApproveProofRequest req) {
        PartnerProof proofEx = pProofRepo.findById(partnerProofId).orElseThrow(EntityNotFoundException::new);
        if (proofEx.roleIsProverAndRequestReceived()) {
            try {
                List<String> referents = (req == null) ? null : req.getReferents();
                // find all the matching credentials using the (optionally) provided referent
                // data
                Optional<PresentationExchangeRecord> record;
                if (proofEx.getExchangeVersion().isV1()) {
                    record = ac.presentProofRecordsGetById(proofEx.getPresentationExchangeId());
                } else {
                    record = ac.presentProofV2RecordsGetById(proofEx.getPresentationExchangeId())
                            .map(V20PresExRecordToV1Converter::toV1);
                }
                record.ifPresent(per -> this.presentProofAcceptSelected(per, referents, proofEx.getExchangeVersion()));
            } catch (IOException e) {
                throw new NetworkException(ms.getMessage("acapy.unavailable"), e);
            }
        } else {
            throw new WrongApiUsageException(
                    "PresentationExchangeRole!= 'prover' or PresentationExchangeState != 'request-received'");
        }
    }

    void presentProofAcceptSelected(@NonNull PresentationExchangeRecord presentationExchangeRecord,
            @Nullable List<String> referents, @NonNull ExchangeVersion version) {
        if (presentationExchangeRecord.stateIsRequestReceived()) {
            getMatchingCredentials(presentationExchangeRecord.getPresentationExchangeId(), version)
                    .ifPresentOrElse(creds -> {
                        if (CollectionUtils.isNotEmpty(creds)) {
                            List<org.hyperledger.aries.api.present_proof.PresentationRequestCredentials> selected = getPresentationRequestCredentials(
                                    creds, referents);
                            PresentationRequestBuilder.acceptAll(presentationExchangeRecord, selected)
                                    .ifPresent(pr -> {
                                        try {
                                            if (version.isV1()) {
                                                ac.presentProofRecordsSendPresentation(
                                                        presentationExchangeRecord.getPresentationExchangeId(),
                                                        pr);

                                            } else {
                                                ac.presentProofV2RecordsSendPresentation(
                                                        presentationExchangeRecord.getPresentationExchangeId(),
                                                        V20PresSpecByFormatRequest.builder()
                                                                .indy(pr)
                                                                .build());
                                            }
                                        } catch (IOException e) {
                                            log.error(ms.getMessage("acapy.unavailable"), e);
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
        }
    }

    private List<org.hyperledger.aries.api.present_proof.PresentationRequestCredentials> getPresentationRequestCredentials(
            List<org.hyperledger.aries.api.present_proof.PresentationRequestCredentials> creds,
            List<String> referents) {
        if (CollectionUtils.isEmpty(referents)) {
            return creds;
        }
        return creds
                .stream()
                .filter(c -> referents.contains(c.getCredentialInfo().getReferent()))
                .collect(Collectors.toList());
    }

    PartnerProof handleAckedOrVerifiedProofEvent(@NonNull PresentationExchangeRecord proof, @NonNull PartnerProof pp) {
        Map<String, PresentationExchangeRecord.RevealedAttributeGroup> revealedAttributeGroups = proof
                .findRevealedAttributeGroups();
        pp
                .setValid(proof.isVerified())
                .pushStates(proof.getState(), proof.getUpdatedAt())
                .setProofRequest(proof.getPresentationRequest())
                .setProof(CollectionUtils.isNotEmpty(revealedAttributeGroups)
                        ? conv.toMap(proof.findRevealedAttributeGroups())
                        : proof.findRevealedAttributes());
        final PartnerProof savedProof = pProofRepo.update(pp);
        didRes.resolveDid(savedProof, proof.getIdentifiers());
        return savedProof;
    }

    private void sendPresentProofProblemReport(@NonNull String presentationExchangeId,
            @NonNull String problemString,
            @Nullable ExchangeVersion version) throws IOException {
        if (version == null || version.isV1()) {
            ac.presentProofRecordsProblemReport(presentationExchangeId, V10PresentationProblemReportRequest.builder()
                    .description(problemString)
                    .build());
        } else {
            ac.presentProofV2RecordsProblemReport(presentationExchangeId, V20PresProblemReportRequest.builder()
                    .description(problemString)
                    .build());
        }
    }

    // CRUD methods

    public List<AriesProofExchange> listPartnerProofs(@NonNull UUID partnerId) {
        return pProofRepo.findByPartnerIdOrderByRole(partnerId).stream()
                .map(conv::toAPIObject)
                .collect(Collectors.toList());
    }

    public AriesProofExchange getPartnerProofById(@NonNull UUID id) {
        return pProofRepo.findById(id).map(conv::toAPIObject).orElseThrow(EntityNotFoundException::new);
    }

    public void deletePartnerProof(@NonNull UUID id) {
        pProofRepo.findById(id).ifPresent(pp -> {
            try {
                if (pp.getExchangeVersion().isV1()) {
                    ac.presentProofRecordsRemove(pp.getPresentationExchangeId());
                } else {
                    ac.presentProofV2RecordsRemove(pp.getPresentationExchangeId());
                }
            } catch (IOException e) {
                log.error(ms.getMessage("acapy.unavailable"), e);
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
