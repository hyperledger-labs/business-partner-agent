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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.micronaut.core.annotation.Introspected;
import lombok.*;
import org.hyperledger.bpa.impl.verification.input.SanitizedStringDeserializer;
import org.hyperledger.bpa.persistence.model.messaging.MessageTemplate;

import javax.validation.constraints.NotNull;
import java.util.UUID;

public class MessageTemplateCmd {

    @Data
    @NoArgsConstructor
    @Introspected
    public static final class MessageTemplateRequest {

        @JsonDeserialize(using = SanitizedStringDeserializer.class)
        private String subject;

        @NotNull
        @JsonDeserialize(using = SanitizedStringDeserializer.class)
        private String template;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static final class ApiMessageTemplate {
        private UUID id;
        private Long createdAt;
        private Long updatedAt;
        private String subject;
        private String template;

        public static ApiMessageTemplate fromMessageTemplate(@NonNull MessageTemplate t) {
            return ApiMessageTemplate.builder()
                    .id(t.getId())
                    .createdAt(t.getCreatedAt().toEpochMilli())
                    .updatedAt(t.getUpdatedAt().toEpochMilli())
                    .subject(t.getSubject())
                    .template(t.getTemplate())
                    .build();
        }
    }
}
