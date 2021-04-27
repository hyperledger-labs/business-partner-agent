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
package org.hyperledger.bpa.controller;

import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.websocket.WebSocketBroadcaster;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.OnClose;
import io.micronaut.websocket.annotation.OnMessage;
import io.micronaut.websocket.annotation.OnOpen;
import io.micronaut.websocket.annotation.ServerWebSocket;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.bpa.impl.MessageService;

import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
@ServerWebSocket("/events")
@Secured(SecurityRule.IS_AUTHENTICATED)
public class WebsocketController {

    @Inject
    MessageService msg;

    @SuppressWarnings("unused")
    private final WebSocketBroadcaster broadcaster;

    public WebsocketController(WebSocketBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @OnOpen
    public void onOpen(WebSocketSession session) {
        log.debug("New websocket session: {}", session.getId());
        msg.addSession(session);
        msg.sendStored(session);
    }

    @OnMessage
    public void onMessage(String message, WebSocketSession session) {
        log.debug("Received websocket message: {} -> {}", session.getId(), message);
    }

    @OnClose
    public void onClose(WebSocketSession session) {
        log.debug("Websocket session disconnected: {}", session.getId());
        msg.removeSession(session);
    }
}
