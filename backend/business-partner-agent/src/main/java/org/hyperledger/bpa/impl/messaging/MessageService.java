package org.hyperledger.bpa.impl.messaging;

import io.micronaut.websocket.WebSocketSession;
import org.hyperledger.bpa.controller.api.WebSocketMessageBody;

public interface MessageService {

    void subscribe(WebSocketSession session);

    void unsubscribe(WebSocketSession session);

    boolean hasConnectedSessions();

    /** Called by impl */
    void sendMessage(WebSocketMessageBody message);

    /** Called by controller */
    void sendStored(WebSocketSession session);

    default String baseChannel() {
        return "bpa-messages-";
    }
}
