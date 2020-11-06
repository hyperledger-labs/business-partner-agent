package org.hyperledger.oa.impl;

import io.micronaut.core.util.CollectionUtils;
import io.micronaut.scheduling.annotation.Async;
import io.micronaut.scheduling.annotation.Scheduled;
import io.micronaut.websocket.WebSocketBroadcaster;
import io.micronaut.websocket.WebSocketSession;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.oa.controller.api.WebSocketMessageBody;
import org.hyperledger.oa.impl.util.Converter;
import org.hyperledger.oa.model.MessageQueue;
import org.hyperledger.oa.repository.MessageQueueRepository;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Singleton
public class MessageService {

    @Inject
    WebSocketBroadcaster broadcaster;

    @Inject
    MessageQueueRepository queue;

    @Inject
    Converter conv;

    private final Map<String, WebSocketSession> connected = new ConcurrentHashMap<>();

    public void addSession(WebSocketSession session) {
        connected.put(session.getId(), session);
    }

    public void removeSession(WebSocketSession session) {
        connected.remove(session.getId());
    }

    public boolean hasConnectedSessions() {
        return CollectionUtils.isNotEmpty(connected);
    }

    // called by impl
    @Async
    public void sendMessage(WebSocketMessageBody message) {
        try {
            if (hasConnectedSessions()) {
                broadcaster.broadcastSync(message);
            } else {
                MessageQueue msg = MessageQueue.builder().message(conv.toMap(message)).build();
                queue.save(msg);
            }
        } catch (Exception e) {
            log.error("Could not send websocket message.", e);
        }
    }

    // called by controller
    public void sendStored(WebSocketSession session) {
        queue.findAll().forEach(msg -> {
            WebSocketMessageBody toSend = conv.fromMap(msg.getMessage(), WebSocketMessageBody.class);
            session.sendSync(toSend);
        });
        queue.deleteAll();
    }

    @Scheduled(fixedRate = "1h")
    public void cleanupStaleSessions() {
        log.debug("Cleaning up stale websocket sessions.");
        List<String> stale = new ArrayList<>();
        connected.forEach((k, v) -> {
            if (!v.isOpen()) {
                stale.add(k);
            }
        });
        log.debug("Found {} session(s), {} of them are stale.", connected.size(), stale.size());
        stale.forEach(connected::remove);
    }

//    @Scheduled(fixedRate = "5s")
//    public void sendTestMessage() {
//        this.sendMessage(WebSocketMessageBody.of(WebSocketMessageBody.WebSocketMessage
//                .builder()
//                .type(WebSocketMessageBody.WebSocketMessageType.PARTNER)
//                .build()));
//        this.cleanupStaleSessions();
//    }
}
