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
package org.hyperledger.bpa.core;

import lombok.*;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Webhook Container
 *
 */
@Data
@NoArgsConstructor
@SuperBuilder
public class RegisteredWebhook {

    @NotBlank
    private String url;
    @Size(min = 1)
    private List<WebhookEventType> registeredEvent;
    private WebhookCredentials credentials;

    public RegisteredWebhook(RegisteredWebhook hook) {
        super();
        this.url = hook.getUrl();
        this.registeredEvent = hook.getRegisteredEvent();
        this.credentials = hook.getCredentials();
    }

    public enum WebhookEventType {
        ALL,
        PARTNER_ADD,
        PARTNER_UPDATE
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static final class WebhookCredentials {
        // Hardcoded default for now
        public static final String TYPE = "BASIC";
        @Size(min = 1)
        private String username;
        @Size(min = 1)
        private String password;
    }

    @SuperBuilder
    @Data
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class RegisteredWebhookResponse extends RegisteredWebhook {
        private UUID id;

        public RegisteredWebhookResponse(UUID id, RegisteredWebhook hook) {
            super(hook);
            this.id = id;
        }
    }
}
