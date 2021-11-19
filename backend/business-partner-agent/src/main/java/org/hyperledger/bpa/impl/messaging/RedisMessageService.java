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

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.KeyValue;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.websocket.WebSocketBroadcaster;
import io.micronaut.websocket.WebSocketSession;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.bpa.controller.api.WebSocketMessageBody;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.repository.MessageQueueRepository;
import org.slf4j.Logger;

@Slf4j
@Singleton
@Requires(property = "micronaut.session.http.redis.enabled")
public final class RedisMessageService implements MessageService {

    @Value("${micronaut.application.instance.id}")
    String instanceId;

    @Inject
    WebSocketBroadcaster broadcaster;

    @Inject
    @Getter
    MessageQueueRepository queue;

    @Inject
    @Getter
    Converter conv;

    @Inject
    ObjectMapper mapper;

    @Inject
    StatefulRedisConnection<String, String> redis;

    @Inject
    StatefulRedisPubSubConnection<String, String> pubSub;

    @EventListener
    public void onServiceStartedEvent(@SuppressWarnings("unused") StartupEvent startEvent) {
        pubSub.reactive().subscribe(baseChannel()).subscribe();
        pubSub.reactive().observeChannels()
                .doOnNext(pm -> {
                    log.debug("Reactive handler sending to channel: {}, message, {}", pm.getChannel(), pm.getMessage());
                    if (StringUtils.equals(pm.getChannel(), baseChannel())) {
                        broadcaster.broadcastSync(pm.getMessage());
                    }
                })
                .doOnError(e -> log.error("Error in reactive observer", e))
                .subscribe();
    }

    public void subscribe(WebSocketSession session) {
        redis.sync().hset(baseChannel(), session.getId(), null);
    }

    public void unsubscribe(WebSocketSession session) {
        redis.sync().hdel(baseChannel(), session.getId());
    }

    public boolean hasConnectedSessions() {
        KeyValue<String, String> subscriptions = redis.reactive().hgetall(baseChannel()).blockFirst();
        return subscriptions != null && !subscriptions.isEmpty();
    }

    public void send(WebSocketMessageBody body) {
        try {
            String message = mapper.writeValueAsString(body);
            redis.reactive().publish(baseChannel(), message).block();
        } catch (JacksonException e) {
            log.error("Could not send message to channel", e);
        }
    }

    public Logger getLog() {
        return log;
    }
}
