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
package org.hyperledger.oa.impl.aries;

import java.time.Duration;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.ledger.EndpointResponse;
import org.hyperledger.aries.api.ledger.EndpointType;
import org.hyperledger.aries.api.wallet.SetDidEndpointRequest;
import org.hyperledger.oa.config.runtime.RequiresAries;
import org.hyperledger.oa.impl.activity.VPManager;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.env.Environment;
import io.micronaut.scheduling.annotation.Async;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@RequiresAries
@Requires(notEnv = { Environment.TEST })
public class AriesStartupTasks {

    @Value("${oagent.host}")
    String host;

    @Inject
    VPManager vpMgmt;

    @Inject
    AriesClient ac;

    @Inject
    Optional<SchemaService> schemaService;

    @Inject
    Optional<PartnerCredDefLookup> credLookup;

    @Async
    public void onServiceStartedEvent() {
        log.debug("Running aries startup tasks...");

        ac.statusWaitUntilReady(Duration.ofSeconds(60));

        createDefaultSchemas();

        vpMgmt.getVerifiablePresentation().ifPresentOrElse(vp -> {
            log.info("VP already exists, skipping: {}", host);
        }, () -> {
            log.info("Creating default public profile for host: {}", host);
            vpMgmt.recreateVerifiablePresentation();
        });

        credLookup.ifPresent(lookup -> lookup.lookupTypesForAllPartnersAsync());

        // currently done by aca-py --profile-endpoint option
        // registerProfileEndpoint();
    }

    private void createDefaultSchemas() {
        log.debug("Purging and re-setting default schemas.");

        schemaService.ifPresent(s -> s.resetWriteOnlySchemas());
    }

    void registerProfileEndpoint() {
        try {
            ac.walletDidPublic().ifPresentOrElse(res -> {
                try {
                    final String endpoint = "https://" + host + "/profile.jsonld";
                    final Optional<EndpointResponse> profileEP = ac.ledgerDidEndpoint(
                            res.getDid(), EndpointType.Profile);
                    if (profileEP.isEmpty() || StringUtils.isEmpty(profileEP.get().getEndpoint())
                            || profileEP.isPresent() && !endpoint.equals(profileEP.get().getEndpoint())) {
                        log.info("Publishing public 'profile' endpoint: {}", endpoint);
                        ac.walletSetDidEndpoint(SetDidEndpointRequest
                                .builder()
                                .did(res.getDid())
                                .endpointType(EndpointType.Profile)
                                .endpoint(endpoint)
                                .build());
                    } else {
                        log.info("Profile endpoint found on the ledger, skipping: {}",
                                profileEP.get().getEndpoint());
                    }
                } catch (Exception e) {
                    log.error("Could not publish the 'Profile' endpoint", e);
                }
            }, () -> {
                log.warn("No public did available, no 'Profile' endpoint is published");
            });
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
