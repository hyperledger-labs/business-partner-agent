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

import io.micronaut.core.annotation.Nullable;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.ExchangeVersion;
import org.hyperledger.aries.api.credentials.CredentialAttributes;
import org.hyperledger.aries.api.credentials.CredentialPreview;
import org.hyperledger.aries.api.exception.AriesException;
import org.hyperledger.aries.api.issue_credential_v1.*;
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecord;
import org.hyperledger.aries.api.issue_credential_v2.V2ToV1IndyCredentialConverter;
import org.hyperledger.aries.api.revocation.RevocationNotificationEvent;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.api.aries.AriesCredential;
import org.hyperledger.bpa.api.aries.SchemaAPI;
import org.hyperledger.bpa.api.exception.EntityNotFoundException;
import org.hyperledger.bpa.api.exception.NetworkException;
import org.hyperledger.bpa.api.exception.PartnerException;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.impl.activity.LabelStrategy;
import org.hyperledger.bpa.impl.aries.jsonld.VPManager;
import org.hyperledger.bpa.impl.aries.schema.SchemaService;
import org.hyperledger.bpa.impl.util.AriesStringUtil;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class HolderCredentialManager extends BaseHolderManager {

    @Inject
    @Setter(AccessLevel.PROTECTED)
    AriesClient ac;

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    MyDocumentRepository docRepo;

    @Inject
    HolderCredExRepository holderCredExRepo;

    @Inject
    VPManager vpMgmt;

    @Inject
    @Setter(AccessLevel.PROTECTED)
    SchemaService schemaService;

    @Inject
    LabelStrategy labelStrategy;

    @Inject
    BPAMessageSource.DefaultMessageSource msg;

    // request credential from issuer (partner)
    public void sendCredentialRequest(@NonNull UUID partnerId, @NonNull UUID myDocId,
            @Nullable ExchangeVersion version) {
        Partner dbPartner = partnerRepo.findById(partnerId)
                .orElseThrow(
                        () -> new PartnerException(msg.getMessage("api.partner.not.found", Map.of("id", partnerId))));
        MyDocument dbDoc = docRepo.findById(myDocId)
                .orElseThrow(
                        () -> new PartnerException(msg.getMessage("api.document.not.found", Map.of("id", myDocId))));
        if (!CredentialType.INDY.equals(dbDoc.getType())) {
            throw new PartnerException(msg.getMessage("api.schema.credential.document.conversion.failure"));
        }
        try {
            BPASchema s = schemaService.getSchemaFor(dbDoc.getSchemaId())
                    .orElseThrow(
                            () -> new PartnerException(msg.getMessage("api.schema.restriction.schema.not.found",
                                    Map.of("id", dbDoc.getSchemaId() != null ? dbDoc.getSchemaId() : ""))));
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
                ac.issueCredentialV2SendProposal(v1CredentialProposalRequest).ifPresent(v2 -> dbCredEx
                        .threadId(v2.getThreadId())
                        .credentialExchangeId(v2.getCredentialExchangeId())
                        .exchangeVersion(ExchangeVersion.V2)
                        .credentialProposal(BPACredentialExchange.ExchangePayload
                                .indy(V2ToV1IndyCredentialConverter.INSTANCE().toV1Proposal(v2)
                                        .getCredentialProposalDict()
                                        .getCredentialProposal())));
            }
            holderCredExRepo.save(dbCredEx.build());
        } catch (IOException e) {
            throw new NetworkException(msg.getMessage("acapy.unavailable"), e);
        }
    }

    public void declineCredentialOffer(@NonNull UUID id, @Nullable String message) {
        if (StringUtils.isEmpty(message)) {
            message = msg.getMessage("api.holder.credential.exchange.declined");
        }
        BPACredentialExchange dbEx = getCredentialExchange(id);
        dbEx.pushStates(CredentialExchangeState.DECLINED, Instant.now());
        holderCredExRepo.updateStates(dbEx.getId(), dbEx.getState(), dbEx.getStateToTimestamp(), message);
        declineCredentialExchange(dbEx, message);
    }

    // credential visible in public profile
    public void toggleVisibility(UUID id) {
        BPACredentialExchange cred = holderCredExRepo.findById(id).orElseThrow(EntityNotFoundException::new);
        holderCredExRepo.updateIsPublic(id, !cred.checkIfPublic());
        vpMgmt.recreateVerifiablePresentation();
    }

    // credential CRUD operations

    public List<AriesCredential> listCredentials() {
        return holderCredExRepo.findByRoleEqualsAndStateIn(
                CredentialExchangeRole.HOLDER,
                List.of(CredentialExchangeState.CREDENTIAL_ACKED, CredentialExchangeState.DONE))
                .stream()
                .map(this::buildCredential)
                .collect(Collectors.toList());
    }

    public AriesCredential getCredentialById(@NonNull UUID id) {
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
        final AriesCredential cred = getCredentialById(id);
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

    /**
     * Scheduled task that checks the revocation status of all credentials issued to
     * this BPA.
     */
    @Scheduled(fixedDelay = "5m", initialDelay = "1m")
    void checkRevocationStatus() {
        log.trace("Running revocation checks");
        holderCredExRepo.findNotRevoked().parallelStream().forEach(cred -> {
            try {
                log.trace("Running revocation check for credential exchange: {}", cred.getReferent());
                ac.credentialRevoked(Objects.requireNonNull(cred.getReferent())).ifPresent(isRevoked -> {
                    if (isRevoked.getRevoked() != null && isRevoked.getRevoked()) {
                        cred.pushStates(CredentialExchangeState.CREDENTIAL_REVOKED, Instant.now());
                        holderCredExRepo.updateRevoked(cred.getId(), Boolean.TRUE, cred.getState(),
                                cred.getStateToTimestamp());
                        log.debug("Credential with referent id: {} has been revoked", cred.getReferent());
                    }
                });
            } catch (AriesException e) {
                if (e.getCode() == 404) {
                    log.error("aca-py has no credential with referent id: {}", cred.getReferent());
                    holderCredExRepo.updateReferent(cred.getId(), null);
                }
            } catch (Exception e) {
                log.error("Revocation check failed", e);
            }
        });
    }

    // credential event handling

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
                    .setIssuer(resolveIssuer(db.getPartner()))
                    .pushStates(credEx.getState(), TimeUtil.fromISOInstant(credEx.getUpdatedAt()));
            holderCredExRepo.update(db);
            fireCredentialAddedEvent(db);
        });
    }

    // v2 credential, signed and stored in wallet
    public void handleV2CredentialReceived(@NonNull V20CredExRecord credEx) {
        holderCredExRepo.findByCredentialExchangeId(credEx.getCredentialExchangeId()).ifPresent(
                dbCred -> V2ToV1IndyCredentialConverter.INSTANCE().toV1Credential(credEx)
                        .ifPresent(c -> {
                            String label = labelStrategy.apply(c);
                            dbCred
                                    .pushStates(credEx.getState(), credEx.getUpdatedAt())
                                    .setIndyCredential(c)
                                    .setLabel(label)
                                    .setIssuer(resolveIssuer(dbCred.getPartner()));
                            BPACredentialExchange dbCredential = holderCredExRepo.update(dbCred);
                            fireCredentialAddedEvent(dbCredential);
                        }));
    }

    public void handleRevocationNotification(RevocationNotificationEvent revocationNotification) {
        AriesStringUtil.RevocationInfo revocationInfo = AriesStringUtil
                .revocationEventToRevocationInfo(revocationNotification.getThreadId());
        holderCredExRepo.findByRevRegIdAndCredRevId(revocationInfo.getRevRegId(), revocationInfo.getCredRevId())
                .ifPresent(credEx -> {
                    credEx.pushStates(CredentialExchangeState.CREDENTIAL_REVOKED, Instant.now());
                    holderCredExRepo.updateRevoked(credEx.getId(), true, credEx.getState(),
                            credEx.getStateToTimestamp());
                });
    }

    @Override
    public BPASchema checkSchema(BaseCredExRecord credEx) {
        String schemaId = null;
        BPASchema bpaSchema = null;
        if (credEx instanceof V1CredentialExchange) {
            schemaId = ((V1CredentialExchange) credEx).getSchemaId();
        } else if (credEx instanceof V20CredExRecord) {
            schemaId = V2ToV1IndyCredentialConverter.INSTANCE()
                    .toV1Offer((V20CredExRecord) credEx).getCredentialProposalDict().getSchemaId();
        }
        if (schemaId != null) {
            bpaSchema = schemaService.getSchemaFor(schemaId).orElse(null);
            if (bpaSchema == null) {
                SchemaAPI schemaAPI = schemaService.addIndySchema(schemaId, null, null);
                if (schemaAPI != null) {
                    bpaSchema = BPASchema.builder().id(schemaAPI.getId()).build();
                }
            }
        }
        return bpaSchema;
    }
}
