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
package org.hyperledger.bpa.controller.api.messaging;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.Nullable;
import lombok.*;
import org.hyperledger.bpa.persistence.model.messaging.MessageTrigger;
import org.hyperledger.bpa.persistence.model.messaging.MessageTriggerConfig;

import javax.validation.constraints.NotNull;
import java.util.UUID;

public class MessageTriggerConfigCmd {

    @Data
    @NoArgsConstructor
    @Introspected
    public static final class TriggerConfigRequest {
        @NotNull
        private MessageTrigger trigger;
        @Nullable
        private UUID messageTemplateId;
        @NotNull
        private UUID userInfoId;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static final class ApiTriggerConfig {
        private UUID id;
        private Long createdAt;
        private Long updatedAt;
        private MessageTrigger trigger;
        private MessageUserInfoCmd.ApiUserInfo userInfo;
        private MessageTemplateCmd.ApiMessageTemplate template;

        public static ApiTriggerConfig fromMessageTriggerConfig(@NonNull MessageTriggerConfig c) {
            return ApiTriggerConfig.builder()
                    .id(c.getId())
                    .createdAt(c.getCreatedAt().toEpochMilli())
                    .updatedAt(c.getUpdatedAt().toEpochMilli())
                    .trigger(c.getTrigger())
                    .userInfo(c.getUserInfo() != null
                            ? MessageUserInfoCmd.ApiUserInfo.fromMessageUserInfo(c.getUserInfo())
                            : null)
                    .template(c.getTemplate() != null
                            ? MessageTemplateCmd.ApiMessageTemplate.fromMessageTemplate(c.getTemplate())
                            : null)
                    .build();
        }
    }
}
