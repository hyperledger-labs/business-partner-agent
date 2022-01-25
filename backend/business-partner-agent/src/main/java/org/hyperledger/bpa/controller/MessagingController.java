/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
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
import io.micronaut.validation.Validated;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.bpa.controller.api.messaging.MessageTemplateCmd;
import org.hyperledger.bpa.controller.api.messaging.MessageTriggerConfigCmd;
import org.hyperledger.bpa.controller.api.messaging.MessageUserInfoCmd;
import org.hyperledger.bpa.impl.messaging.MessagingManager;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

@Slf4j
@Controller("/api/messaging")
@Tag(name = "Messaging Management")
@Validated
@Secured(SecurityRule.IS_AUTHENTICATED)
@ExecuteOn(TaskExecutors.IO)
public class MessagingController {

    @Inject
    MessagingManager messaging;

    // crud message template

    @Get("/template")
    public HttpResponse<List<MessageTemplateCmd.ApiMessageTemplate>> listMessageTemplates() {
        return HttpResponse.ok(messaging.listMessageTemplates());
    }

    @Post("/template")
    public HttpResponse<MessageTemplateCmd.ApiMessageTemplate> addMessageTemplate(
            @Valid @Body MessageTemplateCmd.MessageTemplateRequest template) {
        return HttpResponse.ok(messaging.addMessageTemplate(template.getSubject(), template.getTemplate()));
    }

    @Put("/template/{id}")
    public HttpResponse<Void> updateMessageTemplate(@PathVariable UUID id,
            @Valid @Body MessageTemplateCmd.MessageTemplateRequest template) {
        messaging.updateMessageTemplate(id, template.getSubject(), template.getTemplate());
        return HttpResponse.ok();
    }

    @Delete("/template/{id}")
    public HttpResponse<Void> deleteMessageTemplate(@PathVariable UUID id) {
        messaging.deleteTemplateInfo(id);
        return HttpResponse.ok();
    }

    // crud send config

    @Get("/user-info")
    public HttpResponse<List<MessageUserInfoCmd.ApiUserInfo>> listUserInfo() {
        return HttpResponse.ok(messaging.listUserInfo());
    }

    @Post("/user-info")
    public HttpResponse<MessageUserInfoCmd.ApiUserInfo> addUserInfo(
            @Valid @Body MessageUserInfoCmd.UserInfoRequest config) {
        return HttpResponse.ok(messaging.addUserInfo(config.getLabel(), config.getSendTo()));
    }

    @Put("/user-info/{id}")
    public HttpResponse<Void> updateUserInfo(@PathVariable UUID id,
            @Valid @Body MessageUserInfoCmd.UserInfoRequest config) {
        messaging.updateUserInfo(id, config.getLabel(), config.getSendTo());
        return HttpResponse.ok();
    }

    @Delete("/user-info/{id}")
    public HttpResponse<Void> deleteUserInfo(@PathVariable UUID id) {
        messaging.deleteUserInfo(id);
        return HttpResponse.ok();
    }

    // crud trigger config

    @Get("/trigger")
    public HttpResponse<List<MessageTriggerConfigCmd.ApiTriggerConfig>> listMessageTrigger() {
        return HttpResponse.ok(messaging.listTriggerConfig());
    }

    @Post("/trigger")
    public HttpResponse<MessageTriggerConfigCmd.ApiTriggerConfig> addMessageTrigger(
            @Valid @Body MessageTriggerConfigCmd.TriggerConfigRequest trigger) {
        return HttpResponse.ok(messaging.addTriggerConfig(trigger.getTrigger(), trigger.getMessageTemplateId(),
                trigger.getUserInfoId()));
    }

    @Put("/trigger/{id}")
    public HttpResponse<Void> updateMessageTrigger(@PathVariable UUID id,
            @Valid @Body MessageTriggerConfigCmd.TriggerConfigRequest trigger) {
        messaging.updateTriggerConfig(id, trigger.getTrigger(), trigger.getMessageTemplateId(),
                trigger.getUserInfoId());
        return HttpResponse.ok();
    }

    @Delete("/trigger/{id}")
    public HttpResponse<Void> deleteMessageTrigger(@PathVariable UUID id) {
        messaging.deleteTriggerConfig(id);
        return HttpResponse.ok();
    }
}
