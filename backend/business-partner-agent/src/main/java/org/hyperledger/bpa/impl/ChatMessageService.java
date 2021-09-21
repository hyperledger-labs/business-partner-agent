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
import org.hyperledger.bpa.api.exception.DataPersistenceException;
import org.hyperledger.bpa.impl.aries.ConnectionManager;
import org.hyperledger.bpa.model.ChatMessage;
import org.hyperledger.bpa.model.Partner;
import org.hyperledger.bpa.repository.ChatMessageRepository;
import org.hyperledger.bpa.repository.PartnerRepository;

import java.util.List;
import java.util.UUID;

@Slf4j
@NoArgsConstructor
@Singleton
public class ChatMessageService {

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    ChatMessageRepository chatMsgRepo;

    @Inject
    ConnectionManager cm;

    public List<ChatMessage> getMessagesForPartner(@NonNull String partnerId) {
        return chatMsgRepo.findByPartnerIdOrderByCreatedAtAsc(UUID.fromString(partnerId));
    }

    public ChatMessage saveIncomingMessage(@NonNull Partner partner, @NonNull String content) {
        try {
            return chatMsgRepo.save(ChatMessage.builder().partner(partner).content(content).incoming(true).build());
        } catch (Exception e) {
            String msg = "Error saving (incoming) chat message";
            log.error(msg, e);
            throw new DataPersistenceException(msg);
        }
    }

    public ChatMessage saveOutgoingMessage(@NonNull Partner partner, @NonNull String content) {
        try {
            return chatMsgRepo.save(ChatMessage.builder().partner(partner).content(content).incoming(false).build());
        } catch (Exception e) {
            String msg = "Error saving (outgoing) chat message";
            log.error(msg, e);
            throw new DataPersistenceException(msg);
        }
    }

    public void deletePartnerMessages(@NonNull Partner partner) {
        chatMsgRepo.deleteByPartnerId(partner.getId());
    }
}
