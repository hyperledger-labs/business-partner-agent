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
package org.hyperledger.bpa.impl.aries.credential;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.core.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.acy_py.generated.model.V20CredRequestRequest;
import org.hyperledger.aries.api.ExchangeVersion;
import org.hyperledger.aries.api.credentials.CredentialAttributes;
import org.hyperledger.aries.api.credentials.CredentialPreview;
import org.hyperledger.aries.api.exception.AriesException;
import org.hyperledger.aries.api.issue_credential_v1.BaseCredExRecord;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialProposalRequest;
import org.hyperledger.aries.api.issue_credential_v2.V1ToV2IssueCredentialConverter;
import org.hyperledger.aries.api.issue_credential_v2.V2CredentialExchangeFree;
import org.hyperledger.aries.api.issue_credential_v2.V2ToV1IndyCredentialConverter;
import org.hyperledger.aries.api.jsonld.VerifiableCredential;
import org.hyperledger.aries.api.jsonld.VerifiablePresentation;
import org.hyperledger.aries.config.GsonConfig;
import org.hyperledger.bpa.api.aries.AriesCredential;
import org.hyperledger.bpa.api.aries.ProfileVC;
import org.hyperledger.bpa.api.exception.EntityNotFoundException;
import org.hyperledger.bpa.api.exception.NetworkException;
import org.hyperledger.bpa.api.exception.PartnerException;
import org.hyperledger.bpa.api.notification.CredentialAddedEvent;
import org.hyperledger.bpa.api.notification.CredentialOfferedEvent;
import org.hyperledger.bpa.impl.activity.LabelStrategy;
import org.hyperledger.bpa.impl.aries.jsonld.LDContextHelper;
import org.hyperledger.bpa.impl.aries.jsonld.VPManager;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.impl.util.CryptoUtil;
import org.hyperledger.bpa.impl.util.TimeUtil;
import org.hyperledger.bpa.persistence.model.BPACredentialExchange;
import org.hyperledger.bpa.persistence.model.BPASchema;
import org.hyperledger.bpa.persistence.model.MyDocument;
import org.hyperledger.bpa.persistence.model.Partner;
import org.hyperledger.bpa.persistence.repository.HolderCredExRepository;
import org.hyperledger.bpa.persistence.repository.MyDocumentRepository;
import org.hyperledger.bpa.persistence.repository.PartnerRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Wraps all credential holder specific logic that is common for both indy and
 * json-ld credentials.
 */
@Slf4j
@Singleton
public abstract class BaseHolderManager extends BaseCredentialManager {

    @Inject
    HolderCredExRepository holderCredExRepo;

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    ApplicationEventPublisher eventPublisher;

    @Inject
    Converter conv;

    @Inject
    LDContextHelper ldHelper;

    @Inject
    MyDocumentRepository docRepo;

    @Inject
    ObjectMapper mapper;

    @Inject
    VPManager vpMgmt;

    @Inject
    LabelStrategy labelStrategy;

    public abstract BPASchema checkSchema(BaseCredExRecord credExBase);

    // Credential Management - Called By User

