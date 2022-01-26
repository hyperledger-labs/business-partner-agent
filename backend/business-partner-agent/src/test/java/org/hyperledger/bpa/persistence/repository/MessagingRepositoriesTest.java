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
package org.hyperledger.bpa.persistence.repository;

import io.micronaut.data.exceptions.DataAccessException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.hyperledger.bpa.persistence.model.messaging.MessageTemplate;
import org.hyperledger.bpa.persistence.model.messaging.MessageTrigger;
import org.hyperledger.bpa.persistence.model.messaging.MessageTriggerConfig;
import org.hyperledger.bpa.persistence.model.messaging.MessageUserInfo;
import org.hyperledger.bpa.persistence.repository.messaging.MessageTemplateRepository;
import org.hyperledger.bpa.persistence.repository.messaging.MessageTriggerConfigRepository;
import org.hyperledger.bpa.persistence.repository.messaging.MessageUserInfoRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@MicronautTest(transactional = false)
public class MessagingRepositoriesTest {

    @Inject
    MessageTemplateRepository template;

    @Inject
    MessageUserInfoRepository user;

    @Inject
    MessageTriggerConfigRepository trigger;

    @Test
    void testPersistTriggerWithDependencies() {
        MessageUserInfo userInfo1 = user.save(MessageUserInfo.builder()
                .sendTo("test@somewhere.co")
                .build());

        MessageUserInfo userInfo2 = user.save(MessageUserInfo.builder()
                .sendTo("something@else.co")
                .build());

        MessageTemplate messageTemplate = template.save(MessageTemplate.builder()
                .template("notification message")
                .build());

        MessageTriggerConfig triggerConfig = trigger.save(MessageTriggerConfig.builder()
                .userInfo(MessageUserInfo.builder().id(userInfo1.getId()).build())
                .template(MessageTemplate.builder().id(messageTemplate.getId()).build())
                .trigger(MessageTrigger.CREDENTIAL_PROPOSAL)
                .build());

        Assertions.assertEquals("test@somewhere.co", trigger
                .findById(triggerConfig.getId()).orElseThrow().getUserInfo().getSendTo());

        trigger.updateTriggerConfig(triggerConfig.getId(), MessageTrigger.PROPOSAL_RECEIVED, messageTemplate,
                userInfo2);
        Assertions.assertEquals("something@else.co", trigger
                .findById(triggerConfig.getId()).orElseThrow().getUserInfo().getSendTo());

        user.deleteById(userInfo1.getId());
        Assertions.assertThrows(DataAccessException.class, () -> user.deleteById(userInfo2.getId()));
        Assertions.assertThrows(DataAccessException.class, () -> template.deleteById(messageTemplate.getId()));

        trigger.updateTriggerConfig(triggerConfig.getId(), MessageTrigger.PROPOSAL_RECEIVED, null, userInfo2);
        trigger.deleteById(triggerConfig.getId());

        Assertions.assertTrue(template.findById(messageTemplate.getId()).isPresent());
        template.deleteById(messageTemplate.getId());
        Assertions.assertFalse(template.findById(messageTemplate.getId()).isPresent());
    }
}
