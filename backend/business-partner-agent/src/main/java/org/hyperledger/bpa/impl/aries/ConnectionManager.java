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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.CollectionUtils;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.acy_py.generated.model.InvitationCreateRequest;
import org.hyperledger.acy_py.generated.model.InvitationRecord;
import org.hyperledger.acy_py.generated.model.SendMessage;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.connection.*;
import org.hyperledger.aries.api.did_exchange.DidExchangeCreateRequestFilter;
import org.hyperledger.aries.api.exception.AriesException;
import org.hyperledger.aries.api.out_of_band.CreateInvitationFilter;
import org.hyperledger.aries.api.out_of_band.ReceiveInvitationFilter;
import org.hyperledger.aries.api.present_proof.PresentProofRecordsFilter;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord;
import org.hyperledger.bpa.api.exception.InvitationException;
import org.hyperledger.bpa.api.exception.NetworkException;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.controller.api.invitation.CheckInvitationResponse;
import org.hyperledger.bpa.controller.api.partner.CreatePartnerInvitationRequest;
import org.hyperledger.bpa.impl.InvitationParser;
import org.hyperledger.bpa.impl.activity.DidResolver;
import org.hyperledger.bpa.impl.notification.*;
import org.hyperledger.bpa.model.Partner;
import org.hyperledger.bpa.model.PartnerProof;
import org.hyperledger.bpa.model.Tag;
import org.hyperledger.bpa.repository.MyCredentialRepository;
import org.hyperledger.bpa.repository.PartnerProofRepository;
import org.hyperledger.bpa.repository.PartnerRepository;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class ConnectionManager {

    private static final String UNKNOWN_DID = "unknown";

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
    DidResolver didResolver;

    @Inject
    ObjectMapper mapper;

    @Inject
    BPAMessageSource.DefaultMessageSource messageSource;

    @Inject
    ApplicationEventPublisher eventPublisher;

    @Inject
    InvitationParser invitationParser;

    /**
     * Creates a connection invitation to be used within a barcode
     *
     * @param req {@link CreatePartnerInvitationRequest}
     * @return {@link CreateInvitationResponse}
     */
    public JsonNode createConnectionInvitation(@NonNull CreatePartnerInvitationRequest req) {
        Object invitation = null;
        String connId = null;
        String invMsgId = null;
        try {
            if (req.getUseOutOfBand()) {
                InvitationRecord oobInvitation = createOOBInvitation(req.getAlias());
                invitation = oobInvitation;
                invMsgId = oobInvitation.getInviMsgId();
            } else {
                CreateInvitationResponse conInvite = createInvitation(req.getAlias());
                invitation = conInvite;
                connId = conInvite.getConnectionId();
            }
            partnerRepo.save(Partner
                    .builder()
                    .ariesSupport(Boolean.TRUE)
                    .alias(StringUtils.trimToNull(req.getAlias()))
                    .connectionId(connId)
                    .invitationMsgId(invMsgId)
                    .did(didPrefix + UNKNOWN_DID)
                    .state(ConnectionState.INVITATION)
                    .incoming(Boolean.TRUE)
                    .tags(req.getTag() != null ? new HashSet<>(req.getTag()) : null)
                    .trustPing(req.getTrustPing() != null ? req.getTrustPing() : Boolean.FALSE)
                    .build());
        } catch (IOException e) {
            log.error("Could not create aries connection invitation", e);
        }
        return mapper.valueToTree(invitation);
    }

    public CheckInvitationResponse checkReceivedInvitation(@NonNull String invitationUrl) {
        return invitationParser.checkInvitation(invitationUrl);
    }

    public void receiveInvitation(@NonNull String encodedInvitation, @Nullable String alias,
            @Nullable List<Tag> tags, @Nullable Boolean trustPing) {
        InvitationParser.Invitation invitation = invitationParser.parseInvitation(encodedInvitation);
        if (invitation.isParsed()) {
            if (invitation.getInvitationRequest() != null) {
                try {
                    ac.connectionsReceiveInvitation(invitation.getInvitationRequest(),
                            ConnectionReceiveInvitationFilter.builder()
                                    .alias(invitation.getInvitationRequest().getLabel())
                                    .autoAccept(true)
                                    .build())
                            .ifPresent(persistPartner(alias, tags, trustPing));
                } catch (IOException e) {
                    String msg = messageSource.getMessage("acapy.unavailable");
                    log.error(msg, e);
                    throw new NetworkException(msg);
                }
            } else if (invitation.getInvitationMessage() != null) {
                try {
                    ac.outOfBandReceiveInvitation(invitation.getInvitationMessage(),
                            ReceiveInvitationFilter.builder()
                                    .alias(invitation.getInvitationMessage().getLabel())
                                    .autoAccept(true)
                                    .build())
                            .ifPresent(persistPartner(alias, tags, trustPing));
                } catch (IOException e) {
                    String msg = messageSource.getMessage("acapy.unavailable");
                    log.error(msg, e);
                    throw new NetworkException(msg);
                }
            }
        } else {
            throw new InvitationException(invitation.getError());
        }
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

    // manual connection flow
    public void acceptConnection(@NonNull String connectionId) {
        try {
            ac.didExchangeAcceptRequest(connectionId, null);
        } catch (IOException e) {
            String msg = messageSource.getMessage("acapy.unavailable");
            log.error(msg, e);
            throw new NetworkException(msg);
        }
    }

    // connection that originated from this agent
    public void handleOutgoingConnectionEvent(ConnectionRecord record) {
        partnerRepo.findByConnectionId(record.getConnectionId()).ifPresent(
                dbP -> {
                    if (StringUtils.isEmpty(dbP.getLabel())) {
                        dbP.setLabel(record.getTheirLabel());
                        dbP.setState(record.getState());
                        partnerRepo.update(dbP);
                    } else {
                        partnerRepo.updateState(dbP.getId(), record.getState());
                    }
                    if (ConnectionState.REQUEST.equals(record.getState())) {
                        eventPublisher.publishEventAsync(PartnerAddedEvent.builder().partner(dbP).build());
                    } else if (ConnectionState.RESPONSE.equals(record.getState()) ||
                            ConnectionState.COMPLETED.equals(record.getState())) {
                        eventPublisher.publishEventAsync(PartnerAcceptedEvent.builder().partner(dbP).build());
                    }
                });
    }

    // handles invitations and incoming connection events
    public void handleIncomingConnectionEvent(ConnectionRecord record) {
        partnerRepo.findByConnectionId(record.getConnectionId()).ifPresentOrElse(
                dbP -> {
                    if (StringUtils.isEmpty(dbP.getLabel())) {
                        dbP.setLabel(record.getTheirLabel());
                    }
                    if (StringUtils.isEmpty(dbP.getDid()) || dbP.getDid().endsWith(UNKNOWN_DID)) {
                        dbP.setDid(didPrefix + record.getTheirDid());
                    }
                    dbP.setState(record.getState());
                    partnerRepo.update(dbP);
                    resolveAndSend(record, dbP);
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
                            .trustPing(Boolean.TRUE)
                            .build();
                    p = partnerRepo.save(p);
                    resolveAndSend(record, p);
                });
    }

    public void handleOOBInvitation(ConnectionRecord record) {
        partnerRepo.findByInvitationMsgId(record.getInvitationMsgId()).ifPresent(dbP -> {
            if (StringUtils.isEmpty(dbP.getConnectionId())) {
                dbP.setConnectionId(record.getConnectionId());
                dbP.setDid(didPrefix + record.getTheirDid());
                dbP.setState(record.getState());
                dbP.setLabel(record.getTheirLabel());
                partnerRepo.update(dbP);
            } else {
                partnerRepo.updateState(dbP.getId(), record.getState());
            }
            resolveAndSend(record, dbP);
        });
    }

    private void resolveAndSend(ConnectionRecord record, Partner p) {
        // only incoming connections in state request
        if (ConnectionState.REQUEST.equals(record.getState())) {
            didResolver.lookupIncoming(p);
            if (record.isIncomingConnection()) {
                eventPublisher.publishEventAsync(PartnerRequestReceivedEvent.builder().partner(p).build());
            }
        } else if (ConnectionState.COMPLETED.equals(record.getState()) && record.isIncomingConnection()) {
            eventPublisher.publishEventAsync(PartnerRequestCompletedEvent.builder().partner(p).build());
        }
    }

    public void removeConnection(String connectionId) {
        log.debug("Removing connection: {}", connectionId);
        try {
            try {
                ac.connectionsRemove(connectionId);
            } catch (IOException | AriesException e) {
                log.warn("Could not delete aries connection.", e);
            }

            Optional<Partner> partner = partnerRepo.findByConnectionId(connectionId);
            partner.ifPresent(p -> {
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
            partner.ifPresent(value -> eventPublisher
                    .publishEventAsync(PartnerRemovedEvent.builder().partner(value).build()));
        } catch (IOException e) {
            log.error("Could not delete connection: {}", connectionId, e);
        }
    }

    public boolean sendMessage(String connectionId, String content) {
        if (StringUtils.isNotEmpty(content)) {
            try {
                ac.connectionsSendMessage(connectionId,
                        SendMessage.builder().content(content).build());
                return true;
            } catch (IOException e) {
                log.error("Could not send message to connection: {}", connectionId, e);
            }
        }
        return false;
    }

    private InvitationRecord createOOBInvitation(@Nullable String alias) throws IOException {
        return ac.outOfBandCreateInvitation(
                InvitationCreateRequest.builder()
                        .alias(alias)
                        .usePublicDid(Boolean.TRUE)
                        .handshakeProtocols(List.of(ConnectionRecord.ConnectionProtocol.DID_EXCHANGE_V1.getValue()))
                        .build(),
                CreateInvitationFilter.builder()
                        .autoAccept(Boolean.TRUE)
                        .build())
                .orElseThrow();
    }

    private CreateInvitationResponse createInvitation(@Nullable String alias) throws IOException {
        return ac.connectionsCreateInvitation(
                CreateInvitationRequest.builder()
                        .build(),
                CreateInvitationParams.builder()
                        .alias(alias)
                        .autoAccept(Boolean.TRUE)
                        .build())
                .orElseThrow();
    }

    private Consumer<ConnectionRecord> persistPartner(
            @Nullable String alias, @Nullable List<Tag> tags, @Nullable Boolean trustPing) {
        return connectionRecord -> partnerRepo.save(Partner
                .builder()
                .ariesSupport(Boolean.TRUE)
                .alias(StringUtils.trimToNull(alias))
                .connectionId(connectionRecord.getConnectionId())
                .invitationMsgId(connectionRecord.getInvitationMsgId())
                .did(didPrefix + UNKNOWN_DID)
                .state(ConnectionState.INVITATION)
                .incoming(Boolean.TRUE)
                .tags(tags != null ? new HashSet<>(tags) : null)
                .trustPing(trustPing != null ? trustPing : Boolean.TRUE)
                .build());
    }
}
