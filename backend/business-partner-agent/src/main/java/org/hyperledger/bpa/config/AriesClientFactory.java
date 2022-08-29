/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
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
package org.hyperledger.bpa.config;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.env.Environment;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.Getter;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.AriesWebSocketClient;
import org.hyperledger.aries.config.UriUtil;
import org.hyperledger.aries.webhook.EventHandler;

import java.util.List;

@Factory
@Requires(notEnv = Environment.TEST)
public class AriesClientFactory {

    @Value("${bpa.acapy.url}")
    private String url;
    @Value("${bpa.acapy.apiKey}")
    private String apiKey;

    @Singleton
    public AriesClient ariesClient() {
        return AriesClient.builder()
                .url(url)
                .apiKey(apiKey)
                .build();
    }

    @Bean(preDestroy = "close")
    public AriesWebSocketClient ariesWebSocketClient(List<EventHandler> handlers) {
        return AriesWebSocketClient.builder()
                .url(UriUtil.httpToWs(url))
                .apiKey(apiKey)
                .handler(handlers)
                .reactiveBufferSize(20)
                .build();
    }

    @Singleton
    @Requires(notEnv = Environment.TEST)
    public record EagerWebsocketClient(@Inject @Getter AriesWebSocketClient ac) {
        @EventListener
        public void onServiceStartedEvent(StartupEvent startEvent) {
            ac.basicMessage(); // only needed to reference the bean so that it is initiated
        }
    }
}