    /**
     * Request credential from issuer (partner)
     * 
     * @param partnerId {@link UUID}
     * @param myDocId   {@link UUID}
     * @param version   {@link ExchangeVersion}
     */
    public void sendCredentialProposal(@NonNull UUID partnerId, @NonNull UUID myDocId,
            @Nullable ExchangeVersion version) {
        Partner dbPartner = partnerRepo.findById(partnerId)
                .orElseThrow(
                        () -> new PartnerException(msg.getMessage("api.partner.not.found", Map.of("id", partnerId))));
        MyDocument dbDoc = docRepo.findById(myDocId)
                .orElseThrow(
                        () -> new PartnerException(msg.getMessage("api.document.not.found", Map.of("id", myDocId))));
        if (dbDoc.getSchema() == null) {
            throw new PartnerException(msg.getMessage("api.schema.restriction.schema.not.found",
                    Map.of("id", dbDoc.getSchemaId() != null ? dbDoc.getSchemaId() : "")));
        }
        BPASchema s = dbDoc.getSchema();
        try {
            V1CredentialProposalRequest v1CredentialProposalRequest = V1CredentialProposalRequest
                    .builder()
                    .connectionId(Objects.requireNonNull(dbPartner.getConnectionId()))
                    .schemaId(s.getSchemaId())
                    .credentialProposal(
                            new CredentialPreview(
                                    CredentialAttributes.from(
                                            Objects.requireNonNull(dbDoc.getDocument()))))
                    .build();
            BPACredentialExchange.BPACredentialExchangeBuilder dbCredEx = BPACredentialExchange
                    .builder()
                    .partner(dbPartner)
                    .schema(s)
                    .state(CredentialExchangeState.PROPOSAL_SENT)
                    .pushStateChange(CredentialExchangeState.PROPOSAL_SENT, Instant.now())
                    .role(CredentialExchangeRole.HOLDER);
            if (version == null || ExchangeVersion.V1.equals(version)) {
                ac.issueCredentialSendProposal(v1CredentialProposalRequest).ifPresent(v1 -> dbCredEx
                        .threadId(v1.getThreadId())
                        .credentialExchangeId(v1.getCredentialExchangeId())
                        .credentialProposal(BPACredentialExchange.ExchangePayload
                                .indy(v1.getCredentialProposalDict().getCredentialProposal())));
            } else {
                V2CredentialExchangeFree v2Request;
                if (dbDoc.typeIsIndy()) {
                    v2Request = V1ToV2IssueCredentialConverter.toV20CredExFree(v1CredentialProposalRequest);
                } else {
                    v2Request = V2CredentialExchangeFree.builder()
                            .connectionId(UUID.fromString(Objects.requireNonNull(dbPartner.getConnectionId())))
                            .filter(ldHelper.buildVC(s, mapper.valueToTree(dbDoc.getDocument()), Boolean.FALSE))
                            .build();
                }
                ac.issueCredentialV2SendProposal(v2Request).ifPresent(v2 -> {
                    BPACredentialExchange.ExchangePayload proposal;
                    if (dbDoc.typeIsIndy()) {
                        proposal = BPACredentialExchange.ExchangePayload
                                .indy(V2ToV1IndyCredentialConverter.INSTANCE().toV1Proposal(v2)
                                        .getCredentialProposalDict()
                                        .getCredentialProposal());
                    } else {
                        proposal = BPACredentialExchange.ExchangePayload.jsonLD(v2.resolveLDCredProposal());
                    }
                    dbCredEx
                            .threadId(v2.getThreadId())
                            .credentialExchangeId(v2.getCredentialExchangeId())
                            .exchangeVersion(ExchangeVersion.V2)
                            .credentialProposal(proposal);
                });
            }
            holderCredExRepo.save(dbCredEx.build());
        } catch (IOException e) {
            throw new NetworkException(msg.getMessage("acapy.unavailable"), e);
        }
    }

    /**
     * Holder accepts credential offer received from issuer
     * 
     * @param id {@link UUID} bpa credential exchange id
     */
    public void sendCredentialRequest(@NonNull UUID id) {
        BPACredentialExchange dbEx = holderCredExRepo.findById(id).orElseThrow(EntityNotFoundException::new);
        try {
            if (ExchangeVersion.V1.equals(dbEx.getExchangeVersion())) {
                ac.issueCredentialRecordsSendRequest(dbEx.getCredentialExchangeId());
            } else {
                ac.issueCredentialV2RecordsSendRequest(dbEx.getCredentialExchangeId(), V20CredRequestRequest
                        .builder().build());
            }
        } catch (IOException e) {
            throw new NetworkException(msg.getMessage("acapy.unavailable"), e);
        }
    }

