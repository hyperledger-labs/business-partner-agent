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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.aries.api.message.BasicMessage;
import org.hyperledger.bpa.api.exception.NetworkException;
import org.hyperledger.bpa.api.exception.PartnerException;
import org.hyperledger.bpa.impl.aries.ConnectionManager;
import org.hyperledger.bpa.model.ChatMessage;
import org.hyperledger.bpa.repository.PartnerRepository;

import java.util.UUID;

@Slf4j
@NoArgsConstructor
@Singleton
public class ChatMessageManager {

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    ConnectionManager cm;

    @Inject
    ChatMessageService chatMessageService;

    @Inject
    NotificationService notificationService;

    public void handleIncomingMessage(@NonNull BasicMessage basicMessage) {
        // if this was more complicated (ie many states and roles), we would
        // put this in its own class.
        // but since this is simple, we will place it with the other basic message/chat
        // message logic.

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
                try {
                    ChatMessage chatMessage = chatMessageService.saveIncomingMessage(p, basicMessage.getContent());
                    notificationService.newIncomingMessage(p, chatMessage);
                } catch (Exception e) {
                    log.error(
                            "Error handling incoming basic message. Chat Message not saved. Partner ID = {}, Connection ID = {}, Message ID = {}",
                            p.getId().toString(), basicMessage.getConnectionId(), basicMessage.getMessageId());
                }
            },
                    () -> log.error("Error handling incoming basic message. Partner not found for Connection ID = {}",
                            basicMessage.getConnectionId()));
        } else {
            log.debug("Not persisting basic message. Message has no content. Connection ID = {}, Message ID = {}",
                    basicMessage.getConnectionId(), basicMessage.getMessageId());
        }
    }

    public void sendMessage(@NonNull UUID partnerId, @NonNull String content) {
        // want to send an outgoing message to a partner...
        // workflow:
        // get partner, check if we can send (connection id and aries support required)
        // all ok, then send it via ARIES
        // if sent, then we need to persist it

        // since we are handling user initiated work, we need to throw exceptions if
        // something doesn't work
        // perhaps the user can fix it, but at least they will know what they expected
        // to happen didn't happen.
        partnerRepo.findById(partnerId).ifPresentOrElse(p -> {
            if (StringUtils.isNotEmpty(p.getConnectionId()) && p.getAriesSupport()) {
                if (cm.sendMessage(p.getConnectionId(), content)) {
                    chatMessageService.saveOutgoingMessage(p, content);
                } else {
                    throw new NetworkException("Could not send message to partner; Aries network issue.");
                }
            } else {
                log.error(
                        "Error handling outgoing chat message. Partner has no connection id or missing Aries Support. Partner ID = {}, Connection ID = {}",
                        partnerId, p.getConnectionId());
                throw new PartnerException("Partner does not accept messages.");
            }
        },
                () -> {
                    log.error("Error handling outgoing chat message. Partner not found for ID = {}", partnerId);
                    throw new PartnerException(String.format("Partner not found for ID = '%s'", partnerId));
                });
    }
}
