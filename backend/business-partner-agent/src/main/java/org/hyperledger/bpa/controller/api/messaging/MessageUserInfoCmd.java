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
import lombok.*;
import org.hyperledger.bpa.persistence.model.messaging.MessageType;
import org.hyperledger.bpa.persistence.model.messaging.MessageUserInfo;

import javax.validation.constraints.NotNull;
import java.util.UUID;

public class MessageUserInfoCmd {

    @Data
    @NoArgsConstructor
    @Introspected
    public static final class UserInfoRequest {
        @NotNull
        private String sendTo;
        private String label;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static final class ApiUserInfo {
        private UUID id;
        private Long createdAt;
        private Long updatedAt;
        private MessageType type;
        private String sendTo;
        private String label;

        public static ApiUserInfo fromMessageUserInfo(@NonNull MessageUserInfo i) {
            return ApiUserInfo.builder()
                    .id(i.getId())
                    .createdAt(i.getCreatedAt().toEpochMilli())
                    .updatedAt(i.getUpdatedAt().toEpochMilli())
                    .type(i.getType())
                    .sendTo(i.getSendTo())
                    .label(i.getLabel())
                    .build();
        }
    }
}
