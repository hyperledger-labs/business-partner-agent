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
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.aries.api.message.BasicMessage;
import org.hyperledger.bpa.impl.aries.ConnectionManager;
import org.hyperledger.bpa.model.ChatMessage;
import org.hyperledger.bpa.repository.PartnerRepository;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.UUID;

@Slf4j
@NoArgsConstructor
@Singleton
public class ChatMessageEventHandler {

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    ConnectionManager cm;

    @Inject
    ChatMessageManager chatMessageManager;

    @Inject
    NotificationManager notificationManager;

    public void handleIncomingMessage(@NonNull BasicMessage basicMessage) {
        // message coming in from aries...
        // workflow:
        // check for content
        // check for partner
        // all ok, then save it
        // if saved, send notification to Frontend for new message
        if (StringUtils.isNotEmpty(basicMessage.getContent())) {
            // find the partner...
            partnerRepo.findByConnectionId(basicMessage.getConnectionId()).ifPresentOrElse(p -> {
                // great, we have a partner...
                // save the message
                ChatMessage chatMessage = chatMessageManager.saveIncomingMessage(p, basicMessage.getContent());
                if (chatMessage != null && chatMessage.getId() != null) {
                    // notify frontend
                    notificationManager.newIncomingMessage(p, chatMessage);
                } else {
                    log.error(
                            "Error handling incoming basic message. Chat Message not saved. Partner ID = '%s', Connection ID = '%s', Message ID = '%s'",
                            p.getId().toString(), basicMessage.getConnectionId(), basicMessage.getMessageId());
                }
            },
                    () -> {
                        log.error("Error handling incoming basic message. Partner not found for Connection ID = '%s'",
                                basicMessage.getConnectionId());
                    });
        } else {
            log.debug("Not persisting basic message. Message has no content. Connection ID = '%s', Message ID = '%s'",
                    basicMessage.getConnectionId(), basicMessage.getMessageId());
        }
    }

    public void handleOutgoingMessage(@NonNull String partnerId, @NonNull String content) {
        // want to send an outgoing message to a partner...
        // workflow:
        // get partner, check if we can send (connection id and aries support required)
        // all ok, then send it via ARIES
        // if send, then we need to persist it
        partnerRepo.findById(UUID.fromString(partnerId)).ifPresentOrElse(p -> {
            if (StringUtils.isNotEmpty(p.getConnectionId()) && p.getAriesSupport()) {
                if (cm.sendMessage(p.getConnectionId(), content)) {
                    ChatMessage chatMessage = chatMessageManager.saveOutgoingMessage(p, content);
                    if (chatMessage == null || chatMessage.getId() == null) {
                        log.error("Error handling outgoing chat message. Chat Message not saved. Partner ID = '%s'",
                                p.getId().toString());
                    }
                }
            } else {
                log.error(
                        "Error handling outgoing chat message. Partner has no connection id or missing Aries Support. Partner ID = '%s', Connection ID = '%s'",
                        partnerId, p.getConnectionId());
            }
        },
                () -> {
                    log.error("Error handling outgoing chat message. Partner not found for ID = '%s'", partnerId);
                });
    }
}
