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
import io.micronaut.scheduling.annotation.Scheduled;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.connection.ConnectionFilter;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.aries.api.message.PingEvent;
import org.hyperledger.aries.api.message.PingRequest;
import org.hyperledger.bpa.repository.PartnerRepository;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Singleton
@Requires(notEnv = { Environment.TEST })
public class PingManager {

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
        if ("response_received".equals(event.getState())) {
            received.put(event.getThreadId(), "received");
        }
    }

    @Scheduled(fixedRate = "1m", initialDelay = "90s") // init delay needs to be > than aca-py connection timeout
    public void checkConnections() {
        try {
            List<String> activeConnections = aries.connectionIds(
                    ConnectionFilter.builder().state(ConnectionState.ACTIVE).build());
            if (CollectionUtils.isNotEmpty(activeConnections)) {
                if (!firstRun) {
                    setNewState();
                }
                sendPingToActiveConnections(activeConnections);
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
                state = ConnectionState.ACTIVE;
                repo.updateStateAndLastSeenByConnectionId(v, state, Instant.now());
            } else {
                state = ConnectionState.INACTIVE;
                repo.updateStateByConnectionId(v, state);
            }
        });
        sent.clear();
        received.clear();
    }

    private void sendPingToActiveConnections(List<String> activeConnections) {
        try {
            for (String connectionId : activeConnections) {
                log.debug("Sending ping to: {}", connectionId);
                aries.connectionsSendPing(connectionId, new PingRequest(connectionId))
                        .ifPresent(resp -> sent.put(resp.getThreadId(), connectionId));
            }
        } catch (IOException e) {
            log.error("Could not ping active connections", e);
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
        List<String> bpaConIds = new ArrayList<>();
        repo.findAll().forEach(p -> bpaConIds.add(p.getConnectionId()));

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
