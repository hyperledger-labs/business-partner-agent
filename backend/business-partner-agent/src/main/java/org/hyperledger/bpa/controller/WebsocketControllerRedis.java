package org.hyperledger.bpa.controller;

import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands;
import io.micronaut.scheduling.annotation.Async;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.websocket.WebSocketBroadcaster;
import io.micronaut.websocket.WebSocketSession;
import io.micronaut.websocket.annotation.*;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@ServerWebSocket("/events")
@Secured(SecurityRule.IS_AUTHENTICATED)
public class WebsocketControllerRedis {

    public static final String BASE_CHANNEL = "bpa-messages";

    private final StatefulRedisPubSubConnection<String, String> connection;

    @SuppressWarnings("unused")
    private final WebSocketBroadcaster broadcaster;

    public WebsocketControllerRedis(WebSocketBroadcaster broadcaster,
                               StatefulRedisPubSubConnection<String, String> connection) {
        this.broadcaster = broadcaster;
        this.connection = connection;

        connection.reactive().observeChannels()
                .doOnNext(pm -> {
                    log.debug("Reactive handler sending to channel: {}, message, {}", pm.getChannel(), pm.getMessage());
                    broadcaster.broadcastSync(pm.getMessage());
                })
                .doOnError(e -> log.error("Error in reactive observer", e))
                .subscribe();
    }

    @OnOpen
    @Async
    public void onOpen(WebSocketSession session) {
        log.debug("New websocket session: {}", session.getId());
        session.getUserPrincipal().ifPresent(p -> {
            log.debug("Subscribing user: {}", p.getName());
            connection.reactive().subscribe(BASE_CHANNEL).subscribe();
        });
    }

    @OnMessage
    public void onMessage(String message, WebSocketSession session) {
        log.debug("Received websocket message: {} -> {}", session.getId(), message);
    }

    @OnClose
    @Async
    public void onClose(WebSocketSession session) {
        log.debug("Websocket session disconnected: {}", session.getId());
        unsubscribe(session);
    }

    @OnError
    public void onError(Throwable e, WebSocketSession session) {
        log.error("Websocket error: ", e);
        if (session != null && session.isOpen()) {
            unsubscribe(session);
            session.close();
        }
    }

    private void unsubscribe(WebSocketSession session) {
        session.getUserPrincipal().ifPresent(p -> {
            log.debug("Unsubscribing user: {} from redis event handler", p.getName());
            connection.reactive().unsubscribe(BASE_CHANNEL).block();
        });
    }
}
