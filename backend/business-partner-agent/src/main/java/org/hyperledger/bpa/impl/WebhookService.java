/*
 * Copyright (c) 2020 - for information on the respective copyright owner
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

import io.micronaut.core.util.StringUtils;
import io.micronaut.data.exceptions.DataAccessException;
import io.micronaut.scheduling.annotation.Async;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.core.RegisteredWebhook;
import org.hyperledger.bpa.core.RegisteredWebhook.RegisteredWebhookResponse;
import org.hyperledger.bpa.core.RegisteredWebhook.WebhookCredentials;
import org.hyperledger.bpa.core.RegisteredWebhook.WebhookEventType;
import org.hyperledger.bpa.core.WebhookEvent;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.model.BPAWebhook;
import org.hyperledger.bpa.repository.BPAWebhookRepository;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Slf4j
@Singleton
public class WebhookService {

    static final MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient okClient = new OkHttpClient();

    @Inject
    Converter conv;

    @Inject
    BPAWebhookRepository repo;

    public List<RegisteredWebhookResponse> listRegisteredWebhooks() {
        List<RegisteredWebhookResponse> result = new ArrayList<>();
        repo.findAll().forEach(h -> {
            final RegisteredWebhook rh = conv.fromMap(h.getWebhook(), RegisteredWebhook.class);
            result.add(new RegisteredWebhookResponse(h.getId(), rh));
        });
        return result;
    }

    public RegisteredWebhookResponse registerWebhook(@NonNull RegisteredWebhook hook) {
        checkUrl(hook.getUrl());
        final Map<String, Object> map = conv.toMap(hook);
        try {
            BPAWebhook dbHook = repo.save(BPAWebhook.builder().webhook(map).build());
            return new RegisteredWebhookResponse(dbHook.getId(), hook);
        } catch (@SuppressWarnings("unused") DataAccessException e) {
            throw new WrongApiUsageException("Webhook for url: " + hook.getUrl() + " is already registered");
        }
    }

    public Optional<RegisteredWebhookResponse> updateRegisteredWebhook(
            @NonNull UUID id, @NonNull RegisteredWebhook hook) {
        checkUrl(hook.getUrl());
        Optional<RegisteredWebhookResponse> result = Optional.empty();
        final Map<String, Object> map = conv.toMap(hook);
        final Optional<BPAWebhook> existing = repo.findById(id);
        if (existing.isPresent()) {
            existing.get().setWebhook(map);
            try {
                final BPAWebhook updated = repo.update(existing.get());
                return Optional.of(new RegisteredWebhookResponse(updated.getId(), hook));
            } catch (@SuppressWarnings("unused") DataAccessException e) {
                throw new WrongApiUsageException("Webhook for url: " + hook.getUrl() + " is already registered");
            }
        }
        return result;
    }

    public void deleteRegisteredWebhook(UUID id) {
        repo.deleteById(id);
    }

    @Async
    public void convertAndSend(@NonNull WebhookEventType eventType, @NonNull Object msg) {
        // Not a parallel stream for now to keep it simple
        repo.findByEventType(eventType).forEach(e -> {
            final RegisteredWebhook hook = conv.fromMap(e.getWebhook(), RegisteredWebhook.class);
            try {
                WebhookEvent<?> event = WebhookEvent
                        .builder()
                        .payload(msg)
                        .type(eventType)
                        .sent(Instant.now().toEpochMilli())
                        .build();

                conv.writeValueAsString(event).ifPresent(json -> {

                    Request.Builder request = new Request.Builder()
                            .url(hook.getUrl())
                            .post(RequestBody.create(json, JSON_TYPE));
                    addBasicAuthHeaderIfSet(request, hook);

                    try (Response response = okClient.newCall(request.build()).execute()) {
                        if (!response.isSuccessful()) {
                            String body = response.body() != null ? response.body().toString() : "";
                            log.error("Call to {} falied, code: {}, msg: {}",
                                    hook.getUrl(), response.code(), body);
                        }
                    } catch (IOException ex) {
                        log.error("Call to " + hook.getUrl() + " failed", ex);
                    }
                });
            } catch (Exception e1) {
                log.error("Could not send webhook for url: {}", hook.getUrl(), e1);
            }
        });
    }

    // TODO use a hibernate validator
    @SuppressWarnings("unused")
    private static void checkUrl(String url) {
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            throw new WrongApiUsageException("Not a valid URL: " + url);
        }
    }

    private static void addBasicAuthHeaderIfSet(Request.Builder b, RegisteredWebhook hook) {
        final WebhookCredentials creds = hook.getCredentials();
        if (creds != null
                && StringUtils.isNotEmpty(creds.getUsername())) {
            String basic = "Basic ";
            String base64 = Base64.getEncoder()
                    .encodeToString((creds.getUsername() + ":" + creds.getPassword())
                            .getBytes(StandardCharsets.UTF_8));
            b.addHeader("Authorization", basic + base64);
        }
    }
}
