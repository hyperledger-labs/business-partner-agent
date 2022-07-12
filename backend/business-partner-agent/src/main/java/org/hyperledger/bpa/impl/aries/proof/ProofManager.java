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

import io.micronaut.context.annotation.Value;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
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
import org.hyperledger.aries.api.jsonld.VerifiableCredential;
import org.hyperledger.aries.api.present_proof.*;
import org.hyperledger.aries.api.present_proof_v2.*;
import org.hyperledger.aries.api.schema.SchemaSendResponse.Schema;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.api.aries.AriesProofExchange;
import org.hyperledger.bpa.api.exception.*;
import org.hyperledger.bpa.api.notification.PresentationRequestDeclinedEvent;
import org.hyperledger.bpa.api.notification.PresentationRequestSentEvent;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.controller.api.partner.ApproveProofRequest;
import org.hyperledger.bpa.controller.api.partner.RequestProofRequest;
import org.hyperledger.bpa.controller.api.proof.PresentationRequestCredentialsIndy;
import org.hyperledger.bpa.impl.activity.DidResolver;
import org.hyperledger.bpa.impl.aries.credential.CredentialInfoResolver;
import org.hyperledger.bpa.impl.aries.prooftemplates.ProofTemplateConversion;
import org.hyperledger.bpa.impl.util.AriesStringUtil;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.persistence.model.BPAProofTemplate;
import org.hyperledger.bpa.persistence.model.Partner;
import org.hyperledger.bpa.persistence.model.PartnerProof;
import org.hyperledger.bpa.persistence.model.converter.ExchangePayload;
import org.hyperledger.bpa.persistence.repository.HolderCredExRepository;
import org.hyperledger.bpa.persistence.repository.PartnerProofRepository;
import org.hyperledger.bpa.persistence.repository.PartnerRepository;

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
    ProverLDManager ldProver;

    @Inject
    VerifierLDManager ldVerifier;

    @Inject
    CredentialInfoResolver credentialInfoResolver;

    @Inject
    BPAMessageSource.DefaultMessageSource ms;

    // request proof from partner via proof template
    public void sendPresentProofRequestIndy(@NonNull UUID partnerId, @NonNull @Valid BPAProofTemplate proofTemplate,
            @NonNull ExchangeVersion version) {
        try {
            PresentProofRequest proofRequest = proofTemplateConversion.proofRequestViaVisitorFrom(partnerId,
                    proofTemplate);
            // the proofTemplate does not contain the proof request Non-Revocation value, if
            // that was not part of the template and set during proof request creation.
            // using null for issuerId and schemaId because the template could have multiple
            // of each.
            if (version.isV1()) {
                ac.presentProofSendRequest(proofRequest)
                        .ifPresent(persistProof(partnerId, proofTemplate, CredentialType.INDY));
            } else {
                ac.presentProofV2SendRequest(V20PresSendRequestRequest
                        .builder()
                        .connectionId(proofRequest.getConnectionId())
                        .presentationRequest(V20PresSendRequestRequest.V20PresRequestByFormat.builder()
                                .indy(proofRequest.getProofRequest())
                                .build())
                        .build())
                        .map(V20PresExRecordToV1Converter::toV1)
                        .ifPresent(persistProof(partnerId, proofTemplate, CredentialType.INDY));
            }
        } catch (IOException e) {
            throw new NetworkException(ms.getMessage("acapy.unavailable"), e);
        }
    }

    public void sendPresentProofRequestJsonLD(@NonNull UUID partnerId, @NonNull @Valid BPAProofTemplate proofTemplate) {
        Partner p = partnerRepo.findById(partnerId).orElseThrow(EntityNotFoundException::new);
        try {
            ac.presentProofV2SendRequest(V20PresSendRequestRequest
                    .builder()
                    .connectionId(p.getConnectionId())
                    .presentationRequest(V20PresSendRequestRequest.V20PresRequestByFormat.builder()
                            .dif(ldVerifier.prepareRequest(proofTemplate))
                            .build())
                    .build())
                    .ifPresent(persistProof(partnerId, proofTemplate, CredentialType.JSON_LD));
        } catch (IOException e) {
            throw new NetworkException(ms.getMessage("acapy.unavailable"), e);
        }

    }

    // request proof from partner - currently not used by the frontend
    public void sendPresentProofRequestIndy(@NonNull UUID partnerId, @NonNull RequestProofRequest req) {
        try {
            final Partner partner = partnerRepo.findById(partnerId)
                    .orElseThrow(() -> new PartnerException(
                            ms.getMessage("api.partner.not.found", Map.of("id", partnerId))));
            if (!partner.hasConnectionId()) {
                throw new PartnerException(ms.getMessage("api.partner.no.connection"));
            }
            if (req.isRequestBySchema()) {
                String schemaId = req.getRequestBySchema().getSchemaId();
                final Schema schema = ac.schemasGetById(schemaId)
                        .orElseThrow(() -> new PartnerException(ms
                                .getMessage("api.schema.restriction.schema.not.found.on.ledger",
                                        Map.of("id", schemaId))));
                PresentProofRequest proofRequest = PresentProofRequestHelper
                        .buildForAllAttributes(partner.getConnectionId(),
                                Set.copyOf(schema.getAttrNames()), req.buildRestrictions());
                ac.presentProofSendRequest(proofRequest).ifPresent(
                        persistProof(partnerId, null, CredentialType.INDY));
            } else {
                ac.presentProofSendRequest(req.getRequestRaw().toString()).ifPresent(
                        persistProof(partnerId, null, CredentialType.INDY));
            }
        } catch (IOException e) {
            throw new NetworkException(ms.getMessage("acapy.unavailable"), e);
        }
    }

    // send presentation offer to partner based on a wallet credential
    public void sendProofProposal(@NonNull UUID partnerId, @NonNull UUID myCredentialId,
            @Nullable ExchangeVersion version) {
        partnerRepo.findById(partnerId).ifPresent(p -> holderCredExRepo.findById(myCredentialId).ifPresent(c -> {
            if (StringUtils.isNotEmpty(p.getConnectionId())) {
                throw new WrongApiUsageException(ms.getMessage("api.partner.no.connection"));
            }
            ExchangeVersion v = VersionHelper.determineVersion(version, c);
            try {
                if (c.typeIsIndy()) {
                    Credential cred = Objects.requireNonNull(c.getIndyCredential());
                    if (v.isV1()) {
                        ac.presentProofSendProposal(
                                PresentProofProposalBuilder.fromCredential(p.getConnectionId(), cred))
                                .ifPresent(persistProof(partnerId, null, CredentialType.INDY));
                    } else if (v.isV2()) {
                        ac.presentProofV2SendProposal(PresentProofProposalBuilder.v2IndyFromCredential(
                                p.getConnectionId(), cred, AriesStringUtil.schemaGetName(cred.getSchemaId())))
                                .map(V20PresExRecordToV1Converter::toV1)
                                .ifPresent(persistProof(partnerId, null, CredentialType.INDY));
                    }
                } else if (c.typeIsJsonLd()) {
                    V20PresProposalRequest proofProposal = ldProver.prepareProposal(p.getConnectionId(), c);
                    ac.presentProofV2SendProposal(proofProposal)
                            .ifPresent(persistProof(partnerId, null, CredentialType.JSON_LD));
                }
            } catch (IOException e) {
                throw new NetworkException(ms.getMessage("acapy.unavailable"), e);
            }
        }));
    }

    private Consumer<BasePresExRecord> persistProof(
            @NonNull UUID partnerId, @Nullable BPAProofTemplate proofTemplate, @NonNull CredentialType type) {
        return exchange -> {
            final PartnerProof pp = PartnerProof
                    .builder()
                    .partnerId(partnerId)
                    .state(exchange.getState())
                    .type(type)
                    .presentationExchangeId(exchange.getPresentationExchangeId())
                    .role(exchange.getRole())
                    .threadId(exchange.getThreadId())
                    .proofRequest(ExchangePayload.buildForProofRequest(exchange))
                    .proofTemplate(proofTemplate)
                    .exchangeVersion(exchange.getVersion())
                    .pushStateChange(exchange.getState(), Instant.now())
                    .build();
            pProofRepo.save(pp);
            eventPublisher.publishEventAsync(PresentationRequestSentEvent.builder()
                    .partnerProof(pp)
                    .build());
        };
    }

    // manual proof request flow
    public List<PresentationRequestCredentialsIndy> getMatchingCredentials(@NonNull UUID partnerProofId) {
        PartnerProof partnerProof = pProofRepo.findById(partnerProofId).orElseThrow(EntityNotFoundException::new);
        return getMatchingIndyCredentials(partnerProof.getPresentationExchangeId(), partnerProof.getExchangeVersion())
                .map(pres -> pres.stream().map(rec -> PresentationRequestCredentialsIndy
                        .from(rec, credentialInfoResolver.populateCredentialInfo(rec.getCredentialInfo())))
                        .collect(Collectors.toList()))
                .orElse(List.of());
    }

    // TODO aggregate the result into a model that adheres to some basic dif proof
    // exchange functionality
    public List<PresentationRequestCredentialsIndy.CredentialInfo> getMatchingLDCredentials(
            @NonNull UUID partnerProofId) {
        PartnerProof partnerProof = pProofRepo.findById(partnerProofId).orElseThrow(EntityNotFoundException::new);
        try {
            return ac.presentProofV2RecordsCredentialsDif(partnerProof.getPresentationExchangeId(), null)
                    .orElseThrow()
                    .stream()
                    .map(match -> credentialInfoResolver.populateCredentialInfo(match))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new NetworkException(ms.getMessage("acapy.unavailable"), e);
        }
    }

    private Optional<List<PresentationRequestCredentials>> getMatchingIndyCredentials(
            @NonNull String presentationExchangeId, @NonNull ExchangeVersion version) {
        try {
            Optional<List<PresentationRequestCredentials>> matches;
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
    public void declinePresentProofRequest(@NonNull UUID partnerProofId, @Nullable String message) {
        PartnerProof proofEx = pProofRepo.findById(partnerProofId).orElseThrow(EntityNotFoundException::new);
        if (proofEx.stateIsRequestReceived()) {
            try {
                if (StringUtils.isEmpty(message)) {
                    message = ms.getMessage("api.proof.exchange.declined");
                }
                proofEx.pushStates(PresentationExchangeState.DECLINED);
                pProofRepo.update(proofEx);
                sendPresentProofProblemReport(proofEx.getPresentationExchangeId(), message,
                        proofEx.getExchangeVersion());
                eventPublisher
                        .publishEventAsync(PresentationRequestDeclinedEvent.builder().partnerProof(proofEx).build());
            } catch (IOException e) {
                throw new NetworkException(ms.getMessage("acapy.unavailable"), e);
            }
        } else {
            throw new WrongApiUsageException(ms.getMessage("api.proof.exchange.wrong.state"));
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
                BasePresExRecord record;
                if (proofEx.getExchangeVersion().isV1()) {
                    record = ac.presentProofRecordsGetById(proofEx.getPresentationExchangeId()).orElseThrow();
                } else {
                    V20PresExRecord v2 = ac.presentProofV2RecordsGetById(proofEx.getPresentationExchangeId())
                            .orElseThrow();
                    if (v2.isDif()) {
                        record = v2;
                    } else {
                        record = V20PresExRecordToV1Converter.toV1(v2);
                    }
                }
                this.presentProofAcceptSelected(record, referents, proofEx.getExchangeVersion());
            } catch (IOException e) {
                throw new NetworkException(ms.getMessage("acapy.unavailable"), e);
            }
        } else {
            throw new WrongApiUsageException(ms.getMessage("api.present.proof.wrong.state"));
        }
    }

    void presentProofAcceptSelected(@NonNull BasePresExRecord presExRecord,
            @Nullable List<String> referents, @NonNull ExchangeVersion version) {
        if (presExRecord.stateIsRequestReceived()) {
            if (presExRecord instanceof PresentationExchangeRecord indy) {
                acceptSelectedIndyCredentials(referents, version, indy);
            } else if (presExRecord instanceof V20PresExRecord dif) {
                acceptSelectedDifCredentials(dif);
            }
        }
    }

    private void acceptSelectedIndyCredentials(@Nullable List<String> referents, @NonNull ExchangeVersion version,
            @NonNull PresentationExchangeRecord presentationExchangeRecord) {
        getMatchingIndyCredentials(presentationExchangeRecord.getPresentationExchangeId(), version)
                .ifPresentOrElse(creds -> {
                    if (CollectionUtils.isNotEmpty(creds)) {
                        List<PresentationRequestCredentials> selected = getPresentationRequestCredentials(
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
                        String msg = ms.getMessage("api.proof.exchange.no.match",
                                Map.of("id", presentationExchangeRecord.getPresentationExchangeId()));
                        log.warn(msg);
                        pProofRepo.findByPresentationExchangeId(
                                presentationExchangeRecord.getPresentationExchangeId())
                                .ifPresent(pp -> pProofRepo.updateProblemReport(pp.getId(), msg));
                        throw new PresentationConstructionException(msg);
                    }
                }, () -> log.error("Could not load matching credentials from aca-py"));
    }

    private void acceptSelectedDifCredentials(@NonNull V20PresExRecord dif) {
        try {
            List<VerifiableCredential.VerifiableCredentialMatch> matches = ac.presentProofV2RecordsCredentialsDif(
                    dif.getPresentationExchangeId(), null)
                    .orElseThrow();
            Optional<VerifiableCredential.VerifiableCredentialMatch> match = matches.stream()
                    .filter(m -> StringUtils.isNotEmpty(m.getIssuer()) && m.getIssuer().startsWith(didPrefix))
                    .findFirst();
            if (match.isPresent()) {

                // set holder to null
                V2DIFProofRequest.PresentationDefinition presentationDefinition = dif.resolveDifPresentationRequest()
                        .getPresentationDefinition();
                if (presentationDefinition.getInputDescriptors() != null) {
                    presentationDefinition.getInputDescriptors().forEach(id -> {
                        if (id.getConstraints() != null) {
                            id.getConstraints().setIsHolder(null);
                        }
                    });
                }
                ac.presentProofV2RecordsSendPresentation(
                        dif.getPresentationExchangeId(),
                        V20PresSpecByFormatRequest.builder()
                                .dif(DIFPresSpec.builder()
                                        // not set automatically, won't validate if not set
                                        //.issuerId(match.get().getIssuer())
                                        //.presentationDefinition(dif.resolveDifPresentationRequest().getPresentationDefinition())
                                        .presentationDefinition(presentationDefinition)
                                        .recordIds(Map.of(
                                                dif.resolveDifPresentationRequest().getPresentationDefinition()
                                                        .getInputDescriptors().get(0).getId(),
                                                List.of(match.get().getRecordId())))
                                        .build())
                                .build());
            }
        } catch (IOException e) {
            log.error(ms.getMessage("acapy.unavailable"), e);
        }
    }

    private List<PresentationRequestCredentials> getPresentationRequestCredentials(
            List<PresentationRequestCredentials> creds,
            List<String> referents) {
        if (CollectionUtils.isEmpty(referents)) {
            return creds;
        }
        return creds
                .stream()
                .filter(c -> referents.contains(c.getCredentialInfo().getReferent()))
                .collect(Collectors.toList());
    }

    PartnerProof handleAckedOrVerifiedProofEvent(@NonNull BasePresExRecord proof, @NonNull PartnerProof pp) {
        pp
                .setValid(proof.isVerified())
                .pushStates(proof.getState(), proof.getUpdatedAt());
        if (proof instanceof PresentationExchangeRecord indy) {
            Map<String, PresentationExchangeRecord.RevealedAttributeGroup> revealedAttributeGroups = indy
                    .findRevealedAttributeGroups();
            pp
                    .setProofRequest(ExchangePayload.indy(indy.getPresentationRequest()))
                    .setProof(CollectionUtils.isNotEmpty(revealedAttributeGroups)
                            ? ExchangePayload.indy(indy.findRevealedAttributeGroups())
                            : ExchangePayload.indy(
                                    conv.revealedAttrsToGroup(indy.findRevealedAttributedFull(),
                                            indy.getIdentifiers())));
            didRes.resolveDid(pp, indy.getIdentifiers());
        } else if (proof instanceof V20PresExRecord dif) {
            pp
                    .setProofRequest(ExchangePayload.jsonLD(dif.resolveDifPresentationRequest()))
                    .setProof(ExchangePayload.jsonLD(dif.resolveDifPresentation()));
        }
        return pProofRepo.update(pp);
    }

    void handleVerifierPresentationReceived(@NonNull ExchangeVersion version, @NonNull String presentationExchangeId) {
        try {
            if (version.isV1()) {
                ac.presentProofRecordsVerifyPresentation(presentationExchangeId);
            } else {
                ac.presentProofV2RecordsVerifyPresentation(presentationExchangeId);
            }
        } catch (IOException e) {
            log.error(ms.getMessage("acapy.unavailable"), e);
        }
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
                    log.warn("ACA-py PresentationExchange not found, BPA Partner Proof will still be deleted");
                } else {
                    throw e;
                }
            }
            pProofRepo.deleteById(id);
        });
    }
}
