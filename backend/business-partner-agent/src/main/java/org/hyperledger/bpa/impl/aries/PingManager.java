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

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.aries.api.exception.AriesException;
import org.hyperledger.aries.api.message.PingEvent;
import org.hyperledger.aries.api.message.PingRequest;
import org.hyperledger.bpa.model.Partner;
import org.hyperledger.bpa.repository.PartnerRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Singleton
@Requires(notEnv = { Environment.TEST })
public class PingManager {

    final static List<ConnectionState> statesToFilter = List.of(
            ConnectionState.ACTIVE, ConnectionState.COMPLETED,
            ConnectionState.PING_RESPONSE, ConnectionState.PING_NO_RESPONSE);

    @Inject
    AriesClient aries;

    @Inject
    PartnerRepository repo;

    private boolean firstRun;

    public PingManager() {
        super();
        this.firstRun = true;
    }

    // threadId, connectionId
    private final Map<String, String> sent = new ConcurrentHashMap<>();

    // threadId, state
    private final Map<String, String> received = new ConcurrentHashMap<>();

    public void handlePingEvent(PingEvent event) {
        if (event.stateIsReceived() && event.hasResponded()) {
            received.put(event.getThreadId(), "received");
        }
    }

    @Scheduled(fixedRate = "1m", initialDelay = "90s") // init delay needs to be > than aca-py connection timeout
    public void checkConnections() {
        try {
            List<String> connectionsToPing = repo
                    .findByStateInAndTrustPingTrueAndAriesSupportTrue(statesToFilter)
                    .stream().map(Partner::getConnectionId).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(connectionsToPing)) {
                if (!firstRun) {
                    setNewState();
                }
                sendPingToConnections(connectionsToPing);
            }
            if (firstRun) {
                firstRun = false;
            }
        } catch (Exception e) {
            log.error("Trust ping job failed.", e);
        }
    }

    private void setNewState() {
        sent.forEach((k, v) -> {
            ConnectionState state;
            if (received.containsKey(k)) {
                state = ConnectionState.PING_RESPONSE;
                repo.updateStateAndLastSeenByConnectionId(v, state, Instant.now());
            } else {
                state = ConnectionState.PING_NO_RESPONSE;
                repo.updateStateByConnectionId(v, state);
            }
        });
        sent.clear();
        received.clear();
    }

    private void sendPingToConnections(List<String> connectionsToPing) {
        try {
            for (String connectionId : connectionsToPing) {
                log.debug("Sending ping to: {}", connectionId);
                try {
                    aries.connectionsSendPing(connectionId, new PingRequest(connectionId))
                            .ifPresent(resp -> sent.put(resp.getThreadId(), connectionId));
                } catch (AriesException e) {
                    if (e.getCode() == 404) {
                        log.error("Connection id {} exists in the BPA but not in aca-py", connectionId);
                        repo.updateStateByConnectionId(connectionId, ConnectionState.PING_NO_RESPONSE);
                    } else {
                        log.error("Could not send ping request to connection {}", connectionId, e);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Could not ping active connections, because aca-py is not available", e);
        }
    }

    int getSentSize() {
        return sent.size();
    }

    int getReceivedSize() {
        return received.size();
    }

    @Scheduled(fixedRate = "30m", initialDelay = "1m")
    public void deleteStaleConnections() {
        List<String> bpaConIds = StreamSupport.stream(repo.findAll().spliterator(), false)
                .map(Partner::getConnectionId)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toList());

        try {
            List<String> acaConIds = aries.connectionIds();
            List<String> stale = acaConIds
                    .stream()
                    .filter(acaId -> bpaConIds
                            .stream()
                            .noneMatch(bpaId -> bpaId.equals(acaId)))
                    .collect(Collectors.toList());
            for (String conId : stale) {
                aries.connectionsRemove(conId);
            }
        } catch (IOException e) {
            log.error("aca-py not reachable.", e);
        }

    }
}
