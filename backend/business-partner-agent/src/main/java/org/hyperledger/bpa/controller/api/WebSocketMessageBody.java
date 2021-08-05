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
package org.hyperledger.bpa.controller.api;

import io.micronaut.core.annotation.Nullable;
import lombok.*;
import org.hyperledger.aries.api.message.BasicMessage;
import org.hyperledger.bpa.api.PartnerAPI;

/**
 * Websocket events
 */
@Data
@NoArgsConstructor
@RequiredArgsConstructor(staticName = "of")
public class WebSocketMessageBody {

    private final long id = System.nanoTime();

    private final long timestamp = System.currentTimeMillis();

    @NonNull
    private WebSocketMessage message;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static final class WebSocketMessage {
        private WebSocketMessageType type;
        private String linkId;
        private Object info;
        private PartnerAPI partner;
    }

    public enum WebSocketMessageType {
        ON_MESSAGE_RECEIVED,
        ON_CREDENTIAL_ADDED,
        ON_PARTNER_REQUEST_COMPLETED,
        ON_PARTNER_REQUEST_RECEIVED,
        ON_PARTNER_ADDED,
        ON_PARTNER_ACCEPTED,
        ON_PARTNER_REMOVED,
        ON_PRESENTATION_VERIFIED,
        ON_PRESENTATION_PROVED,
        ON_PRESENTATION_REQUEST_DECLINED,
        ON_PRESENTATION_REQUEST_DELETED,
        ON_PRESENTATION_REQUEST_RECEIVED,
        ON_PRESENTATION_REQUEST_SENT,
        TASK_ADDED,
        TASK_COMPLETED
    }

    public static WebSocketMessageBody message(PartnerAPI partner, BasicMessage message) {
        return notificationEvent(WebSocketMessageType.ON_MESSAGE_RECEIVED,
                partner.getId(),
                PartnerMessage.builder()
                        .partnerId(partner.getId())
                        .messageId(message.getMessageId())
                        .content(message.getContent())
                        .build(),
                partner);
    }

    public static WebSocketMessageBody notificationEvent(@NonNull WebSocketMessageType type,
            @Nullable String linkId,
            @Nullable Object info,
            @Nullable PartnerAPI partner) {
        return WebSocketMessageBody.of(WebSocketMessage
                .builder()
                .type(type)
                .info(info)
                .partner(partner)
                .linkId(linkId)
                .build());
    }

}
