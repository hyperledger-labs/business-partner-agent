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
package org.hyperledger.bpa.impl.aries.connection;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Value;
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
import org.hyperledger.acy_py.generated.model.InvitationRecord;
import org.hyperledger.acy_py.generated.model.SendMessage;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.connection.*;
import org.hyperledger.aries.api.did_exchange.DidExchangeCreateRequestFilter;
import org.hyperledger.aries.api.exception.AriesException;
import org.hyperledger.aries.api.issue_credential_v1.IssueCredentialRecordsFilter;
import org.hyperledger.aries.api.issue_credential_v2.V2IssueCredentialRecordsFilter;
import org.hyperledger.aries.api.out_of_band.CreateInvitationFilter;
import org.hyperledger.aries.api.out_of_band.InvitationCreateRequest;
import org.hyperledger.aries.api.out_of_band.ReceiveInvitationFilter;
import org.hyperledger.bpa.api.exception.EntityNotFoundException;
import org.hyperledger.bpa.api.exception.InvitationException;
import org.hyperledger.bpa.api.exception.NetworkException;
import org.hyperledger.bpa.api.notification.*;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.controller.api.invitation.APICreateInvitationResponse;
import org.hyperledger.bpa.controller.api.invitation.CheckInvitationResponse;
import org.hyperledger.bpa.controller.api.partner.CreatePartnerInvitationRequest;
import org.hyperledger.bpa.impl.activity.DidResolver;
import org.hyperledger.bpa.impl.activity.PartnerCredDefLookup;
import org.hyperledger.bpa.impl.util.AriesStringUtil;
import org.hyperledger.bpa.impl.util.TimeUtil;
import org.hyperledger.bpa.persistence.model.Partner;
import org.hyperledger.bpa.persistence.model.PartnerProof;
import org.hyperledger.bpa.persistence.model.Tag;
import org.hyperledger.bpa.persistence.repository.HolderCredExRepository;
import org.hyperledger.bpa.persistence.repository.PartnerProofRepository;
import org.hyperledger.bpa.persistence.repository.PartnerRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@Slf4j
@Singleton
public class ConnectionManager {

    public static final String UNKNOWN_DID = "unknown";

    @Value("${bpa.did.prefix}")
    String didPrefix;

    @Inject
    AriesClient ac;

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    PartnerProofRepository partnerProofRepo;

    @Inject
    HolderCredExRepository holderCredExRepo;

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

    @Inject
    PartnerCredDefLookup partnerCredDefLookup;

    /**
     * Creates a connection invitation to be used within a barcode
     *
     * @param req {@link CreatePartnerInvitationRequest}
     * @return {@link APICreateInvitationResponse}
     */
    public APICreateInvitationResponse createConnectionInvitation(@NonNull CreatePartnerInvitationRequest req) {
        APICreateInvitationResponse.APICreateInvitationResponseBuilder invitation = APICreateInvitationResponse
                .builder();
        String connId = null;
        String invMsgId = null;
        InvitationRecord invitationRecord;
        try {
            if (req.getUseOutOfBand()) {
                invitationRecord = createOOBInvitation(req.getAlias(), req.getUsePublicDid());
                invMsgId = invitationRecord.getInviMsgId();
                invitation
                        .invitationUrl(invitationRecord.getInvitationUrl())
                        .invitationId(invMsgId);
            } else {
                CreateInvitationResponse conInvite = createInvitation(req.getAlias());
                connId = conInvite.getConnectionId();
                invitationRecord = InvitationRecord.builder().invitationUrl(conInvite.getInvitationUrl()).build();
                invitation
                        .invitationUrl(invitationRecord.getInvitationUrl())
                        .invitationId(connId);
            }
            Partner partner = createNewPartner(req.getAlias(), connId, invMsgId, req.getTag(), req.getTrustPing(),
                    invitationRecord);
            invitation.partnerId(partner.getId().toString());
        } catch (IOException e) {
            throw new NetworkException("acapy.unavailable");
        }
        return invitation.build();
    }

