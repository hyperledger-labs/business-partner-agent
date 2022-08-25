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
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.ExchangeVersion;
import org.hyperledger.aries.api.credentials.CredentialAttributes;
import org.hyperledger.aries.api.credentials.CredentialPreview;
import org.hyperledger.aries.api.exception.AriesException;
import org.hyperledger.aries.api.issue_credential_v1.BaseCredExRecord;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialProposalRequest;
import org.hyperledger.aries.api.issue_credential_v2.V1ToV2IssueCredentialConverter;
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecord;
import org.hyperledger.aries.api.issue_credential_v2.V2CredentialExchangeFree;
import org.hyperledger.aries.api.issue_credential_v2.V2ToV1IndyCredentialConverter;
import org.hyperledger.bpa.api.aries.SchemaAPI;
import org.hyperledger.bpa.impl.activity.LabelStrategy;
import org.hyperledger.bpa.impl.aries.schema.SchemaService;
import org.hyperledger.bpa.persistence.model.BPACredentialExchange;
import org.hyperledger.bpa.persistence.model.BPASchema;
import org.hyperledger.bpa.persistence.model.converter.ExchangePayload;
import org.hyperledger.bpa.persistence.repository.HolderCredExRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Handles all credential holder logic that is specific to indy
 */
@Slf4j
@Singleton
public class HolderIndyManager {

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

    public void sendCredentialProposal(
            @NonNull String connectionId,
            @NonNull String schemaId,
            @NonNull Map<String, Object> document,
            @NonNull BPACredentialExchange.BPACredentialExchangeBuilder dbCredEx,
            @Nullable ExchangeVersion version)
            throws IOException {
        V1CredentialProposalRequest v1CredentialProposalRequest = V1CredentialProposalRequest
                .builder()
                .connectionId(Objects.requireNonNull(connectionId))
                .schemaId(schemaId)
                .credentialProposal(
                        new CredentialPreview(
                                CredentialAttributes.from(
                                        Objects.requireNonNull(document))))
                .build();
        if (version == null || ExchangeVersion.V1.equals(version)) {
            ac.issueCredentialSendProposal(v1CredentialProposalRequest).ifPresent(v1 -> dbCredEx
                    .threadId(v1.getThreadId())
                    .credentialExchangeId(v1.getCredentialExchangeId())
                    .credentialProposal(ExchangePayload
                            .indy(v1.getCredentialProposalDict().getCredentialProposal())));
        } else {
            V2CredentialExchangeFree v2Request = V1ToV2IssueCredentialConverter
                    .toV20CredExFree(v1CredentialProposalRequest);
            ac.issueCredentialV2SendProposal(v2Request).ifPresent(v2 -> dbCredEx
                    .threadId(v2.getThreadId())
                    .credentialExchangeId(v2.getCredentialExchangeId())
                    .exchangeVersion(ExchangeVersion.V2)
                    .credentialProposal(ExchangePayload
                            .indy(V2ToV1IndyCredentialConverter.INSTANCE().toV1Proposal(v2)
                                    .getCredentialProposalDict()
                                    .getCredentialProposal())));
        }
    }

    /**
     * Scheduled task that checks the revocation status of all credentials issued to
     * this BPA.
     */
    @Scheduled(fixedDelay = "5m", initialDelay = "1m")
    void checkRevocationStatus() {
        log.trace("Running revocation checks");
        Page<BPACredentialExchange> notRevoked = holderCredExRepo.findNotRevoked(Pageable.from(0, 25));
        do {
            notRevoked = pageRevocationStatusCheck(notRevoked);
        } while (notRevoked.getNumberOfElements() > 0);
    }

    private Page<BPACredentialExchange> pageRevocationStatusCheck(@NonNull Page<BPACredentialExchange> page) {
        page.forEach(cred -> {
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
        return holderCredExRepo.findNotRevoked(page.nextPageable());
    }

    // credential event handling

    // v2 credential, signed and stored in wallet
    public void handleV2CredentialReceived(@NonNull V20CredExRecord credEx, @NonNull BPACredentialExchange dbCred) {
        V2ToV1IndyCredentialConverter.INSTANCE().toV1Credential(credEx)
                .ifPresent(c -> {
                    String label = labelStrategy.apply(c);
                    dbCred
                            .pushStates(credEx.getState(), credEx.getUpdatedAt())
                            .setIndyCredential(c)
                            .setLabel(label);
                    holderCredExRepo.update(dbCred);
                });
    }

    public BPASchema checkSchema(BaseCredExRecord credEx) {
        String schemaId = null;
        BPASchema bpaSchema = null;
        if (credEx instanceof V1CredentialExchange) {
            schemaId = ((V1CredentialExchange) credEx).getSchemaId();
        } else if (credEx instanceof V20CredExRecord) {
            schemaId = V2ToV1IndyCredentialConverter
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
