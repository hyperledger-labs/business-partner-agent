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
package org.hyperledger.oa.impl.aries;

import io.micronaut.context.annotation.Value;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.scheduling.annotation.Async;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.connection.ConnectionRecord;
import org.hyperledger.aries.api.connection.ReceiveInvitationRequest;
import org.hyperledger.aries.api.exception.AriesException;
import org.hyperledger.aries.api.proof.PresentProofRecordsFilter;
import org.hyperledger.aries.api.proof.PresentationExchangeRecord;
import org.hyperledger.oa.config.runtime.RequiresAries;
import org.hyperledger.oa.controller.api.WebSocketMessageBody;
import org.hyperledger.oa.impl.MessageService;
import org.hyperledger.oa.impl.activity.DidResolver;
import org.hyperledger.oa.impl.util.AriesStringUtil;
import org.hyperledger.oa.impl.util.Converter;
import org.hyperledger.oa.model.Partner;
import org.hyperledger.oa.model.PartnerProof;
import org.hyperledger.oa.repository.MyCredentialRepository;
import org.hyperledger.oa.repository.PartnerProofRepository;
import org.hyperledger.oa.repository.PartnerRepository;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Singleton
@RequiresAries
public class ConnectionManager {

    @Value("${oagent.did.prefix}")
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

    @Async
    public void createConnection(@NonNull String did, @NonNull String label, @Nullable String alias) {
        try {
            ac.connectionsReceiveInvitation(
                    ReceiveInvitationRequest.builder()
                            .did(AriesStringUtil.getLastSegment(did))
                            .label(label)
                            .build(),
                    alias);
        } catch (IOException e) {
            log.error("Could not create aries connection", e);
        }
    }

    public synchronized void handleConnectionEvent(ConnectionRecord connection) {
        if (connection.isIncomingConnection()) {
            partnerRepo.findByConnectionId(connection.getConnectionId())
                    .ifPresentOrElse(dbP -> partnerRepo.updateState(dbP.getId(), connection.getState()), () -> {
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
                    });
        } else {
            partnerRepo.findByLabel(connection.getTheirLabel()).ifPresent(dbP -> {
                if (dbP.getConnectionId() == null) {
                    dbP.setConnectionId(connection.getConnectionId());
                    dbP.setState(connection.getState());
                    partnerRepo.update(dbP);
                } else {
                    partnerRepo.updateState(dbP.getId(), connection.getState());
                }
            });
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
