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

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.exceptions.DataAccessException;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.bpa.api.exception.EntityNotFoundException;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.api.notification.CredentialProposalEvent;
import org.hyperledger.bpa.api.notification.PartnerRequestReceivedEvent;
import org.hyperledger.bpa.api.notification.PresentationRequestReceivedEvent;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.controller.api.messaging.AdHocMessageRequest;
import org.hyperledger.bpa.controller.api.messaging.MessageTemplateCmd;
import org.hyperledger.bpa.controller.api.messaging.MessageTriggerConfigCmd;
import org.hyperledger.bpa.controller.api.messaging.MessageUserInfoCmd;
import org.hyperledger.bpa.impl.messaging.email.EmailCmd;
import org.hyperledger.bpa.impl.messaging.email.EmailService;
import org.hyperledger.bpa.impl.util.TimeUtil;
import org.hyperledger.bpa.persistence.model.Partner;
import org.hyperledger.bpa.persistence.model.messaging.MessageTemplate;
import org.hyperledger.bpa.persistence.model.messaging.MessageTrigger;
import org.hyperledger.bpa.persistence.model.messaging.MessageTriggerConfig;
import org.hyperledger.bpa.persistence.model.messaging.MessageUserInfo;
import org.hyperledger.bpa.persistence.repository.PartnerRepository;
import org.hyperledger.bpa.persistence.repository.messaging.MessageTemplateRepository;
import org.hyperledger.bpa.persistence.repository.messaging.MessageTriggerConfigRepository;
import org.hyperledger.bpa.persistence.repository.messaging.MessageUserInfoRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Slf4j
@Singleton
public class MessagingManager {

    private final Handlebars handlebars = new Handlebars();

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

    @Inject
    PartnerRepository partnerRepo;

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
        try {
            messageTemplate.deleteById(id);
        } catch (DataAccessException e) {
            throw new WrongApiUsageException(ms.getMessage("mail.template.constrain.violation"));
        }
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
        try {
            userInfo.deleteById(id);
        } catch (DataAccessException e) {
            throw new WrongApiUsageException(ms.getMessage("mail.user.info.constrain.violation"));
        }
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

    public void sendMessage(@NonNull AdHocMessageRequest request) {
        EmailService mailer = emailService
                .orElseThrow(() -> new IllegalStateException(ms.getMessage("mail.error.no.email.provider")));
        Partner p = partnerRepo.findByConnectionIdOrInvitationMsgId(
                request.getInvitationId().toString(), request.getInvitationId().toString())
                .orElseThrow(EntityNotFoundException::new);
        MessageTemplate t = null;
        if (request.getTemplateId() != null) {
            t = messageTemplate.findById(request.getTemplateId()).orElseThrow(EntityNotFoundException::new);
        }

        String to;
        if (StringUtils.isNotEmpty(request.getEmail())) {
            to = request.getEmail();
        } else {
            if (request.getUserInfoId() == null) {
                throw new WrongApiUsageException(ms.getMessage("mail.error.no.valid.email"));
            }
            MessageUserInfo i = userInfo.findById(request.getUserInfoId()).orElseThrow(EntityNotFoundException::new);
            to = i.getSendTo();
        }

        String subject;
        if (t != null && StringUtils.isNotEmpty(t.getSubject())) {
            subject = t.getSubject();
        } else {
            subject = ms.getMessage("mail.default.notification.subject");
        }

        if (p.getInvitationRecord() == null || StringUtils.isEmpty(p.getInvitationRecord().getInvitationUrl())) {
            throw new IllegalStateException(ms.getMessage("mail.error.invitation.uri.not.set"));
        }
        String body = resolveAdHocMessageBody(p.getInvitationRecord().getInvitationUrl(), t);

        mailer.send(EmailCmd.builder()
                .to(to)
                .subject(subject)
                .textBody(body)
                .build());
    }

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
        emailService.ifPresent(mailer -> {
            String defaultSubject = ms.getMessage("mail.default.event.subject");
            triggerConfig.findByTrigger(trigger)
                    .parallelStream()
                    .forEach(t -> mailer.send(t.toEmailCmd(defaultSubject, resolveEventMessageBody(trigger, t))));
        });
    }

    private String resolveEventMessageBody(@NonNull MessageTrigger trigger, @NonNull MessageTriggerConfig t) {
        Map<String, Object> model = Map.of("event", trigger, "time", TimeUtil.toISOInstant(Instant.now()));
        String defaultBody = ms.getMessage("mail.default.event.body", model);
        if (t.getTemplate() == null || t.getTemplate().getTemplate() == null) {
            return defaultBody;
        }
        return compileTemplate(t.getTemplate(), model, defaultBody);
    }

    private String resolveAdHocMessageBody(@NonNull String uri, @Nullable MessageTemplate t) {
        Map<String, Object> model = Map.of("uri", uri);
        String defaultBody = ms.getMessage("mail.default.notification.body", model);
        if (t == null || t.getTemplate() == null) {
            return defaultBody;
        }
        return compileTemplate(t, model, defaultBody);
    }

    private String compileTemplate(@NonNull MessageTemplate t, @NonNull Map<String, Object> model,
            @NonNull String defaultBody) {
        try {
            Template template = handlebars.compileInline(t.getTemplate());
            return template.apply(model);
        } catch (IOException e) {
            return defaultBody;
        }
    }
}
