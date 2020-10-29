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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.ledger.EndpointResponse;
import org.hyperledger.aries.api.ledger.EndpointType;
import org.hyperledger.aries.api.ledger.TAAAccept;
import org.hyperledger.aries.api.ledger.TAAInfo;
import org.hyperledger.aries.api.ledger.TAAInfo.TAARecord;
import org.hyperledger.aries.api.wallet.SetDidEndpointRequest;
import org.hyperledger.aries.api.wallet.WalletDidResponse;
import org.hyperledger.oa.api.exception.NetworkException;

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

    private boolean endpointRegistrationRequired = false;

    private final Map<String, EndpointType> endpoints;

    public EndpointService() {
        endpoints = new HashMap<String, EndpointType>();
        endpoints.put("https://" + host + "/profile.jsonld", EndpointType.Profile);
        endpoints.put(agentEndpoint, EndpointType.Endpoint);
    }

    /**
     * Register endpoints with prior TAA acceptance
     * 
     * @param tAADigest the digest of the TAA text
     */
    public void registerEndpoints(String tAADigest) {
        try {
            acceptTAA(tAADigest);
        } catch (IOException e) {
            String message = "TAA could not be accepted on ledger.";
            log.error(message, e);
            throw new NetworkException(message);
        }
        registerEndpoints();
    }

    /**
     * Register endpoints
     */
    public void registerEndpoints() {
        for (Entry<String, EndpointType> endpoint : endpoints.entrySet()) {
            registerProfileEndpoint(endpoint.getKey(), endpoint.getValue());
        }

        this.endpointRegistrationRequired = false;
    }

    /**
     * Set temporarily flag to allow deferred registration
     */
    public void setEndpointRegistrationRequired() {
        this.endpointRegistrationRequired = true;
    }

    public boolean getEndpointRegistrationRequired() {
        return this.endpointRegistrationRequired;
    }

    public Optional<TAARecord> getTAA() {
        Optional<TAAInfo> taa = null;
        try {
            taa = ac.ledgerTaa();
            if (taa.isPresent())
                return Optional.of(taa.get().getTaaRecord());

        } catch (IOException e) {
            String message = "TAA could not be retrieved";
            log.error(message, e);
            throw new NetworkException(message);
        }
        return Optional.empty();
    }

    public boolean endpointsNewOrChanged() {
        boolean retval = false;

        // register profile endpoint
        final String endpoint = "https://" + host + "/profile.jsonld";
        EndpointType type = EndpointType.Profile;
        if (endpointNewOrChanged(endpoint, type))
            retval = true;

        // register aries endpoint
        type = EndpointType.Endpoint;
        if (endpointNewOrChanged(agentEndpoint, type))
            retval = true;

        return retval;
    }

    public boolean endpointNewOrChanged(String endpoint, EndpointType type) {
        try {
            if (ac.walletDidPublic().isPresent()) {
                WalletDidResponse res = ac.walletDidPublic().get();

                final Optional<EndpointResponse> existingEndpoint = ac.ledgerDidEndpoint(
                        res.getDid(), type);
                boolean newOrChanged = existingEndpoint.isEmpty()
                        || StringUtils.isEmpty(existingEndpoint.get().getEndpoint())
                        || existingEndpoint.isPresent() && !endpoint.equals(existingEndpoint.get().getEndpoint());

                if (!newOrChanged)
                    log.info("Endpoint found on the ledger which did not change: {}",
                            existingEndpoint.get().getEndpoint());
                else
                    log.info("Endpoint has to be set or changed to: {}",
                            endpoint);
                return newOrChanged;
            } else {
                log.warn("No public did available");
            }
        } catch (Exception e) {
            log.error("Could not query for the '{}' endpoint", type, e);
        }
        return true;
    }

    private void registerProfileEndpoint(String endpoint, EndpointType type) {
        try {
            WalletDidResponse res = ac.walletDidPublic().get();
            log.info("Publishing public '{}' endpoint: {}", type, endpoint);
            ac.walletSetDidEndpoint(SetDidEndpointRequest
                    .builder()
                    .did(res.getDid())
                    .endpointType(type)
                    .endpoint(endpoint)
                    .build());
        } catch (IOException e) {
            log.error("Could not publish the '{}' endpoint", type, e);
        }
    }

    private void acceptTAA(String tAADigest) throws IOException {
        Optional<TAAInfo> taa = ac.ledgerTaa();
        if (!taa.isEmpty()) {
            // do TAA acceptance even if not required or if already done
            // (assuming the user accepted the TAA for this single ledger interaction or
            // this session)
            TAAInfo taaInfo = taa.get();
            TAARecord taaRecord = taaInfo.getTaaRecord();
            if (tAADigest.equals(taaRecord.getDigest())) {
                ac.ledgerTaaAccept(TAAAccept.builder()
                        // TODO use suitable mechanism, e.g. "session type".For the time being we just
                        // use a random one.
                        .mechanism(taaInfo.getAmlRecord().getAml().keySet().iterator().next())
                        .text(taaRecord.getText())
                        .version(taaRecord.getVersion())
                        .build());
            }
        }
    }
}
