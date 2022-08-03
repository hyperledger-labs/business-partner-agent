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

import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.acy_py.generated.model.V20CredRequestRequest;
import org.hyperledger.aries.api.ExchangeVersion;
import org.hyperledger.aries.api.issue_credential_v1.BaseCredExRecord;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecord;
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecordByFormat;
import org.hyperledger.aries.api.issue_credential_v2.V2ToV1IndyCredentialConverter;
import org.hyperledger.aries.api.jsonld.VerifiableCredential;
import org.hyperledger.aries.api.jsonld.VerifiablePresentation;
import org.hyperledger.aries.api.revocation.RevocationNotificationBase;
import org.hyperledger.aries.config.GsonConfig;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.api.aries.AriesCredential;
import org.hyperledger.bpa.api.aries.ProfileVC;
import org.hyperledger.bpa.api.exception.EntityNotFoundException;
import org.hyperledger.bpa.api.exception.NetworkException;
import org.hyperledger.bpa.api.exception.PartnerException;
import org.hyperledger.bpa.api.notification.CredentialAddedEvent;
import org.hyperledger.bpa.api.notification.CredentialOfferedEvent;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.impl.activity.LabelStrategy;
import org.hyperledger.bpa.impl.aries.jsonld.VPManager;
import org.hyperledger.bpa.impl.aries.wallet.Identity;
import org.hyperledger.bpa.impl.util.CryptoUtil;
import org.hyperledger.bpa.impl.util.TimeUtil;
import org.hyperledger.bpa.persistence.model.BPACredentialExchange;
import org.hyperledger.bpa.persistence.model.BPASchema;
import org.hyperledger.bpa.persistence.model.MyDocument;
import org.hyperledger.bpa.persistence.model.Partner;
import org.hyperledger.bpa.persistence.model.converter.ExchangePayload;
import org.hyperledger.bpa.persistence.repository.HolderCredExRepository;
import org.hyperledger.bpa.persistence.repository.MyDocumentRepository;
import org.hyperledger.bpa.persistence.repository.PartnerRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

/**
 * Wraps all credential holder specific logic that is common for both indy and
 * json-ld credentials.
 */
@Slf4j
@Singleton
public class HolderManager extends CredentialManagerBase {

    @Inject
    HolderIndyManager indy;

    @Inject
    HolderLDManager ld;

    @Inject
    HolderCredExRepository holderCredExRepo;

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    ApplicationEventPublisher eventPublisher;

    @Inject
    MyDocumentRepository docRepo;

    @Inject
    VPManager vpMgmt;

    @Inject
    LabelStrategy labelStrategy;

    @Inject
    Identity identity;

