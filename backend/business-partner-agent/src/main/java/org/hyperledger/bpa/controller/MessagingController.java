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
import org.hyperledger.bpa.controller.api.messaging.AdHocMessageRequest;
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

    /**
     * List message templates
     *
     * @return list of {@link MessageTemplateCmd.ApiMessageTemplate}
     */
    @Get("/template")
    public HttpResponse<List<MessageTemplateCmd.ApiMessageTemplate>> listMessageTemplates() {
        return HttpResponse.ok(messaging.listMessageTemplates());
    }

    /**
     * Add new message template
     *
     * @param template {@link MessageTemplateCmd.MessageTemplateRequest}
     * @return {@link MessageTemplateCmd.ApiMessageTemplate}
     */
    @Post("/template")
    public HttpResponse<MessageTemplateCmd.ApiMessageTemplate> addMessageTemplate(
            @Valid @Body MessageTemplateCmd.MessageTemplateRequest template) {
        return HttpResponse.ok(messaging.addMessageTemplate(template.getSubject(), template.getTemplate()));
    }

    /**
     * Update message template
     *
     * @param id       {@link UUID} template id
     * @param template {@link MessageTemplateCmd.MessageTemplateRequest}
     * @return HTTP status
     */
    @Put("/template/{id}")
    public HttpResponse<Void> updateMessageTemplate(@PathVariable UUID id,
            @Valid @Body MessageTemplateCmd.MessageTemplateRequest template) {
        messaging.updateMessageTemplate(id, template.getSubject(), template.getTemplate());
        return HttpResponse.ok();
    }

    /**
     * Delete message template
     *
     * @param id {@link UUID} template id
     * @return HTTP status
     */
    @Delete("/template/{id}")
    public HttpResponse<Void> deleteMessageTemplate(@PathVariable UUID id) {
        messaging.deleteTemplateInfo(id);
        return HttpResponse.ok();
    }

    // crud send config

    /**
     * List user info
     *
     * @return list of {@link MessageUserInfoCmd.ApiUserInfo}
     */
    @Get("/user-info")
    public HttpResponse<List<MessageUserInfoCmd.ApiUserInfo>> listUserInfo() {
        return HttpResponse.ok(messaging.listUserInfo());
    }

    /**
     * Add new user info configuration
     *
     * @param config {@link MessageUserInfoCmd.UserInfoRequest}
     * @return {@link MessageUserInfoCmd.ApiUserInfo}
     */
    @Post("/user-info")
    public HttpResponse<MessageUserInfoCmd.ApiUserInfo> addUserInfo(
            @Valid @Body MessageUserInfoCmd.UserInfoRequest config) {
        return HttpResponse.ok(messaging.addUserInfo(config.getLabel(), config.getSendTo()));
    }

    /**
     * Update user info configuration
     *
     * @param id     {@link UUID} user info id
     * @param config {@link MessageUserInfoCmd.UserInfoRequest}
     * @return HTTP status
     */
    @Put("/user-info/{id}")
    public HttpResponse<Void> updateUserInfo(@PathVariable UUID id,
            @Valid @Body MessageUserInfoCmd.UserInfoRequest config) {
        messaging.updateUserInfo(id, config.getLabel(), config.getSendTo());
        return HttpResponse.ok();
    }

    /**
     * Delete user info configuration
     *
     * @param id {@link UUID} user info id
     * @return HTTP status
     */
    @Delete("/user-info/{id}")
    public HttpResponse<Void> deleteUserInfo(@PathVariable UUID id) {
        messaging.deleteUserInfo(id);
        return HttpResponse.ok();
    }

    // crud trigger config

    /**
     * List message trigger configuration
     *
     * @return list of {@link MessageTriggerConfigCmd.ApiTriggerConfig}
     */
    @Get("/trigger")
    public HttpResponse<List<MessageTriggerConfigCmd.ApiTriggerConfig>> listMessageTrigger() {
        return HttpResponse.ok(messaging.listTriggerConfig());
    }

    /**
     * ASdd new message trigger configuration
     *
     * @param trigger {@link MessageTriggerConfigCmd.ApiTriggerConfig}
     * @return {@link MessageTriggerConfigCmd.TriggerConfigRequest}
     */
    @Post("/trigger")
    public HttpResponse<MessageTriggerConfigCmd.ApiTriggerConfig> addMessageTrigger(
            @Valid @Body MessageTriggerConfigCmd.TriggerConfigRequest trigger) {
        return HttpResponse.ok(messaging.addTriggerConfig(trigger.getTrigger(), trigger.getMessageTemplateId(),
                trigger.getUserInfoId()));
    }

    /**
     * Update message trigger configuration
     *
     * @param id      {@link UUID} message trigger id
     * @param trigger {@link MessageTriggerConfigCmd.TriggerConfigRequest}
     * @return HTTP status
     */
    @Put("/trigger/{id}")
    public HttpResponse<Void> updateMessageTrigger(@PathVariable UUID id,
            @Valid @Body MessageTriggerConfigCmd.TriggerConfigRequest trigger) {
        messaging.updateTriggerConfig(id, trigger.getTrigger(), trigger.getMessageTemplateId(),
                trigger.getUserInfoId());
        return HttpResponse.ok();
    }

    /**
     * Delete message trigger configuration
     *
     * @param id {@link UUID} message trigger id
     * @return HTTP status
     */
    @Delete("/trigger/{id}")
    public HttpResponse<Void> deleteMessageTrigger(@PathVariable UUID id) {
        messaging.deleteTriggerConfig(id);
        return HttpResponse.ok();
    }

    // invitation by mail

    /**
     * Manual trigger to send an invitation url by email
     *
     * @param request {@link AdHocMessageRequest}
     * @return HTTP status
     */
    @Post("/send-invitation")
    public HttpResponse<Void> sendInvitationMail(@Valid @Body AdHocMessageRequest request) {
        messaging.sendMessage(request);
        return HttpResponse.ok();
    }
}
