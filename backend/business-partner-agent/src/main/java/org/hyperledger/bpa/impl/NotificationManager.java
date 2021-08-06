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
package org.hyperledger.bpa.impl;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.bpa.controller.api.WebSocketMessageBody;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.model.ChatMessage;
import org.hyperledger.bpa.model.Partner;

import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@NoArgsConstructor
@Singleton
public class NotificationManager {

    @Inject
    MessageService messageService;

    @Inject
    Converter conv;

    public void newIncomingMessage(@NonNull Partner partner, @NonNull ChatMessage chatMessage) {
        WebSocketMessageBody message = WebSocketMessageBody.message(conv.toAPIObject(partner), chatMessage);
        messageService.sendMessage(message);
    }
}
