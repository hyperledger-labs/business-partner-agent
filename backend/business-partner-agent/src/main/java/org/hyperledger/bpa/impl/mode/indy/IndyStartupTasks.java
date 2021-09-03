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

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.ledger.TAAInfo;
import org.hyperledger.bpa.config.runtime.RequiresIndy;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Singleton
@RequiresIndy
@Requires(notEnv = { Environment.TEST })
public class IndyStartupTasks {

    @Inject
    AriesClient ac;

    @Inject
    EndpointService endpointService;

    public void onServiceStartedEvent() {
        log.debug("Running aries startup tasks...");

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
    }
}