    public CheckInvitationResponse checkReceivedInvitation(@NonNull String invitationUri) {
        return invitationParser.checkInvitation(invitationUri);
    }

    public void receiveInvitation(@NonNull String encodedInvitation, @Nullable String alias,
            @Nullable List<Tag> tags, @Nullable Boolean trustPing) {
        InvitationParser.Invitation invitation = invitationParser.parseInvitation(encodedInvitation);
        if (invitation.isParsed()) {
            if (invitation.getInvitationRequest() != null) {
                try {
                    String invMsgId = invitation.getInvitationRequest().getId();
                    createNewPartner(alias, invMsgId, tags, trustPing);
                    ac.connectionsReceiveInvitation(invitation.getInvitationRequest(),
                            ConnectionReceiveInvitationFilter.builder()
                                    .alias(invitation.getInvitationRequest().getLabel())
                                    .autoAccept(true)
                                    .build());
                } catch (IOException e) {
                    String msg = messageSource.getMessage("acapy.unavailable");
                    log.error(msg, e);
                    throw new NetworkException(msg);
                }
            } else if (invitation.getInvitationMessage() != null) {
                try {
                    String invMsgId = invitation.getInvitationMessage().getAtId();
                    createNewPartner(alias, invMsgId, tags, trustPing);
                    ac.outOfBandReceiveInvitation(invitation.getInvitationMessage(),
                            ReceiveInvitationFilter.builder()
                                    .alias(invitation.getInvitationMessage().getLabel())
                                    .autoAccept(true)
                                    .useExistingConnection(true)
                                    .build());
                } catch (IOException e) {
                    String msg = messageSource.getMessage("acapy.unavailable");
                    log.error(msg, e);
                    throw new NetworkException(msg);
                } catch (AriesException e) {
                    // if there is an attachment ignore any aca-py exception
                    if (CollectionUtils.isEmpty(invitation.getInvitationMessage().getRequestsAttach())) {
                        throw new InvitationException(e.getMessage());
                    }
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
            ConnectionRecord con = ac.connectionsGetById(connectionId).orElseThrow(EntityNotFoundException::new);
            if (ConnectionRecord.ConnectionProtocol.DID_EXCHANGE_V1.equals(con.getConnectionProtocol())) {
                ac.didExchangeAcceptRequest(connectionId, null);
            } else {
                ac.connectionsAcceptRequest(connectionId, null);
            }
        } catch (IOException e) {
            String errorMsg = messageSource.getMessage("acapy.unavailable");
            log.error(errorMsg, e);
            throw new NetworkException(errorMsg);
        }
    }

    // connection that originated from this agent
    public void handleOutgoingConnectionEvent(ConnectionRecord record) {
        partnerRepo.findByConnectionId(record.getConnectionId()).ifPresent(
                dbP -> {
                    dbP.pushStates(record.getState(), record.getUpdatedAt());
                    if (StringUtils.isEmpty(dbP.getLabel())) {
                        partnerRepo.updateStateAndLabel(
                                dbP.getId(), dbP.getState(), dbP.getStateToTimestamp(), record.getTheirLabel());
                    } else {
                        partnerRepo.updateState(
                                dbP.getId(), dbP.getState(), dbP.getStateToTimestamp());
                    }
                    if (record.stateIsRequest()) {
                        eventPublisher.publishEventAsync(PartnerAddedEvent.builder().partner(dbP).build());
                    } else if (record.stateIsResponse() || record.stateIsCompleted()) {
                        eventPublisher.publishEventAsync(PartnerAcceptedEvent.builder().partner(dbP).build());
                    }
                });
    }

    // handles invitations and incoming connection events
    public void handleIncomingConnectionEvent(ConnectionRecord record) {
        Optional<Partner> partner;
        if (StringUtils.isNotEmpty(record.getInvitationMsgId())) {
            partner = partnerRepo.findByConnectionIdOrInvitationMsgId(record.getConnectionId(),
                    record.getInvitationMsgId());
        } else {
            partner = partnerRepo.findByConnectionId(record.getConnectionId());
        }
        partner.ifPresentOrElse(
                dbP -> {
                    if (StringUtils.isEmpty(dbP.getLabel())) {
                        dbP.setLabel(record.getTheirLabel());
                    }
                    if (StringUtils.isEmpty(dbP.getDid()) || dbP.getDid().endsWith(UNKNOWN_DID)) {
                        dbP.setDid(resolveDidFromRecord(record));
                    }
                    if (StringUtils.isEmpty(dbP.getConnectionId())) {
                        dbP.setConnectionId(record.getConnectionId());
                    }
                    dbP.pushStates(record.getState(), record.getUpdatedAt());
                    partnerRepo.update(dbP);
                    resolveAndSend(record, dbP);
                },
                () -> {
                    Partner p = Partner
                            .builder()
                            .ariesSupport(Boolean.TRUE)
                            .connectionId(record.getConnectionId())
                            .did(resolveDidFromRecord(record))
                            .state(record.getState())
                            .pushStateChange(record.getState(), TimeUtil.fromISOInstant(record.getUpdatedAt()))
                            .label(record.getTheirLabel())
                            .incoming(Boolean.TRUE)
                            .trustPing(Boolean.TRUE)
                            .invitationMsgId(record.getInvitationMsgId())
                            .build();
                    p = partnerRepo.save(p);
                    resolveAndSend(record, p);
                });
    }

    String resolveDidFromRecord(@NonNull ConnectionRecord record) {
        String did = null;
        if (record.protocolIsIdDidExchangeV1() && !record.isInvitationRequest()) {
            did = AriesStringUtil.qualifyDidIfNeeded(StringUtils.isNotEmpty(record.getTheirPublicDid())
                    ? record.getTheirPublicDid()
                    : record.getTheirDid(), didPrefix);
        }
        if (StringUtils.isEmpty(did)) {
            did = AriesStringUtil.qualifyDidIfNeeded(record.getTheirDid(), "did:peer:");
        }
        if (StringUtils.isEmpty(did)) {
            did = UNKNOWN_DID;
        }
        return did;
    }

    /**
     * Handle received invitation events, meaning QR code or URLs created elsewhere
     * but received here. When using the connection protocol all DIDs are peer DIDs
     *
     * @param record {@link ConnectionRecord}
     */
    public void handleInvitationEvent(ConnectionRecord record) {
        partnerRepo.findByInvitationMsgId(record.getInvitationMsgId()).ifPresent(dbP -> {
            dbP.pushStates(record.getState(), record.getUpdatedAt());
            dbP.setConnectionId(record.getConnectionId());
            dbP.setDid(resolveDidFromRecord(record));
            dbP.setLabel(record.getTheirLabel());
            partnerRepo.update(dbP);
            resolveAndSend(record, dbP);
        });
    }

    private void resolveAndSend(ConnectionRecord record, Partner p) {
        if (record.isInvitationResponse()) {
            // handle Connection Invitations...
            // if we generate, and they accept, we do not get a COMPLETED or ACTIVE state,
            // only get to RESPONSE
            // if they generate, and we accept, we may get to ACTIVE, but definitely get to
            // RESPONSE
            // so consider RESPONSE as we are connected, just add a completed task saying
            // connection accepted.
            if (record.stateIsResponse()) {
                eventPublisher.publishEventAsync(PartnerRequestCompletedEvent.builder().partner(p).build());
            }
        } else if (record.stateIsRequest()) {
            didResolver.lookupIncoming(p);
            if (record.isIncomingConnection()) {
                eventPublisher.publishEventAsync(PartnerRequestReceivedEvent.builder().partner(p).build());
                partnerRepo.updateInvitationRecord(p.getId(), null); // reset invitation, if there is one
            }
        } else if (record.stateIsActive() && record.isIncomingConnection()) {
            eventPublisher.publishEventAsync(PartnerRequestCompletedEvent.builder().partner(p).build());
            partnerCredDefLookup.lookupTypesForAllPartnersAsync();
        }
    }

    public void removeConnection(@NonNull Partner partner) {
        if (StringUtils.isNotEmpty(partner.getConnectionId())) {
            String connectionId = partner.getConnectionId();
            log.debug("Removing aca-py connection: {}", connectionId);
            try {
                try {
                    ac.connectionsRemove(connectionId);
                } catch (AriesException e) {
                    log.warn("Could not delete aca-py connection.", e);
                }

                log.debug("Removing all aca-py v1 and v2 presentation exchanges");
                Page<PartnerProof> partnerProofs = partnerProofRepo.findAll(Pageable.from(0, 10));
                do {
                    partnerProofs = deletePresentationExchanges(partnerProofs);
                } while (partnerProofs.getNumberOfElements() > 0);

                log.debug("Removing all aca-py v1 and v2 credential exchanges");
                ac.issueCredentialRecords(IssueCredentialRecordsFilter
                        .builder()
                        .connectionId(connectionId)
                        .build()).ifPresent(records -> records.forEach(record -> {
                            try {
                                ac.issueCredentialRecordsRemove(record.getCredentialExchangeId());
                            } catch (IOException | AriesException e) {
                                log.error("Could not delete credential exchange record: {}",
                                        record.getCredentialExchangeId(), e);
                            }
                        }));
                ac.issueCredentialV2Records(V2IssueCredentialRecordsFilter
                        .builder()
                        .connectionId(connectionId)
                        .build()).ifPresent(records -> records.forEach(record -> {
                            try {
                                ac.issueCredentialV2RecordsRemove(record.getCredExRecord().getCredExId());
                            } catch (IOException | AriesException e) {
                                log.error("Could not delete credential exchange record: {}",
                                        record.getCredExRecord().getCredExId(), e);
                            }
                        }));
            } catch (IOException e) {
                log.error("aca-py not available", e);
            }
        }

        holderCredExRepo.setPartnerIdToNull(partner.getId());
        partnerProofRepo.deleteByPartnerId(partner.getId());

        eventPublisher.publishEventAsync(PartnerRemovedEvent.builder().partner(partner).build());
    }

    private Page<PartnerProof> deletePresentationExchanges(Page<PartnerProof> page) {
        page.forEach(p -> {
            try {
                if (p.exchangeIsV1()) {
                    ac.presentProofRecordsRemove(p.getPresentationExchangeId());
                } else if (p.exchangeIsV2()) {
                    ac.presentProofV2RecordsRemove(p.getPresentationExchangeId());
                }
            } catch (IOException | AriesException e) {
                log.error("Could not delete presentation exchange record: {}",
                        p.getPresentationExchangeId(), e);
            }
        });
        return partnerProofRepo.findAll(page.nextPageable());
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

    private InvitationRecord createOOBInvitation(@Nullable String alias, boolean usePublicDid) throws IOException {
        return ac.outOfBandCreateInvitation(
                InvitationCreateRequest.builder()
                        .alias(alias)
                        .usePublicDid(usePublicDid)
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

    private Partner createNewPartner(String alias, String invMsgId, List<Tag> tag,
            Boolean trustPing) {
        return createNewPartner(alias, null, invMsgId, tag, trustPing, null);
    }

    private Partner createNewPartner(String alias, String connectionId, String invMsgId, List<Tag> tag,
            Boolean trustPing, InvitationRecord invitationRecord) {
        return partnerRepo.save(Partner
                .builder()
                .ariesSupport(Boolean.TRUE)
                .alias(StringUtils.trimToNull(alias))
                .connectionId(connectionId)
                .invitationMsgId(invMsgId)
                .invitationRecord(invitationRecord)
                .did(didPrefix + UNKNOWN_DID)
                .state(ConnectionState.INVITATION)
                .pushStateChange(ConnectionState.INVITATION, Instant.now())
                .incoming(Boolean.TRUE)
                .tags(tag != null ? new HashSet<>(tag) : null)
                .trustPing(trustPing != null ? trustPing : Boolean.FALSE)
                .build());
    }
}
