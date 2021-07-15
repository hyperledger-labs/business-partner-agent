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
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.CollectionUtils;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.connection.*;
import org.hyperledger.aries.api.did_exchange.DidExchangeCreateRequestFilter;
import org.hyperledger.aries.api.endorser.SetEndorserInfoFilter;
import org.hyperledger.aries.api.endorser.SetEndorserRoleFilter;
import org.hyperledger.aries.api.exception.AriesException;
import org.hyperledger.aries.api.present_proof.PresentProofRecordsFilter;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord;
import org.hyperledger.acy_py.generated.model.TransactionJobs;
import org.hyperledger.bpa.api.PartnerAPI;
import org.hyperledger.bpa.api.exception.NetworkException;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.config.RuntimeConfig;
import org.hyperledger.bpa.controller.api.WebSocketMessageBody;
import org.hyperledger.bpa.impl.MessageService;
import org.hyperledger.bpa.impl.activity.DidResolver;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.impl.util.TimeUtil;
import org.hyperledger.bpa.model.Partner;
import org.hyperledger.bpa.model.PartnerProof;
import org.hyperledger.bpa.model.Tag;
import org.hyperledger.bpa.repository.MyCredentialRepository;
import org.hyperledger.bpa.repository.PartnerProofRepository;
import org.hyperledger.bpa.repository.PartnerRepository;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class ConnectionManager {

    private static final String UNKNOWN_DID = "unknown";
    private static final String CONNECTION_INVITATION = "Invitation";

    @Value("${bpa.did.prefix}")
    String didPrefix;

    @Inject
    AriesClient ac;

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    PartnerProofRepository partnerProofRepo;

    @Inject
    MyCredentialRepository myCredRepo;

    @Inject
    MessageService messageService;

    @Inject
    Converter conv;

    @Inject
    DidResolver didResolver;

    @Inject
    BPAMessageSource.DefaultMessageSource messageSource;

    @Inject
    RuntimeConfig config;

    /**
     * Creates a connection invitation to be used within a barcode
     *
     * @param alias optional connection alias
     * @param tags  tags associated with this connection/invitation
     */
    public Optional<CreateInvitationResponse> createConnectionInvitation(
            @Nullable String alias, @Nullable List<Tag> tags) {
        Optional<CreateInvitationResponse> result = Optional.empty();
        try {
            String aliasWithFallback = StringUtils.isNotEmpty(alias) ? alias
                    : CONNECTION_INVITATION + TimeUtil.currentTimeFormatted(Instant.now());
            result = ac.connectionsCreateInvitation(
                    CreateInvitationRequest.builder()
                            .build(),
                    CreateInvitationParams.builder()
                            .alias(aliasWithFallback)
                            .autoAccept(Boolean.TRUE)
                            .build());
            result.ifPresent(r -> partnerRepo.save(Partner
                    .builder()
                    .ariesSupport(Boolean.TRUE)
                    .alias(aliasWithFallback)
                    .connectionId(r.getConnectionId())
                    .did(didPrefix + UNKNOWN_DID)
                    .state(ConnectionState.INVITATION)
                    .incoming(Boolean.TRUE)
                    .tags(tags != null ? new HashSet<>(tags) : null)
                    .build()));
        } catch (IOException e) {
            log.error("Could not create aries connection invitation", e);
        }
        return result;
    }

    /**
     * Create a connection based on a did, e.g. did:web or did:indy.
     * 
     * @param did the fully qualified did like did:indy:123
     * @return {@link ConnectionRecord}
     */
    public Optional<ConnectionRecord> createConnection(@NonNull String did) {
        try {
            return ac.didExchangeCreateRequest(
                    DidExchangeCreateRequestFilter
                            .builder()
                            .theirPublicDid(did)
                            .usePublicDid(Boolean.TRUE)
                            .build());
        } catch (IOException e) {
            String msg = messageSource.getMessage("acapy.unavailable");
            log.error(msg, e);
            throw new NetworkException(msg);
        }
    }

    public void acceptConnection(@NonNull String connectionId) {
        try {
            ac.didExchangeAcceptRequest(connectionId, null);
        } catch (IOException e) {
            String msg = messageSource.getMessage("acapy.unavailable");
            log.error(msg, e);
            throw new NetworkException(msg);
        }
    }

    public synchronized void addConnectionEndorserMetadata(ConnectionRecord record, Partner p) {
        log.info("TODO addConnectionEndorserMetadata() for: {}", p);

        TransactionJobs.TransactionMyJobEnum txMyJob;
        TransactionJobs.TransactionTheirJobEnum txTheirJob;
        if (p.hasTag("Author")) {
            // our partner is tagged as an "Author" so we set our role on the connection as "Author"
            log.info("TODO add connection metadata for Author connection: {}", p);
            txMyJob = TransactionJobs.TransactionMyJobEnum.TRANSACTION_ENDORSER;
            txTheirJob = TransactionJobs.TransactionTheirJobEnum.TRANSACTION_AUTHOR;
        } else if (p.hasTag("Endorser")) {
            // our partner is tagged as an "Endorser" so we set our role on the connection as "Author"
            log.info("TODO add connection metadata for Endorser connection: {}", p);
            txMyJob = TransactionJobs.TransactionMyJobEnum.TRANSACTION_AUTHOR;
            txTheirJob = TransactionJobs.TransactionTheirJobEnum.TRANSACTION_ENDORSER;
        } else {
            log.info("TODO no connection role, return no-op");
            return;
        }

        try {
            // set the endorser role on the connection
            SetEndorserRoleFilter serf = SetEndorserRoleFilter
                .builder()
                .transactionMyJob(TransactionJobs
                    .builder()
                    .transactionMyJob(txMyJob)
                    .transactionTheirJob(txTheirJob)
                    .build()
                )
                .build();
            log.info("TODO set endorser role to: {} with {}", txMyJob, serf);
            ac.endorseTransactionSetEndorserRole(record.getConnectionId(), serf);

            if (p.hasTag("Endorser")) {
                // we have to set the extra Endorser info
                SetEndorserInfoFilter seif = SetEndorserInfoFilter
                    .builder()
                    .endorserDid(p.getDid())
                    .endorserName(p.getAlias())
                    .build();
                log.info("TODO set endorser info to: {} {} with {}", p.getDid(), p.getAlias(), seif);
                ac.endorseTransactionSetEndorserInfo(record.getConnectionId(), seif);
            }
        } catch (IOException e) {
            String msg = messageSource.getMessage("acapy.unavailable");
            log.error(msg, e);
            throw new NetworkException(msg);
        }
}

    // connection that originated from this agent
    public synchronized void handleOutgoingConnectionEvent(ConnectionRecord record) {
        partnerRepo.findByConnectionId(record.getConnectionId()).ifPresent(
                dbP -> {
                    if (StringUtils.isEmpty(dbP.getLabel())) {
                        dbP.setLabel(record.getTheirLabel());
                        dbP.setState(record.getState());
                        partnerRepo.update(dbP);
                    } else {
                        partnerRepo.updateState(dbP.getId(), record.getState());
                    }

                    // if partner is tagged as "Author"/"Endorser" create appropriate meta-data
                    log.info("TODO check for outbound Endorser event for existing partner: {} {}", record, dbP);
                    if (config.hasEndorserRole()) {
                        addConnectionEndorserMetadata(record, dbP);
                    }
                });
    }

    // handles invitations and incoming connection events
    public synchronized void handleIncomingConnectionEvent(ConnectionRecord record) {
        partnerRepo.findByConnectionId(record.getConnectionId()).ifPresentOrElse(
                dbP -> {
                    if (dbP.getAlias() != null && dbP.getAlias().startsWith(CONNECTION_INVITATION)) {
                        dbP.setAlias(record.getTheirLabel());
                    }
                    if (StringUtils.isEmpty(dbP.getDid()) || dbP.getDid().endsWith(UNKNOWN_DID)) {
                        dbP.setDid(didPrefix + record.getTheirDid());
                    }
                    dbP.setState(record.getState());
                    partnerRepo.update(dbP);
                    resolveAndSend(record, dbP);

                    // if partner is tagged as "Author"/"Endorser" create appropriate meta-data
                    log.info("TODO check for inbound Endorser event for existing partner: {} {}", record, dbP);
                    if (config.hasEndorserRole()) {
                        addConnectionEndorserMetadata(record, dbP);
                    }
                },
                () -> {
                    Partner p = Partner
                            .builder()
                            .ariesSupport(Boolean.TRUE)
                            .connectionId(record.getConnectionId())
                            .did(StringUtils.isNotEmpty(record.getTheirDid())
                                    ? didPrefix + record.getTheirDid()
                                    : didPrefix + UNKNOWN_DID)
                            .state(record.getState())
                            .label(record.getTheirLabel())
                            .incoming(Boolean.TRUE)
                            .build();
                    p = partnerRepo.save(p);
                    resolveAndSend(record, p);

                    // if partner is tagged as "Author"/"Endorser" create appropriate meta-data
                    log.info("TODO check for inbound Endorser event for new partner: {} {}", record, p);
                    if (config.hasEndorserRole()) {
                        addConnectionEndorserMetadata(record, p);
                    }
                });
    }

    private void resolveAndSend(ConnectionRecord record, Partner p) {
        // only incoming connections in state request
        if (ConnectionState.REQUEST.equals(record.getState())) {
            didResolver.lookupIncoming(p);
            sendConnectionEvent(record, conv.toAPIObject(p));
        }
    }

    private void sendConnectionEvent(@NonNull ConnectionRecord record, @NonNull PartnerAPI p) {
        // TODO both or either?
        messageService.sendMessage(WebSocketMessageBody.partnerReceived(p));
        if (isConnectionRequest(record)) {
            messageService.sendMessage(WebSocketMessageBody.partnerConnectionRequest(p));
        }
    }

    private boolean isConnectionRequest(ConnectionRecord connection) {
        return ConnectionAcceptance.MANUAL.equals(connection.getAccept())
                && ConnectionState.REQUEST.equals(connection.getState());
    }

    public void removeConnection(String connectionId) {
        log.debug("Removing connection: {}", connectionId);
        try {
            try {
                ac.connectionsRemove(connectionId);
            } catch (IOException | AriesException e) {
                log.warn("Could not delete aries connection.", e);
            }

            partnerRepo.findByConnectionId(connectionId).ifPresent(p -> {
                final List<PartnerProof> proofs = partnerProofRepo.findByPartnerId(p.getId());
                if (CollectionUtils.isNotEmpty(proofs)) {
                    partnerProofRepo.deleteAll(proofs);
                }
            });

            ac.presentProofRecords(PresentProofRecordsFilter
                    .builder()
                    .connectionId(connectionId)
                    .build()).ifPresent(records -> {
                        final List<String> toDelete = records.stream()
                                .map(PresentationExchangeRecord::getPresentationExchangeId)
                                .collect(Collectors.toList());
                        toDelete.forEach(presExId -> {
                            try {
                                ac.presentProofRecordsRemove(presExId);
                            } catch (IOException | AriesException e) {
                                log.error("Could not delete presentation exchange record: {}", presExId, e);
                            }
                        });
                    });

            myCredRepo.updateByConnectionId(connectionId, null);

        } catch (IOException e) {
            log.error("Could not delete connection: {}", connectionId, e);
        }
    }
}
