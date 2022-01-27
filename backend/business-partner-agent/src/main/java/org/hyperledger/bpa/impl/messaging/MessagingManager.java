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
package org.hyperledger.bpa.impl.messaging;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import org.hyperledger.bpa.api.notification.CredentialProposalEvent;
import org.hyperledger.bpa.api.notification.PartnerRequestReceivedEvent;
import org.hyperledger.bpa.api.notification.PresentationRequestReceivedEvent;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.controller.api.messaging.MessageTemplateCmd;
import org.hyperledger.bpa.controller.api.messaging.MessageTriggerConfigCmd;
import org.hyperledger.bpa.controller.api.messaging.MessageUserInfoCmd;
import org.hyperledger.bpa.impl.messaging.email.EmailService;
import org.hyperledger.bpa.impl.util.TimeUtil;
import org.hyperledger.bpa.persistence.model.messaging.MessageTemplate;
import org.hyperledger.bpa.persistence.model.messaging.MessageTrigger;
import org.hyperledger.bpa.persistence.model.messaging.MessageTriggerConfig;
import org.hyperledger.bpa.persistence.model.messaging.MessageUserInfo;
import org.hyperledger.bpa.persistence.repository.messaging.MessageTemplateRepository;
import org.hyperledger.bpa.persistence.repository.messaging.MessageTriggerConfigRepository;
import org.hyperledger.bpa.persistence.repository.messaging.MessageUserInfoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Singleton
public class MessagingManager {

    @Inject
    Optional<EmailService> emailService;

    @Inject
    MessageTemplateRepository messageTemplate;

    @Inject
    MessageUserInfoRepository userInfo;

    @Inject
    MessageTriggerConfigRepository triggerConfig;

    @Inject
    BPAMessageSource.DefaultMessageSource ms;

    // crud message templates

    public List<MessageTemplateCmd.ApiMessageTemplate> listMessageTemplates() {
        return StreamSupport.stream(messageTemplate.findAll().spliterator(), false)
                .map(MessageTemplateCmd.ApiMessageTemplate::fromMessageTemplate)
                .toList();
    }

    @io.micronaut.core.annotation.NonNull
    public MessageTemplateCmd.ApiMessageTemplate addMessageTemplate(@Nullable String subject,
            @NonNull String template) {
        return MessageTemplateCmd.ApiMessageTemplate.fromMessageTemplate(messageTemplate.save(MessageTemplate.builder()
                .subject(subject)
                .template(template)
                .build()));
    }

    public void updateMessageTemplate(@NonNull UUID id, @Nullable String subject, @NonNull String template) {
        messageTemplate.updateTemplateInfo(id, subject, template);
    }

    public void deleteTemplateInfo(@NonNull UUID id) {
        messageTemplate.deleteById(id);
    }

    // crud user info

    public List<MessageUserInfoCmd.ApiUserInfo> listUserInfo() {
        return StreamSupport.stream(userInfo.findAll().spliterator(), false)
                .map(MessageUserInfoCmd.ApiUserInfo::fromMessageUserInfo)
                .toList();
    }

    @io.micronaut.core.annotation.NonNull
    public MessageUserInfoCmd.ApiUserInfo addUserInfo(@Nullable String label, @NonNull String sendTo) {
        return MessageUserInfoCmd.ApiUserInfo.fromMessageUserInfo(userInfo.save(MessageUserInfo.builder()
                .sendTo(sendTo)
                .label(label)
                .build()));
    }

    public void updateUserInfo(@NonNull UUID id, @Nullable String label, @NonNull String sendTo) {
        userInfo.updateUserInfo(id, label, sendTo);
    }

    public void deleteUserInfo(@NonNull UUID id) {
        userInfo.deleteById(id);
    }

    // crud trigger config

    public List<MessageTriggerConfigCmd.ApiTriggerConfig> listTriggerConfig() {
        return StreamSupport.stream(triggerConfig.findAll().spliterator(), false)
                .map(MessageTriggerConfigCmd.ApiTriggerConfig::fromMessageTriggerConfig)
                .toList();
    }

    public MessageTriggerConfigCmd.ApiTriggerConfig addTriggerConfig(@NonNull MessageTrigger trigger,
            @Nullable UUID messageTemplateId, @NonNull UUID userInfoId) {
        return MessageTriggerConfigCmd.ApiTriggerConfig.fromMessageTriggerConfig(triggerConfig.save(MessageTriggerConfig
                .builder()
                .trigger(trigger)
                .template(messageTemplateId != null ? MessageTemplate.builder().id(messageTemplateId).build() : null)
                .userInfo(MessageUserInfo.builder().id(userInfoId).build())
                .build()));
    }

    public void updateTriggerConfig(@NonNull UUID id, @NonNull MessageTrigger trigger, @Nullable UUID messageTemplateId,
            @NonNull UUID userInfoId) {
        triggerConfig.updateTriggerConfig(id, trigger,
                MessageTemplate.builder().id(messageTemplateId).build(),
                MessageUserInfo.builder().id(userInfoId).build());
    }

    public void deleteTriggerConfig(@NonNull UUID id) {
        triggerConfig.deleteById(id);
    }

    // invitation

    // event handler

    @EventListener
    public void onCredentialProposalEvent(CredentialProposalEvent event) {
        findAndSend(MessageTrigger.CREDENTIAL_PROPOSAL);
    }

    @EventListener
    public void onPresentationRequestEvent(PresentationRequestReceivedEvent event) {
        findAndSend(MessageTrigger.PRESENTATION_REQUEST);
    }

    @EventListener
    public void onPartnerRequestEvent(PartnerRequestReceivedEvent event) {
        findAndSend(MessageTrigger.CONNECTION_REQUEST);
    }

    private void findAndSend(@NonNull MessageTrigger trigger) {
        String defaultBody = ms.getMessage("mail.default.body",
                Map.of("event", trigger, "time", TimeUtil.toISOInstant(Instant.now())));
        String defaultSubject = ms.getMessage("mail.default.subject");
        emailService.ifPresent(mailer -> triggerConfig.findByTrigger(trigger)
                .parallelStream().forEach(t -> mailer.send(t.toEmailCmd(defaultSubject, defaultBody))));
    }
}
