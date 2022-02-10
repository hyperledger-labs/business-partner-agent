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

import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.exception.AriesException;
import org.hyperledger.aries.api.issue_credential_v1.BaseCredExRecord;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecord;
import org.hyperledger.aries.api.issue_credential_v2.V2ToV1IndyCredentialConverter;
import org.hyperledger.aries.api.revocation.RevocationNotificationEvent;
import org.hyperledger.bpa.api.aries.SchemaAPI;
import org.hyperledger.bpa.impl.activity.LabelStrategy;
import org.hyperledger.bpa.impl.aries.schema.SchemaService;
import org.hyperledger.bpa.impl.util.AriesStringUtil;
import org.hyperledger.bpa.impl.util.TimeUtil;
import org.hyperledger.bpa.persistence.model.BPACredentialExchange;
import org.hyperledger.bpa.persistence.model.BPASchema;
import org.hyperledger.bpa.persistence.repository.HolderCredExRepository;

import java.time.Instant;
import java.util.Objects;

/**
 * Handles all credential holder logic that is specific to indy
 */
@Slf4j
@Singleton
public class HolderCredentialManager extends BaseHolderManager {

    @Inject
    @Setter(AccessLevel.PROTECTED)
    AriesClient ac;

    @Inject
    HolderCredExRepository holderCredExRepo;

    @Inject
    LabelStrategy labelStrategy;

    @Inject
    @Setter(AccessLevel.PACKAGE)
    SchemaService schemaService;

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
