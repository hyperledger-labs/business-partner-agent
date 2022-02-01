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

import io.micronaut.core.type.Argument;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.hyperledger.bpa.controller.api.messaging.MessageTemplateCmd;
import org.hyperledger.bpa.controller.api.messaging.MessageTriggerConfigCmd;
import org.hyperledger.bpa.controller.api.messaging.MessageUserInfoCmd;
import org.hyperledger.bpa.persistence.model.messaging.MessageTrigger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

@MicronautTest
public class MessagingControllerTest extends BaseControllerTest {

    private static final String TEMPLATE = "/template";
    private static final String TEMPLATE_ID = "/template/{id}";

    public static final String USER_INFO = "/user-info";
    public static final String USER_INFO_ID = "/user-info/{id}";

    public static final String TRIGGER = "/trigger";
    public static final String TRIGGER_ID = "/trigger/{id}";

    public MessagingControllerTest(@Client("/api/messaging") HttpClient client) {
        super(client);
    }

    // template tests

    @Test
    void testCreateTemplateWithMissingTemplate() {
        Assertions.assertThrows(HttpClientResponseException.class,
                () -> post(TEMPLATE, new MessageTemplateCmd.MessageTemplateRequest(),
                        MessageTemplateCmd.ApiMessageTemplate.class));
    }

    @Test
    void testCrudTemplate() {
        MessageTemplateCmd.ApiMessageTemplate template = post(TEMPLATE,
                new MessageTemplateCmd.MessageTemplateRequest(null, "myTemplate"),
                MessageTemplateCmd.ApiMessageTemplate.class).body();
        Assertions.assertNotNull(template);
        Assertions.assertNotNull(template.getId());

        List<MessageTemplateCmd.ApiMessageTemplate> templates = getAll(TEMPLATE,
                Argument.listOf(MessageTemplateCmd.ApiMessageTemplate.class));
        Assertions.assertEquals(1, templates.size());
        Assertions.assertEquals("myTemplate", templates.get(0).getTemplate());

        put(buildURI(TEMPLATE_ID, Map.of("id", template.getId())),
                new MessageTemplateCmd.MessageTemplateRequest(null, "updatedTemplate"));

        templates = getAll(TEMPLATE,
                Argument.listOf(MessageTemplateCmd.ApiMessageTemplate.class));
        Assertions.assertEquals(1, templates.size());
        Assertions.assertEquals("updatedTemplate", templates.get(0).getTemplate());

        delete(TEMPLATE + "/" + template.getId());

        templates = getAll(TEMPLATE,
                Argument.listOf(MessageTemplateCmd.ApiMessageTemplate.class));
        Assertions.assertEquals(0, templates.size());
    }

    // user info

    @Test
    void testCreateWithInvalidMail() {
        Assertions.assertThrows(HttpClientResponseException.class,
                () -> post(USER_INFO, new MessageUserInfoCmd.UserInfoRequest("foo@", null),
                        MessageUserInfoCmd.ApiUserInfo.class));
    }

    @Test
    void testCrudUserInfo() {
        MessageUserInfoCmd.ApiUserInfo userInfo = post(USER_INFO,
                new MessageUserInfoCmd.UserInfoRequest("one@two.com", "test"),
                MessageUserInfoCmd.ApiUserInfo.class).body();
        Assertions.assertNotNull(userInfo);
        Assertions.assertNotNull(userInfo.getId());

        List<MessageUserInfoCmd.ApiUserInfo> userInfos = getAll(USER_INFO,
                Argument.listOf(MessageUserInfoCmd.ApiUserInfo.class));
        Assertions.assertEquals(1, userInfos.size());
        Assertions.assertEquals("one@two.com", userInfos.get(0).getSendTo());

        put(buildURI(USER_INFO_ID, Map.of("id", userInfo.getId())),
                new MessageUserInfoCmd.UserInfoRequest("two@one.com", "other"));

        userInfos = getAll(USER_INFO,
                Argument.listOf(MessageUserInfoCmd.ApiUserInfo.class));
        Assertions.assertEquals(1, userInfos.size());
        Assertions.assertEquals("two@one.com", userInfos.get(0).getSendTo());

        delete(USER_INFO + "/" + userInfo.getId());

        userInfos = getAll(USER_INFO,
                Argument.listOf(MessageUserInfoCmd.ApiUserInfo.class));
        Assertions.assertEquals(0, userInfos.size());
    }

    // trigger

    @Test
    void testCreateInvalidTrigger() {
        Assertions.assertThrows(HttpClientResponseException.class,
                () -> post(TRIGGER, new MessageTriggerConfigCmd.TriggerConfigRequest(),
                        MessageTriggerConfigCmd.ApiTriggerConfig.class));
    }

    @Test
    void testCrudMessageTrigger() {
        MessageUserInfoCmd.ApiUserInfo userInfo = post(USER_INFO,
                new MessageUserInfoCmd.UserInfoRequest("one@two.com", null),
                MessageUserInfoCmd.ApiUserInfo.class).body();
        Assertions.assertNotNull(userInfo);

        MessageTriggerConfigCmd.ApiTriggerConfig trigger = post(TRIGGER,
                new MessageTriggerConfigCmd.TriggerConfigRequest(
                        MessageTrigger.CREDENTIAL_PROPOSAL, null, userInfo.getId()),
                MessageTriggerConfigCmd.ApiTriggerConfig.class).body();
        Assertions.assertNotNull(trigger);

        List<MessageTriggerConfigCmd.ApiTriggerConfig> triggers = getAll(TRIGGER,
                Argument.listOf(MessageTriggerConfigCmd.ApiTriggerConfig.class));
        Assertions.assertEquals(1, triggers.size());
        Assertions.assertEquals(MessageTrigger.CREDENTIAL_PROPOSAL, triggers.get(0).getTrigger());

        MessageTemplateCmd.ApiMessageTemplate template = post(TEMPLATE,
                new MessageTemplateCmd.MessageTemplateRequest(null, "myTemplate"),
                MessageTemplateCmd.ApiMessageTemplate.class).body();
        Assertions.assertNotNull(template);

        put(buildURI(TRIGGER_ID, Map.of("id", trigger.getId())),
                new MessageTriggerConfigCmd.TriggerConfigRequest(
                        MessageTrigger.PRESENTATION_REQUEST, template.getId(), userInfo.getId()));

        Assertions.assertThrows(HttpClientResponseException.class, () -> delete(TEMPLATE + "/" + template.getId()));
        Assertions.assertThrows(HttpClientResponseException.class, () -> delete(USER_INFO + "/" + userInfo.getId()));

        triggers = getAll(TRIGGER,
                Argument.listOf(MessageTriggerConfigCmd.ApiTriggerConfig.class));
        Assertions.assertEquals(1, triggers.size());
        Assertions.assertEquals(MessageTrigger.PRESENTATION_REQUEST, triggers.get(0).getTrigger());
        Assertions.assertEquals(template.getId(), triggers.get(0).getTemplate().getId());

        delete(TRIGGER + "/" + trigger.getId());

        triggers = getAll(TRIGGER,
                Argument.listOf(MessageTriggerConfigCmd.ApiTriggerConfig.class));
        Assertions.assertEquals(0, triggers.size());

        delete(TEMPLATE + "/" + template.getId());
        delete(USER_INFO + "/" + userInfo.getId());
    }
}
