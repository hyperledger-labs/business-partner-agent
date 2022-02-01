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
package org.hyperledger.bpa.impl;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.hyperledger.acy_py.generated.model.InvitationRecord;
import org.hyperledger.bpa.api.exception.EntityNotFoundException;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.controller.api.messaging.AdHocMessageRequest;
import org.hyperledger.bpa.impl.messaging.MessagingManager;
import org.hyperledger.bpa.impl.messaging.email.EmailCmd;
import org.hyperledger.bpa.impl.messaging.email.EmailService;
import org.hyperledger.bpa.persistence.model.Partner;
import org.hyperledger.bpa.persistence.model.messaging.MessageTemplate;
import org.hyperledger.bpa.persistence.model.messaging.MessageUserInfo;
import org.hyperledger.bpa.persistence.repository.PartnerRepository;
import org.hyperledger.bpa.persistence.repository.messaging.MessageTemplateRepository;
import org.hyperledger.bpa.persistence.repository.messaging.MessageUserInfoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

@MicronautTest
public class MessagingManagerTest {

    @Inject
    EmailService emailService; // already a mock

    @Inject
    MessagingManager messagingManager;

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    MessageTemplateRepository messageTemplateRepo;

    @Inject
    MessageUserInfoRepository userInfoRepo;

    @AfterEach
    void reset() {
        Mockito.reset(emailService);
    }

    @Test
    void testNoPartnerFound() {
        Assertions.assertThrows(EntityNotFoundException.class, () -> messagingManager.sendMessage(AdHocMessageRequest
                .builder()
                .invitationId(UUID.randomUUID())
                .build()));
        Mockito.verify(emailService, Mockito.never()).send(Mockito.any(EmailCmd.class));
    }

    @Test
    void testNoMessageTemplateFound() {
        Partner p = createDefaultPartner(UUID.randomUUID().toString(), null);
        Assertions.assertNotNull(p.getInvitationMsgId());
        Assertions.assertThrows(EntityNotFoundException.class, () -> messagingManager.sendMessage(AdHocMessageRequest
                .builder()
                .invitationId(UUID.fromString(p.getInvitationMsgId()))
                .templateId(UUID.randomUUID())
                .build()));
        Mockito.verify(emailService, Mockito.never()).send(Mockito.any(EmailCmd.class));
    }

    @Test
    void testNoUserInfoFound() {
        Partner p = createDefaultPartner(UUID.randomUUID().toString(), null);
        Assertions.assertNotNull(p.getInvitationMsgId());
        Assertions.assertThrows(EntityNotFoundException.class, () -> messagingManager.sendMessage(AdHocMessageRequest
                .builder()
                .invitationId(UUID.fromString(p.getInvitationMsgId()))
                .userInfoId(UUID.randomUUID())
                .build()));
        Mockito.verify(emailService, Mockito.never()).send(Mockito.any(EmailCmd.class));
    }

    @Test
    void testNoValidMailFound() {
        Partner p = createDefaultPartner(UUID.randomUUID().toString(), null);
        MessageTemplate t = createDefaultTemplate(null, "test");
        Assertions.assertNotNull(p.getInvitationMsgId());
        Assertions.assertThrows(WrongApiUsageException.class, () -> messagingManager.sendMessage(AdHocMessageRequest
                .builder()
                .templateId(t.getId())
                .invitationId(UUID.fromString(p.getInvitationMsgId()))
                .build()));
        Mockito.verify(emailService, Mockito.never()).send(Mockito.any(EmailCmd.class));
    }

    @Test
    void testNoInvitationUrl() {
        Partner p = createDefaultPartner(UUID.randomUUID().toString(), null);
        MessageUserInfo u = createDefaultUserInfo("testuser@test.com");
        Assertions.assertNotNull(p.getInvitationMsgId());
        Assertions.assertThrows(IllegalStateException.class, () -> messagingManager.sendMessage(AdHocMessageRequest
                .builder()
                .invitationId(UUID.fromString(p.getInvitationMsgId()))
                .userInfoId(u.getId())
                .build()));
        Mockito.verify(emailService, Mockito.never()).send(Mockito.any(EmailCmd.class));
    }

    @Test
    void testDefaults() {
        Partner p = createDefaultPartner(UUID.randomUUID().toString(), InvitationRecord.builder()
                .invitationUrl("https://mock.test.com")
                .build());
        MessageUserInfo u = createDefaultUserInfo("someone@test.com");
        Assertions.assertNotNull(p.getInvitationMsgId());
        messagingManager.sendMessage(AdHocMessageRequest
                .builder()
                .invitationId(UUID.fromString(p.getInvitationMsgId()))
                .userInfoId(u.getId())
                .build());

        Mockito.verify(emailService, Mockito.times(1))
                .send(Mockito.argThat(cmd -> cmd.getSubject() != null && cmd.getSubject().startsWith("Your request")
                        && cmd.getTo() != null && cmd.getTo().startsWith("someone@test")
                        && cmd.getTextBody() != null && cmd.getTextBody().startsWith("To receive")));
    }

    @Test
    void testProvided() {
        Partner p = createDefaultPartner(UUID.randomUUID().toString(), InvitationRecord.builder()
                .invitationUrl("https://mock.test.com")
                .build());
        MessageTemplate t = createDefaultTemplate("My Subject", "My Template");
        Assertions.assertNotNull(p.getInvitationMsgId());
        messagingManager.sendMessage(AdHocMessageRequest
                .builder()
                .invitationId(UUID.fromString(p.getInvitationMsgId()))
                .templateId(t.getId())
                .email("one@two.com")
                .build());

        Mockito.verify(emailService, Mockito.times(1))
                .send(Mockito.argThat(cmd -> cmd.getSubject() != null && cmd.getSubject().startsWith("My Subject")
                        && cmd.getTo() != null && cmd.getTo().startsWith("one@two")
                        && cmd.getTextBody() != null && cmd.getTextBody().startsWith("My Template")));
    }

    private Partner createDefaultPartner(String invitationMsgId, InvitationRecord invitationRecord) {
        return partnerRepo.save(Partner.builder()
                .did(UUID.randomUUID().toString())
                .alias(UUID.randomUUID().toString())
                .ariesSupport(Boolean.FALSE)
                .invitationMsgId(invitationMsgId)
                .invitationRecord(invitationRecord)
                .build());
    }

    private MessageTemplate createDefaultTemplate(String subject, String template) {
        return messageTemplateRepo.save(MessageTemplate.builder()
                .subject(subject)
                .template(template)
                .build());
    }

    private MessageUserInfo createDefaultUserInfo(String mail) {
        return userInfoRepo.save(MessageUserInfo.builder()
                .sendTo(mail)
                .build());
    }
}
