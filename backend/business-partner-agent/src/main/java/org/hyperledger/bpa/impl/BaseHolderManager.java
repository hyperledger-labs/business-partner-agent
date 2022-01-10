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
package org.hyperledger.bpa.impl;

import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.core.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.Setter;
import org.hyperledger.acy_py.generated.model.V20CredRequestRequest;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.ExchangeVersion;
import org.hyperledger.aries.api.issue_credential_v1.BaseCredExRecord;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.bpa.api.aries.AriesCredential;
import org.hyperledger.bpa.api.exception.EntityNotFoundException;
import org.hyperledger.bpa.api.exception.NetworkException;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.impl.aries.config.SchemaService;
import org.hyperledger.bpa.impl.notification.CredentialAddedEvent;
import org.hyperledger.bpa.impl.notification.CredentialOfferedEvent;
import org.hyperledger.bpa.impl.util.TimeUtil;
import org.hyperledger.bpa.model.BPACredentialExchange;
import org.hyperledger.bpa.model.BPASchema;
import org.hyperledger.bpa.repository.HolderCredExRepository;
import org.hyperledger.bpa.repository.PartnerRepository;
import org.hyperledger.bpa.util.CryptoUtil;

import java.io.IOException;
import java.util.UUID;

@Singleton
public abstract class BaseHolderManager {

    @Inject
    @Setter(AccessLevel.PACKAGE)
    AriesClient ac;

    @Inject
    HolderCredExRepository holderCredExRepo;

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    BPAMessageSource.DefaultMessageSource msg;

    @Inject
    SchemaService schemaService;

    @Inject
    ApplicationEventPublisher eventPublisher;

    public abstract BPASchema checkSchema(BaseCredExRecord credExBase);

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

    private AriesCredential buildCredential(@NonNull BPACredentialExchange dbCred) {
        String typeLabel = null;
        if (dbCred.getIndyCredential() != null) {
            typeLabel = schemaService.getSchemaLabel(dbCred.getIndyCredential().getSchemaId());
        }
        return AriesCredential.fromBPACredentialExchange(dbCred, typeLabel);
    }
}
