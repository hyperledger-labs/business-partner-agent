/**
 * Copyright (c) 2020 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/organizational-agent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hyperledger.oa.core;

import java.util.List;
import java.util.UUID;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

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
    private List<WebhookEvent> registeredEvent;
    private WebhookCredentials credentials;

    public RegisteredWebhook(RegisteredWebhook hook) {
        super();
        this.url = hook.getUrl();
        this.registeredEvent = hook.getRegisteredEvent();
        this.credentials = hook.getCredentials();
    }

    public enum WebhookEvent {
        DOCUMENT_UPDATE,
        CREDENTIAL_UPDATE;
    }

    @Data
    @NoArgsConstructor
    public static final class WebhookCredentials {
        // Hardcoded default for now
        private String type = "BasicAuth";
        private String username;
        private String password;
    }

    @SuperBuilder
    @Data
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static class RegisteredWebhookMessage extends RegisteredWebhook {
        private UUID id;

        public RegisteredWebhookMessage(UUID id, RegisteredWebhook hook) {
            super(hook);
            this.id = id;
        }
    }
}
