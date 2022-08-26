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
import io.micronaut.http.HttpMethod;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.filters.AuthenticationFetcher;
import io.reactivex.rxjava3.core.Maybe;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.bpa.controller.AriesWebhookController;
import org.reactivestreams.Publisher;

/**
 * Backwards compatible AuthFetcher if security is set to true, but no
 * properties are set.
 */
@Slf4j
@Singleton
@Requires(missingProperty = "bpa.webhook.key")
public class AcaPyAuthFetcherAllowAll implements AuthenticationFetcher {

    // TODO Delete

    @Override
    public Publisher<Authentication> fetchAuthentication(io.micronaut.http.HttpRequest<?> request) {
        return Maybe.<Authentication>create(emitter -> {
            if (HttpMethod.POST.equals(request.getMethod())
                    && request.getPath().startsWith(AriesWebhookController.WEBHOOK_CONTROLLER_PATH)) {
                log.trace("Handling aca-py webhook authentication");
                emitter.onSuccess(AcaPyAuthFetcher.acaPyAuthentication());
                return;
            }
            emitter.onComplete();
        }).toFlowable();
    }
}
