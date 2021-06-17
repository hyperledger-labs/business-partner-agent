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

import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.webhook.EventHandler;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Handles incoming aca-py webhook events
 */
@Slf4j
@Hidden
@Tag(name = "Aries Webhook")
@Controller
@Secured(SecurityRule.IS_ANONYMOUS)
@ExecuteOn(TaskExecutors.IO)
public class AriesWebhookController {

    public static final String WEBHOOK_CONTROLLER_PATH = "/log/topic";

    @Inject
    @Named("aries")
    EventHandler ariesEventHandler;

    @Inject
    @Named("rules")
    EventHandler rulesEventHandler;

    @Post(WEBHOOK_CONTROLLER_PATH + "/{eventType}")
    public void logEvent(
            @PathVariable String eventType,
            @Body String eventBody) {

        log.info("Webhook received, type: {}", eventType);

        ariesEventHandler.handleEvent(eventType, eventBody);
        rulesEventHandler.handleEvent(eventType, eventBody); // rules always run after
    }
}
