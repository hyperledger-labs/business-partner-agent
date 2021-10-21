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
package org.hyperledger.bpa.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import org.hyperledger.bpa.core.RegisteredWebhook;
import org.hyperledger.bpa.core.RegisteredWebhook.RegisteredWebhookResponse;
import org.hyperledger.bpa.impl.WebhookService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages webhooks
 *
 */
@Controller("/api/webhook")
@Tag(name = "Webhook")
@Secured(SecurityRule.IS_AUTHENTICATED)
@ExecuteOn(TaskExecutors.IO)
public class WebhookController {

    @Inject
    WebhookService ws;

    /**
     * List registered webhooks
     *
     * @return list of {@link RegisteredWebhookResponse}
     */
    @Get
    public HttpResponse<List<RegisteredWebhookResponse>> listRegisteredWebhooks() {
        return HttpResponse.ok(ws.listRegisteredWebhooks());
    }

    /**
     * Register a new webhook
     *
     * @param request {@link RegisteredWebhook}
     * @return {@link RegisteredWebhookResponse}
     */
    @Post
    public HttpResponse<RegisteredWebhookResponse> registerWebhook(@Body RegisteredWebhook request) {
        return HttpResponse.ok(ws.registerWebhook(request));
    }

    /**
     * Update a registered webhook
     *
     * @param id      the webhook's id
     * @param request {@link RegisteredWebhook}
     * @return {@link RegisteredWebhookResponse}
     */
    @Put("/{id}")
    public HttpResponse<RegisteredWebhookResponse> updateWebhook(
            @PathVariable UUID id,
            @Body RegisteredWebhook request) {
        final Optional<RegisteredWebhookResponse> updated = ws.updateRegisteredWebhook(id, request);
        if (updated.isPresent()) {
            return HttpResponse.ok(updated.get());
        }
        return HttpResponse.notFound();
    }

    /**
     * Delete a registered webhook
     *
     * @param id the webhook's id
     * @return always OK
     */
    @Delete("/{id}")
    public HttpResponse<Void> deleteWebhook(@PathVariable UUID id) {
        ws.deleteRegisteredWebhook(id);
        return HttpResponse.ok();
    }
}
