/**
 * Copyright (c) 2020 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/organizational-agent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hyperledger.oa.impl;

import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.ledger.EndpointResponse;
import org.hyperledger.aries.api.ledger.EndpointType;
import org.hyperledger.aries.api.wallet.SetDidEndpointRequest;

import io.micronaut.context.annotation.Value;
import lombok.extern.slf4j.Slf4j;

/**
 * Service which registers endpoints on the ledger.
 * 
 * Either called in the start-up phases of the business partner agent or later,
 * triggered by the frontend (if TAA acceptance from user is required)
 */
@Slf4j
@Singleton
public class EndpointService {

    @Value("${oagent.agent.endpoint}")
    private String agentEndpoint;

    @Value("${oagent.host}")
    private String host;

    @Inject
    private AriesClient ac;

    public void registerEndpoints() {
        // register profile endpoint
        final String endpoint = "https://" + host + "/profile.jsonld";
        EndpointType type = EndpointType.Profile;
        registerProfileEndpoint(endpoint, type);

        // register aries endpoint
        type = EndpointType.Endpoint;
        registerProfileEndpoint(agentEndpoint, type);
    }

    private void registerProfileEndpoint(String endpoint, EndpointType type) {
        try {
            ac.walletDidPublic().ifPresentOrElse(res -> {
                try {

                    final Optional<EndpointResponse> existingEndpoint = ac.ledgerDidEndpoint(
                            res.getDid(), type);
                    if (existingEndpoint.isEmpty() || StringUtils.isEmpty(existingEndpoint.get().getEndpoint())
                            || existingEndpoint.isPresent() && !endpoint.equals(existingEndpoint.get().getEndpoint())) {
                        log.info("Publishing public '{}' endpoint: {}", type, endpoint);
                        ac.walletSetDidEndpoint(SetDidEndpointRequest
                                .builder()
                                .did(res.getDid())
                                .endpointType(type)
                                .endpoint(endpoint)
                                .build());
                    } else {
                        log.info("Endpoint found on the ledger, skipping: {}",
                                existingEndpoint.get().getEndpoint());
                    }
                } catch (Exception e) {
                    log.error("Could not publish the '{}' endpoint", type, e);
                }

            }, () -> {
                log.warn("No public did available, no '{}' endpoint is published", type);
            });
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