    /**
     * Holder declines credential offer received from issuer
     * 
     * @param id      {@link UUID} bpa credential exchange id
     * @param message optional reason
     */
    public void declineCredentialOffer(@NonNull UUID id, @Nullable String message) {
        if (StringUtils.isEmpty(message)) {
            message = msg.getMessage("api.holder.credential.exchange.declined");
        }
        BPACredentialExchange dbEx = getCredentialExchange(id);
        dbEx.pushStates(CredentialExchangeState.DECLINED, Instant.now());
        holderCredExRepo.updateStates(dbEx.getId(), dbEx.getState(), dbEx.getStateToTimestamp(), message);
        declineCredentialExchange(dbEx, message);
    }

    /**
     * Sets the credential's visibility in the public profile
     * 
     * @param id {@link UUID} bpa credential exchange id
     */
    public void toggleVisibility(UUID id) {
        BPACredentialExchange cred = holderCredExRepo.findById(id).orElseThrow(EntityNotFoundException::new);
        holderCredExRepo.updateIsPublic(id, !cred.checkIfPublic());
        vpMgmt.recreateVerifiablePresentation();
    }

    /**
     * List credential that the user holds in the wallet
     * 
     * @return list of {@link AriesCredential}
     */
    public List<AriesCredential> listHeldCredentials() {
        return holderCredExRepo.findByRoleEqualsAndStateIn(
                CredentialExchangeRole.HOLDER,
                List.of(CredentialExchangeState.CREDENTIAL_ACKED, CredentialExchangeState.DONE))
                .stream()
                .map(this::buildCredential)
                .collect(Collectors.toList());
    }

    /**
     * Find wallet credential by id
     * 
     * @param id {@link UUID} bpa credential exchange id
     * @return {@link AriesCredential}
     */
    public AriesCredential findHeldCredentialById(@NonNull UUID id) {
        return holderCredExRepo.findById(id).map(this::buildCredential).orElseThrow(EntityNotFoundException::new);
    }

    /**
     * Updates the credentials label
     *
     * @param id    the credential id
     * @param label the credentials label
     * @return the updated credential if found
     */
    public AriesCredential updateCredentialById(@NonNull UUID id, @Nullable String label) {
        final AriesCredential cred = findHeldCredentialById(id);
        String mergedLabel = labelStrategy.apply(label, cred);
        holderCredExRepo.updateLabel(id, mergedLabel);
        cred.setLabel(label);
        return cred;
    }

    public void deleteCredentialById(@NonNull UUID id) {
        holderCredExRepo.findById(id).ifPresent(c -> {
            boolean isPublic = c.checkIfPublic();
            try {
                if (c.getReferent() != null) {
                    ac.credentialRemove(c.getReferent());
                }
            } catch (AriesException | IOException e) {
                // if we fail here it's not good, but also no deal-breaker, so log and continue
                log.error("Could not delete aca-py credential for referent: {}", c.getReferent(), e);
            }
            holderCredExRepo.deleteById(id);
            if (isPublic) {
                vpMgmt.recreateVerifiablePresentation();
            }
        });
    }

    // Credential Management - Called By Event Handler

