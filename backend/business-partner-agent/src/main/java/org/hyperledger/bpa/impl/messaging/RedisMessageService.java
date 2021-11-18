package org.hyperledger.bpa.impl.messaging;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.scheduling.annotation.Async;
import io.micronaut.scheduling.annotation.Scheduled;
import io.micronaut.websocket.WebSocketBroadcaster;
import io.micronaut.websocket.WebSocketSession;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.config.GsonConfig;
import org.hyperledger.bpa.controller.api.WebSocketMessageBody;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.model.MessageQueue;
import org.hyperledger.bpa.repository.MessageQueueRepository;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Singleton
@Requires(property = "micronaut.session.http.redis.enabled")
public class RedisMessageService implements MessageService {

    @Value("${micronaut.application.instance.id}")
    String instanceId;

    @Inject
    WebSocketBroadcaster broadcaster;

    @Inject
    MessageQueueRepository queue;

    @Inject
    Converter conv;

    @Inject
    StatefulRedisConnection<String, String> redis;

    private final StatefulRedisPubSubConnection<String, String> pubSub;

    public RedisMessageService(StatefulRedisPubSubConnection<String, String> pubSub) {

        this.pubSub = pubSub;

        this.pubSub.reactive().observeChannels()
                .doOnNext(pm -> {
                    log.debug("Reactive handler sending to channel: {}, message, {}", pm.getChannel(), pm.getMessage());
                    broadcaster.broadcastSync(pm.getMessage());
                })
                .doOnError(e -> log.error("Error in reactive observer", e))
                .subscribe();
    }

    public void subscribe(WebSocketSession session) {
        log.debug("Subscribing session: {}", session.getId());
        pubSub.reactive().subscribe(baseChannel() + session.getId()).subscribe();
    }

    public void unsubscribe(WebSocketSession session) {
        log.debug("Unsubscribing session: {} from redis event handler", session.getId());
        pubSub.reactive().unsubscribe(baseChannel() + session.getId()).block();
    }

    public boolean hasConnectedSessions() {
        return CollectionUtils.isNotEmpty(findConnectedSessions());
    }

    @Async
    public void sendMessage(WebSocketMessageBody message) {
        try {
            if (hasConnectedSessions()) {
                sendToSubscribedChannels(message);
            } else {
                MessageQueue msg = MessageQueue.builder().message(conv.toMap(message)).build();
                queue.save(msg);
            }
        } catch (Exception e) {
            log.error("Could not send websocket message.", e);
        }
    }

    public void sendStored(WebSocketSession session) {
        StreamSupport.stream(queue.findAll().spliterator(), false)
                .filter(msg -> msg.getMessage() != null)
                .forEach(msg -> {
                    WebSocketMessageBody toSend = conv.fromMap(msg.getMessage(), WebSocketMessageBody.class);
                    sendToSubscribedChannels(toSend);
        });
        queue.deleteAll();
    }

    private void sendToSubscribedChannels(WebSocketMessageBody body) {
        String message = GsonConfig.jacksonBehaviour().toJson(body);
        findConnectedSessions().forEach(s -> redis
                .reactive()
                .publish(s, message)
                .block());
    }

    private List<String> findConnectedSessions() {
        List<String> channels = redis.reactive().pubsubChannels().collectList().block();
        return channels != null
                ? channels.stream().filter(c -> c.startsWith(baseChannel())).collect(Collectors.toList())
                : List.of();
    }

    @Scheduled(fixedRate = "10s", initialDelay = "10s")
    void writeToSocket() {
        sendMessage(WebSocketMessageBody.of(WebSocketMessageBody.WebSocketMessage.builder()
                        .linkId("1")
                        .type(WebSocketMessageBody.WebSocketMessageType.ON_CREDENTIAL_OFFERED)
                        .info(instanceId + " Hallo to all")
                .build()));
        // redis.reactive().publish(WebsocketControllerRedis.BASE_CHANNEL, instanceId + " Hallo to all").block();
    }
}