    @Inject
    BPAMessageSource.DefaultMessageSource ms;

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
            BPACredentialExchange.BPACredentialExchangeBuilder dbCredEx = BPACredentialExchange
                    .builder()
                    .partner(dbPartner)
                    .schema(s)
                    .state(CredentialExchangeState.PROPOSAL_SENT)
                    .pushStateChange(CredentialExchangeState.PROPOSAL_SENT, Instant.now())
                    .role(CredentialExchangeRole.HOLDER);
            String connectionId = Objects.requireNonNull(dbPartner.getConnectionId());
            Map<String, Object> document = Objects.requireNonNull(dbDoc.getDocument());
            if (dbDoc.typeIsIndy()) {
                indy.sendCredentialProposal(connectionId, s.getSchemaId(), document, dbCredEx, version);
            } else {
                ld.sendCredentialProposal(connectionId, s, document, dbCredEx);
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
                V20CredRequestRequest.V20CredRequestRequestBuilder credentialRequest = V20CredRequestRequest
                        .builder();
                if (dbEx.typeIsJsonLd()) {
                    credentialRequest.holderDid(identity.getMyDid());
                }
                ac.issueCredentialV2RecordsSendRequest(dbEx.getCredentialExchangeId(), credentialRequest.build());
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
     * @param typesToFilter filter by provided credential types
     * @param pageable      {@link Pageable}
     * @return list of {@link AriesCredential}
     */
    public Page<AriesCredential> listHeldCredentials(
            @Nullable List<CredentialType> typesToFilter,
            @NonNull Pageable pageable) {
        List<CredentialType> types = CollectionUtils.isNotEmpty(typesToFilter)
                ? typesToFilter
                : List.of(CredentialType.values());
        return holderCredExRepo.findByRoleEqualsAndStateInAndTypeIn(
                CredentialExchangeRole.HOLDER,
                List.of(CredentialExchangeState.CREDENTIAL_ACKED, CredentialExchangeState.DONE),
                types, pageable)
                .map(this::buildCredential);
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
                if (StringUtils.isNotEmpty(c.getReferent())) {
                    if (c.typeIsIndy()) {
                        ac.credentialRemove(c.getReferent());
                    } else {
                        ac.credentialW3CRemove(c.getReferent());
                    }
                }
            } catch (IOException e) {
                log.error("Could not delete aca-py credential for referent: {}", c.getReferent(), e);
                throw new NetworkException("acapy.unavailable");
            }
            holderCredExRepo.deleteById(id);
            if (isPublic) {
                vpMgmt.recreateVerifiablePresentation();
            }
        });
    }

    // Credential Management - Called By Event Handler

    // v1 credential, signed and stored in wallet
    public void handleV1CredentialExchangeAcked(@NonNull V1CredentialExchange credEx) {
        String label = labelStrategy.apply(credEx.getCredential());
        holderCredExRepo.findByCredentialExchangeId(credEx.getCredentialExchangeId()).ifPresent(db -> {
            db
                    .setReferent(credEx.getCredential() != null ? credEx.getCredential().getReferent() : null)
                    .setIndyCredential(credEx.getCredential())
                    .setCredRevId(credEx.getCredential() != null ? credEx.getCredential().getCredRevId() : null)
                    .setRevRegId(credEx.getCredential() != null ? credEx.getCredential().getRevRegId() : null)
                    .setLabel(label)
                    .pushStates(credEx.getState(), TimeUtil.fromISOInstant(credEx.getUpdatedAt()));
            holderCredExRepo.update(db);
            fireCredentialAddedEvent(db);
        });
    }

    public void handleV2OfferReceived(@NonNull V20CredExRecord v2CredEx) {
        if (v2CredEx.payloadIsLdProof()) {
            handleOfferReceived(v2CredEx,
                    ExchangePayload.jsonLD(v2CredEx.resolveLDCredOffer()),
                    ExchangeVersion.V2);
        } else {
            handleOfferReceived(v2CredEx,
                    ExchangePayload.indy(V2ToV1IndyCredentialConverter.INSTANCE()
                            .toV1Offer(v2CredEx).getCredentialProposalDict().getCredentialProposal()),
                    ExchangeVersion.V2);
        }
    }

    // credential offer event
    public void handleOfferReceived(@NonNull BaseCredExRecord credExBase,
            @NonNull ExchangePayload<V1CredentialExchange.CredentialProposalDict.CredentialProposal, V20CredExRecordByFormat.LdProof> payload,
            @NonNull ExchangeVersion version) {
        holderCredExRepo.findByCredentialExchangeId(credExBase.getCredentialExchangeId()).ifPresentOrElse(db -> {
            db.pushStates(credExBase.getState());
            holderCredExRepo.updateOnCredentialOfferEvent(db.getId(), db.getState(), db.getStateToTimestamp(), payload);
            // if offer equals proposal send request immediately
            if (CryptoUtil.hashCompare(db.getCredentialProposal(), payload)) {
                sendCredentialRequest(db.getId());
            }
        }, () -> partnerRepo.findByConnectionId(credExBase.getConnectionId()).ifPresent(p -> {
            BPASchema bpaSchema;
            if (payload.typeIsIndy()) {
                bpaSchema = indy.checkSchema(credExBase);
            } else {
                bpaSchema = ld.checkSchema(credExBase);
            }
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

    public void handleV2CredentialReceived(@NonNull V20CredExRecord credEx) {
        holderCredExRepo.findByCredentialExchangeId(credEx.getCredentialExchangeId()).ifPresent(dbCred -> {
            if (dbCred.typeIsIndy()) {
                indy.handleV2CredentialReceived(credEx, dbCred);
            } else {
                ld.handleV2CredentialReceived(credEx, dbCred);
            }
            fireCredentialAddedEvent(dbCred);
        });
    }

    // credential request, receive and problem events
    public void handleStateChangesOnly(
            @NonNull String credExId, @Nullable CredentialExchangeState state,
            @NonNull String updatedAt, @Nullable String errorMsg) {
        holderCredExRepo.findByCredentialExchangeId(credExId).ifPresent(db -> {
            if (db.stateIsNotDeclined()) { // already handled
                CredentialExchangeState s = state == null || CredentialExchangeState.ABANDONED.equals(state)
                        ? CredentialExchangeState.PROBLEM
                        : state;
                db.pushStates(s, updatedAt);
                holderCredExRepo.updateStates(db.getId(), db.getState(), db.getStateToTimestamp(), errorMsg);
            }
        });
    }

    // v1 and v2 revocation notifications
    public void handleRevocationNotification(RevocationNotificationBase.RevocationInfo revocationNotification) {
        holderCredExRepo
                .findByRevRegIdAndCredRevId(revocationNotification.getRevRegId(), revocationNotification.getCredRevId())
                .ifPresent(credEx -> {
                    credEx.pushStates(CredentialExchangeState.CREDENTIAL_REVOKED, Instant.now());
                    holderCredExRepo.updateRevoked(credEx.getId(), true, credEx.getState(),
                            credEx.getStateToTimestamp());
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
    public static String resolveIssuer(@Nullable Partner p) {
        String issuer = null;
        if (p != null) {
            if (StringUtils.isNotEmpty(p.getAlias())) {
                issuer = p.getAlias();
            } else if (p.getVerifiablePresentation() != null) {
                VerifiablePresentation<VerifiableCredential.VerifiableIndyCredential> vp = Objects
                        .requireNonNull(p.getVerifiablePresentation());
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
                dbCred.getSchema() != null ? dbCred.getSchema().resolveSchemaLabel() : null,
                resolveIssuer(dbCred.getPartner()));
    }
}