    // credential offer event
    public void handleOfferReceived(@NonNull BaseCredExRecord credExBase,
            @NonNull BPACredentialExchange.ExchangePayload payload, @NonNull ExchangeVersion version) {
        holderCredExRepo.findByCredentialExchangeId(credExBase.getCredentialExchangeId()).ifPresentOrElse(db -> {
            db.pushStates(credExBase.getState());
            holderCredExRepo.updateOnCredentialOfferEvent(db.getId(), db.getState(), db.getStateToTimestamp(), payload);
            // if offer equals proposal send request immediately
            if (CryptoUtil.hashCompare(db.getCredentialProposal(), payload)) {
                sendCredentialRequest(db.getId());
            }
        }, () -> partnerRepo.findByConnectionId(credExBase.getConnectionId()).ifPresent(p -> {
            BPASchema bpaSchema = checkSchema(credExBase);
            BPACredentialExchange ex = BPACredentialExchange
                    .builder()
                    .schema(bpaSchema)
                    .partner(p)
                    .type(payload.getType())
                    .role(CredentialExchangeRole.HOLDER)
                    .state(credExBase.getState())
                    .pushStateChange(credExBase.getState(), TimeUtil.fromISOInstant(credExBase.getUpdatedAt()))
                    .credentialOffer(payload)
                    .credentialExchangeId(credExBase.getCredentialExchangeId())
                    .threadId(credExBase.getThreadId())
                    .exchangeVersion(version)
                    .build();
            holderCredExRepo.save(ex);
            fireCredentialOfferedEvent(ex);
        }));
    }

    // credential request, receive and problem events
    public void handleStateChangesOnly(
            @NonNull String credExId, @Nullable CredentialExchangeState state,
            @NonNull String updatedAt, @Nullable String errorMsg) {
        holderCredExRepo.findByCredentialExchangeId(credExId).ifPresent(db -> {
            if (db.stateIsNotDeclined()) { // already handled
                CredentialExchangeState s = state != null ? state : CredentialExchangeState.PROBLEM;
                db.pushStates(s, updatedAt);
                holderCredExRepo.updateStates(db.getId(), db.getState(), db.getStateToTimestamp(), errorMsg);
            }
        });
    }

    // Internal Events

    public void fireCredentialOfferedEvent(@NonNull BPACredentialExchange updated) {
        AriesCredential ariesCredential = buildCredential(updated);
        eventPublisher.publishEventAsync(CredentialOfferedEvent.builder()
                .credential(ariesCredential)
                .build());
    }

    public void fireCredentialAddedEvent(@NonNull BPACredentialExchange updated) {
        AriesCredential ariesCredential = buildCredential(updated);
        eventPublisher.publishEventAsync(CredentialAddedEvent.builder()
                .credential(ariesCredential)
                .build());
    }

    // Helper Methods

    /**
     * Tries to resolve the issuers DID into a human-readable name. Resolution order
     * is: 1. Partner alias the user gave 2. Legal name from the partners public
     * profile 3. ACA-PY Label 4. DID
     *
     * @param p {@link Partner}
     * @return the issuer or null when the credential or the credential definition
     *         id is null
     */
    @Nullable
    public String resolveIssuer(@Nullable Partner p) {
        String issuer = null;
        if (p != null) {
            if (StringUtils.isNotEmpty(p.getAlias())) {
                issuer = p.getAlias();
            } else if (p.getVerifiablePresentation() != null) {
                VerifiablePresentation<VerifiableCredential.VerifiableIndyCredential> vp = conv
                        .fromMap(Objects.requireNonNull(p.getVerifiablePresentation()), Converter.VP_TYPEREF);
                Optional<VerifiableCredential.VerifiableIndyCredential> profile = vp.getVerifiableCredential()
                        .stream().filter(ic -> ic.getType().contains("OrganizationalProfileCredential")).findAny();
                if (profile.isPresent() && profile.get().getCredentialSubject() != null) {
                    ProfileVC pVC = GsonConfig.jacksonBehaviour().fromJson(profile.get().getCredentialSubject(),
                            ProfileVC.class);
                    issuer = pVC.getLegalName();
                }
            }
            if (issuer == null && Boolean.TRUE.equals(p.getIncoming())) {
                issuer = p.getLabel();
            }
            if (issuer == null) {
                issuer = p.getDid();
            }
        }
        return issuer;
    }

    private AriesCredential buildCredential(@NonNull BPACredentialExchange dbCred) {
        return AriesCredential.fromBPACredentialExchange(dbCred,
                dbCred.getSchema() != null ? dbCred.getSchema().resolveSchemaLabel() : null);
    }
}
