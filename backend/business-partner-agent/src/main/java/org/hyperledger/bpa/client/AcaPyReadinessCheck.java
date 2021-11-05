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
package org.hyperledger.bpa.client;

import io.micronaut.context.annotation.Requires;
import io.micronaut.health.HealthStatus;
import io.micronaut.management.endpoint.health.HealthEndpoint;
import io.micronaut.management.health.indicator.AbstractHealthIndicator;
import io.micronaut.management.health.indicator.annotation.Readiness;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.server.AdminStatusReadiness;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Singleton
@Readiness
@Requires(beans = HealthEndpoint.class)
public class AcaPyReadinessCheck extends AbstractHealthIndicator<Map<String, String>> {

    @Inject
    AriesClient ac;

    @Override
    protected Map<String, String> getHealthInformation() {
        try {
            Optional<AdminStatusReadiness> status = ac.statusReady();
            if (status.isPresent() && status.get().isReady()) {
                this.healthStatus = HealthStatus.UP;
            } else {
                this.healthStatus = HealthStatus.DOWN;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    @Override
    protected String getName() {
        return "aca-py-readiness";
    }
}
