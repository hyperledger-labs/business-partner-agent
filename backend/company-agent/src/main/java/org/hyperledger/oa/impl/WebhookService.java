/**
 * Copyright (c) 2020 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/organizational-agent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hyperledger.oa.impl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.hyperledger.oa.api.exception.WrongApiUsageException;
import org.hyperledger.oa.core.RegisteredWebhook;
import org.hyperledger.oa.core.RegisteredWebhook.RegisteredWebhookMessage;
import org.hyperledger.oa.core.RegisteredWebhook.WebhookEvent;
import org.hyperledger.oa.impl.util.Converter;
import org.hyperledger.oa.model.BPAWebhook;
import org.hyperledger.oa.repository.BPAWebhookRepository;

import io.micronaut.data.exceptions.DataAccessException;
import io.micronaut.scheduling.annotation.Async;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Slf4j
@Singleton
public class WebhookService {

    private final OkHttpClient okClient = new OkHttpClient();

    @Inject
    Converter conv;

    @Inject
    BPAWebhookRepository repo;

    public List<RegisteredWebhookMessage> listRegisteredWebhooks() {
        List<RegisteredWebhookMessage> result = new ArrayList<>();
        repo.findAll().forEach(h -> {
            final RegisteredWebhook rh = conv.fromMap(h.getWebhook(), RegisteredWebhook.class);
            result.add(new RegisteredWebhookMessage(h.getId(), rh));
        });
        return result;
    }

    public RegisteredWebhookMessage registerWebhook(RegisteredWebhook hook) {
        checkUrl(hook.getUrl());
        final Map<String, Object> map = conv.toMap(hook);
        try {
            BPAWebhook dbHook = repo.save(BPAWebhook.builder().webhook(map).build());
            return new RegisteredWebhookMessage(dbHook.getId(), hook);
        } catch (@SuppressWarnings("unused") DataAccessException e) {
            throw new WrongApiUsageException("Webhook for url: " + hook.getUrl() + " is already registered");
        }
    }

    public Optional<RegisteredWebhookMessage> updateRegisteredWebhook(UUID id, RegisteredWebhook hook) {
        checkUrl(hook.getUrl());
        Optional<RegisteredWebhookMessage> result = Optional.empty();
        final Map<String, Object> map = conv.toMap(hook);
        final Optional<BPAWebhook> existing = repo.findById(id);
        if (existing.isPresent()) {
            existing.get().setWebhook(map);
            try {
                final BPAWebhook updated = repo.update(existing.get());
                return Optional.of(new RegisteredWebhookMessage(updated.getId(), hook));
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
    public void convertAndSend(@NonNull WebhookEvent eventType, @NonNull Object msg) {
        // Not a parallel stream for now to keep it simple
        repo.findByEventType(eventType).forEach(e -> {
            final RegisteredWebhook hook = conv.fromMap(e.getWebhook(), RegisteredWebhook.class);
            Request request = new Request.Builder()
                    .url(hook.getUrl())
                    .build();
            try (Response response = okClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String body = response.body() != null ? response.body().toString() : "";
                    log.error("Call to {} falied, code: {}, msg: {}",
                            hook.getUrl(), Integer.valueOf(response.code()), body);
                    response.body().close();
                }
            } catch (IOException ex) {
                log.error("Call to " + hook.getUrl() + " failed", ex);
            }
        });
    }

    // TODO use a validator
    @SuppressWarnings("unused")
    private static void checkUrl(String url) {
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            throw new WrongApiUsageException("Not a valid URL: " + url);
        }
    }
}
