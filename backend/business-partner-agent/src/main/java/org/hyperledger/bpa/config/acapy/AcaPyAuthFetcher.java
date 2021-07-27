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
package org.hyperledger.bpa.config.acapy;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpMethod;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.filters.AuthenticationFetcher;
import io.reactivex.Maybe;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.bpa.controller.AriesWebhookController;
import org.jetbrains.annotations.NotNull;
import org.reactivestreams.Publisher;

import javax.inject.Singleton;
import java.util.List;
import java.util.Map;

/**
 * Handles aca-py webhook authentication. If the flag --webhook-url is set
 * aca-py allows setting an api key after an # tag. e.g. --webhook-url
 * http://localhost/hook/topic#123 The api key is then sent via the x-api-key
 * HTTP header. As this is not something micronaut handles out of the box, a
 * custom {@link AuthenticationFetcher} needs to be provided.
 *
 * The AuthFetcher becomes active if either the environment variable BPA_WEBHOOK_KEY
 * or the system property bpa.webhook.key is set.
 */
@Slf4j
@Singleton
@Requires(property = "bpa.webhook.key")
public class AcaPyAuthFetcher implements AuthenticationFetcher {

    private static final String X_API_KEY = "x-api-key";

    public static final String ROLE_ACA_PY = "ROLE_ACA_PY";

    @Value("${bpa.webhook.key}")
    String apiKey;

    @Override
    public Publisher<Authentication> fetchAuthentication(io.micronaut.http.HttpRequest<?> request) {
        return Maybe.<Authentication>create(emitter -> {
            if (HttpMethod.POST.equals(request.getMethod())
                    && request.getPath().startsWith(AriesWebhookController.WEBHOOK_CONTROLLER_PATH)) {
                String apiKeyHeader = request.getHeaders().get(X_API_KEY);
                log.trace("Handling aca-py webhook authentication");
                if (StringUtils.isNotBlank(apiKeyHeader) && apiKeyHeader.equals(apiKey)) {
                    emitter.onSuccess(new AcaPyAuthentication());
                    log.trace("aca-py webhook authentication success");
                    return;
                }
                log.error("aca-py webhook authentication failed. " +
                        "Configured bpa.webhook.key: {}, received x-api-key header: {}", apiKey, apiKeyHeader);
            }
            emitter.onComplete();
        }).toFlowable();
    }

    @Data
    @NoArgsConstructor
    public static final class AcaPyAuthentication implements Authentication {

        private static final long serialVersionUID = -2423706290718045174L;

        @NotNull
        @Override
        public Map<String, Object> getAttributes() {
            return Map.of("roles", List.of(ROLE_ACA_PY));
        }

        @Override
        public String getName() {
            return "aca-py";
        }
    }
}