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
package org.hyperledger.bpa.repository;

import io.micronaut.data.exceptions.DataAccessException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.apache.commons.lang3.RandomStringUtils;
import org.hyperledger.bpa.core.RegisteredWebhook;
import org.hyperledger.bpa.core.RegisteredWebhook.WebhookEventType;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.model.BPAWebhook;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
class BPAWebhookRepositoryTest {

    @Inject
    BPAWebhookRepository repo;

    @Inject
    Converter conv;

    @Test
    void testGetByUrl() {
        final String url = "https://test.me";
        RegisteredWebhook h = RegisteredWebhook
                .builder()
                .url(url)
                .build();
        BPAWebhook entity = BPAWebhook
                .builder()
                .webhook(conv.toMap(h))
                .build();
        BPAWebhook saved = repo.save(entity);

        final Optional<BPAWebhook> byUrl = repo.findByUrl(url);
        assertTrue(byUrl.isPresent());
        assertEquals(saved.getId(), byUrl.get().getId());
    }

    @Test
    void testPersistSameUrlTwice() {
        final String url = "https://test.me";
        RegisteredWebhook h = RegisteredWebhook
                .builder()
                .url(url)
                .build();
        BPAWebhook entity = BPAWebhook
                .builder()
                .webhook(conv.toMap(h))
                .build();
        repo.save(entity);

        assertThrows(DataAccessException.class, () -> repo.save(entity));
    }

    @Test
    void testFinDbyType() {
        repo.save(randomWebhook(List.of(WebhookEventType.PARTNER_UPDATE)));
        repo.save(randomWebhook(List.of(WebhookEventType.PARTNER_UPDATE)));
        repo.save(randomWebhook(List.of(WebhookEventType.PARTNER_ADD)));
        repo.save(randomWebhook(List.of()));

        final List<BPAWebhook> two = repo.findByEventType(WebhookEventType.PARTNER_UPDATE);
        assertEquals(2, two.size());

        final List<BPAWebhook> one = repo.findByEventType(WebhookEventType.PARTNER_ADD);
        assertEquals(1, one.size());
    }

    private BPAWebhook randomWebhook(List<WebhookEventType> types) {
        RegisteredWebhook h = RegisteredWebhook
                .builder()
                .url(RandomStringUtils.random(8))
                .registeredEvent(types)
                .build();
        return BPAWebhook
                .builder()
                .webhook(conv.toMap(h))
                .build();
    }

}
