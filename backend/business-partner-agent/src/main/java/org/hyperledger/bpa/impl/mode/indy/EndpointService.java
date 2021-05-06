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
package org.hyperledger.bpa.impl.mode.indy;

import io.micronaut.context.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.acy_py.generated.model.DID;
import org.hyperledger.acy_py.generated.model.DIDEndpointWithType;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.ledger.EndpointResponse;
import org.hyperledger.aries.api.ledger.EndpointType;
import org.hyperledger.aries.api.ledger.TAAAccept;
import org.hyperledger.aries.api.ledger.TAAInfo;
import org.hyperledger.aries.api.ledger.TAAInfo.TAARecord;
import org.hyperledger.bpa.api.exception.NetworkException;
import org.hyperledger.bpa.config.runtime.RequiresIndy;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

/**
 * Service which registers endpoints on the ledger.
 *
 * Either called in the start-up phases of the business partner agent or later,
 * triggered by the frontend (if TAA acceptance from user is required)
 */
@Slf4j
@Singleton
@RequiresIndy
public class EndpointService {

    private boolean endpointRegistrationRequired = false;

    private final Map<String, DIDEndpointWithType.EndpointTypeEnum> endpoints;

    @Inject
    AriesClient ac;

    @Inject
    public EndpointService(
            @Value(value = "${bpa.acapy.endpoint}") String acapyEndpoint,
            @Value(value = "${bpa.scheme}") String scheme,
            @Value(value = "${bpa.host}") String host) {
        endpoints = new HashMap<>();
        endpoints.put(scheme + "://" + host + "/profile.jsonld", DIDEndpointWithType.EndpointTypeEnum.PROFILE);
        endpoints.put(acapyEndpoint, DIDEndpointWithType.EndpointTypeEnum.ENDPOINT);
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
        for (Entry<String, DIDEndpointWithType.EndpointTypeEnum> endpoint : endpoints.entrySet()) {
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
        Optional<TAAInfo> taa;
        try {
            taa = ac.ledgerTaa();
            if (taa.isPresent())
                return Optional.ofNullable(taa.get().getTaaRecord());

        } catch (IOException e) {
            String message = "TAA could not be retrieved";
            log.error(message, e);
            throw new NetworkException(message);
        }
        return Optional.empty();
    }

    public boolean endpointsNewOrChanged() {
        boolean retval = false;

        for (Entry<String, DIDEndpointWithType.EndpointTypeEnum> endpoint : endpoints.entrySet()) {
            if (endpointNewOrChanged(endpoint.getKey(), endpoint.getValue()))
                retval = true;
        }

        return retval;
    }

    public boolean endpointNewOrChanged(String endpoint, DIDEndpointWithType.EndpointTypeEnum type) {
        try {
            Optional<DID> did = ac.walletDidPublic();
            if (did.isPresent()) {
                DID res = did.get();

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
            }
            log.warn("No public did available");
        } catch (Exception e) {
            log.error("Could not query for the '{}' endpoint", type, e);
        }
        return true;
    }

    private void registerProfileEndpoint(String endpoint, DIDEndpointWithType.EndpointTypeEnum type) {
        try {
            Optional<DID> pubDid = ac.walletDidPublic();
            if (pubDid.isPresent()) {
                log.info("Publishing public '{}' endpoint: {}", type, endpoint);
                ac.walletSetDidEndpoint(DIDEndpointWithType
                        .builder()
                        .did(pubDid.get().getDid())
                        .endpointType(type)
                        .endpoint(endpoint)
                        .build());
            }
        } catch (IOException e) {
            log.error("Could not publish the '{}' endpoint", type, e);
        }
    }

    private void acceptTAA(String tAADigest) throws IOException {
        Optional<TAAInfo> taa = ac.ledgerTaa();
        if (taa.isPresent()) {
            // do TAA acceptance even if not required or if already done
            // (assuming the user accepted the TAA for this single ledger interaction or
            // this session)
            TAAInfo taaInfo = taa.get();
            TAARecord taaRecord = taaInfo.getTaaRecord();
            if (MessageDigest.isEqual(tAADigest.getBytes(StandardCharsets.UTF_8),
                    taaRecord.getDigest().getBytes(StandardCharsets.UTF_8))) {
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
