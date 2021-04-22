/*
  Copyright (c) 2020 - for information on the respective copyright owner
  see the NOTICE file and/or the repository at
  https://github.com/hyperledger-labs/business-partner-agent

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package org.hyperledger.bpa.impl.aries;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.scheduling.annotation.Async;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.connection.ConnectionAcceptRequestFilter;
import org.hyperledger.aries.api.connection.ConnectionReceiveInvitationFilter;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.connection.ReceiveInvitationRequest;
import org.hyperledger.aries.api.exception.AriesException;
import org.hyperledger.aries.api.ledger.EndpointType;
import org.hyperledger.aries.api.present_proof.PresentProofRecordsFilter;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord;
import org.hyperledger.bpa.api.ApiConstants;
import org.hyperledger.bpa.api.DidDocAPI;
import org.hyperledger.bpa.api.exception.NetworkException;
import org.hyperledger.bpa.controller.api.WebSocketMessageBody;
import org.hyperledger.bpa.impl.MessageService;
import org.hyperledger.bpa.impl.activity.DidResolver;
import org.hyperledger.bpa.impl.util.AriesStringUtil;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.model.Partner;
import org.hyperledger.bpa.model.PartnerProof;
import org.hyperledger.bpa.repository.MyCredentialRepository;
import org.hyperledger.bpa.repository.PartnerProofRepository;
import org.hyperledger.bpa.repository.PartnerRepository;

import io.micronaut.core.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class ConnectionManager {

    private static final String ACA_PY_ERROR_MSG = "aca-py not available";

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
    ObjectMapper mapper;

    /**
     * Create a connection based on a public did that is registered on a ledger.
     * 
     * @param did   the did like did:iil:123
     * @param label the connection label
     * @param alias optional connection alias
     */
    @Async
    public void createConnection(@NonNull String did, @NonNull String label, @Nullable String alias) {
        try {
            ac.connectionsReceiveInvitation(
                    ReceiveInvitationRequest.builder()
                            .did(AriesStringUtil.getLastSegment(did))
                            .label(label)
                            .build(),
                    ConnectionReceiveInvitationFilter.builder().alias(alias).autoAccept(Boolean.TRUE).build());
        } catch (IOException e) {
            log.error(ACA_PY_ERROR_MSG, e);
        }
    }

    /**
     * Create a connection based on information that is found in the partners did
     * document. Requires at least the endpoint and a verification method to be
     * present in the did document.
     * 
     * @param didDoc {@link DidDocAPI}
     * @param label  the connection label
     * @param alias  optional connection alias
     */
    @Async
    public void createConnection(@NonNull DidDocAPI didDoc, @NonNull String label, @Nullable String alias) {
        // resolve endpoint
        String endpoint = null;
        Optional<DidDocAPI.Service> acaPyEndpoint = didDoc.getService()
                .stream()
                .filter(s -> EndpointType.ENDPOINT.getLedgerName().equals(s.getType()))
                .findFirst();
        if (acaPyEndpoint.isPresent() && StringUtils.isNotEmpty(acaPyEndpoint.get().getServiceEndpoint())) {
            endpoint = acaPyEndpoint.get().getServiceEndpoint();
        } else {
            log.warn("No aca-py endpoint found in the partners did document.");
        }

        // resolve public key
        String pk = null;
        Optional<DidDocAPI.VerificationMethod> verificationMethod = didDoc.getVerificationMethod(mapper)
                .stream()
                .filter(m -> ApiConstants.DEFAULT_VERIFICATION_KEY_TYPE.equals(m.getType()))
                .findFirst();
        if (verificationMethod.isPresent() && StringUtils.isNotEmpty(verificationMethod.get().getPublicKeyBase58())) {
            pk = verificationMethod.get().getPublicKeyBase58();
        } else {
            log.warn("No public key found in the partners did document.");
        }

        try {
            if (endpoint != null && pk != null) {
                ac.connectionsReceiveInvitation(
                        ReceiveInvitationRequest.builder()
                                .serviceEndpoint(endpoint)
                                .recipientKeys(List.of(pk))
                                .label(label)
                                .build(),
                        ConnectionReceiveInvitationFilter.builder().alias(alias).autoAccept(Boolean.TRUE).build());
            }
        } catch (IOException e) {
            log.error(ACA_PY_ERROR_MSG, e);
        }
    }

    public void acceptConnection(@NonNull String connectionId) {
        try {
            ac.connectionsAcceptRequest(connectionId, ConnectionAcceptRequestFilter.builder().build());
        } catch (IOException e) {
            log.error(ACA_PY_ERROR_MSG, e);
            throw new NetworkException(ACA_PY_ERROR_MSG);
        }
    }

    public synchronized void handleConnectionEvent(ConnectionRecord connection) {
        partnerRepo.findByLabel(connection.getTheirLabel()).ifPresentOrElse(
                // connection that originated from this agent
                dbP -> {
                    if (dbP.getConnectionId() == null) {
                        dbP.setConnectionId(connection.getConnectionId());
                        dbP.setState(connection.getState());
                        partnerRepo.update(dbP);
                    } else {
                        partnerRepo.updateState(dbP.getId(), connection.getState());
                    }
                },
                // connection initiated externally
                () -> partnerRepo.findByConnectionId(connection.getConnectionId()).ifPresentOrElse(
                        dbP -> partnerRepo.updateState(dbP.getId(), connection.getState()),
                        () -> {
                            Partner p = Partner
                                    .builder()
                                    .ariesSupport(Boolean.TRUE)
                                    .alias(connection.getTheirLabel()) // event has no alias in this case
                                    .connectionId(connection.getConnectionId())
                                    .did(didPrefix + connection.getTheirDid())
                                    .label(connection.getTheirLabel())
                                    .state(connection.getState())
                                    .incoming(Boolean.TRUE)
                                    .build();
                            p = partnerRepo.save(p);
                            didResolver.lookupIncoming(p);
                            messageService.sendMessage(WebSocketMessageBody.partnerReceived(conv.toAPIObject(p)));
                        }));
    }

    public void handleConnectionRequest(@NonNull String connectionId) {
        partnerRepo.findByConnectionId(connectionId).ifPresent(p -> messageService
                .sendMessage(WebSocketMessageBody.partnerConnectionRequest(conv.toAPIObject(p))));
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
