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
package org.hyperledger.bpa.impl.messaging;

import io.micronaut.scheduling.annotation.Async;
import io.micronaut.websocket.WebSocketSession;
import org.hyperledger.bpa.controller.api.WebSocketMessageBody;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.model.MessageQueue;
import org.hyperledger.bpa.repository.MessageQueueRepository;
import org.slf4j.Logger;

import java.util.stream.StreamSupport;

public interface MessageService {

    void subscribe(WebSocketSession session);

    void unsubscribe(WebSocketSession session);

    boolean hasConnectedSessions();

    void send(WebSocketMessageBody message);

    MessageQueueRepository getQueue();

    Converter getConv();

    Logger getLog();

    default String baseChannel() {
        return "bpa-messages";
    }

    /** Called by impl */
    @Async
    default void sendMessage(WebSocketMessageBody message) {
        try {
            if (hasConnectedSessions()) {
                send(message);
            } else {
                MessageQueue msg = MessageQueue.builder().message(getConv().toMap(message)).build();
                getQueue().save(msg);
            }
        } catch (Exception e) {
            getLog().error("Could not send websocket message.", e);
        }
    }

    /** Called by controller */
    default void sendStored() {
        StreamSupport.stream(getQueue().findAll().spliterator(), false)
                .filter(msg -> msg.getMessage() != null)
                .forEach(msg -> {
                    WebSocketMessageBody toSend = getConv().fromMap(msg.getMessage(), WebSocketMessageBody.class);
                    send(toSend);
                });
        getQueue().deleteAll();
    }
}
