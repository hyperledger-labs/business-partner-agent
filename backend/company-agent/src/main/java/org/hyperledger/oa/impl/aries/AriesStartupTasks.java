/*
  Copyright (c) 2020 - for information on the respective copyright owner
  see the NOTICE file and/or the repository at
  https://github.com/hyperledger-labs/organizational-agent

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

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.env.Environment;
import io.micronaut.scheduling.annotation.Async;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.ledger.TAAInfo;
import org.hyperledger.oa.config.runtime.RequiresAries;
import org.hyperledger.oa.impl.activity.VPManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

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
    EndpointService endpointService;

    @Inject
    Optional<PartnerCredDefLookup> credLookup;

    @Async
    public void onServiceStartedEvent() {
        log.debug("Running aries startup tasks...");

        ac.statusWaitUntilReady(Duration.ofSeconds(60));

        createDefaultSchemas();

        vpMgmt.getVerifiablePresentation().ifPresentOrElse(vp -> log.info("VP already exists, skipping: {}", host),
                () -> {
                    log.info("Creating default public profile for host: {}", host);
                    vpMgmt.recreateVerifiablePresentation();
                });

        if (endpointService.endpointsNewOrChanged()) {
            // register endpoints if no TTA acceptance is required,
            // otherwise set related flag
            Optional<TAAInfo> taa;
            try {
                taa = ac.ledgerTaa();
                if (taa.isEmpty() || !taa.get().getTaaRequired())
                    endpointService.registerEndpoints();
                else
                    endpointService.setEndpointRegistrationRequired();
            } catch (IOException e) {
                log.error("Endpoints could not be registered", e);
            }
        }

        credLookup.ifPresent(PartnerCredDefLookup::lookupTypesForAllPartnersAsync);
    }

    private void createDefaultSchemas() {
        log.debug("Purging and re-setting default schemas.");

        schemaService.ifPresent(SchemaService::resetWriteOnlySchemas);
    }
}
