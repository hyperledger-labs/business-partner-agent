package org.hyperledger.oa.repository;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.RandomStringUtils;
import org.hyperledger.oa.core.RegisteredWebhook;
import org.hyperledger.oa.core.RegisteredWebhook.WebhookEvent;
import org.hyperledger.oa.impl.util.Converter;
import org.hyperledger.oa.model.BPAWebhook;
import org.junit.jupiter.api.Test;

import io.micronaut.data.exceptions.DataAccessException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

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
        repo.save(randomWebhook(List.of(WebhookEvent.CREDENTIAL_UPDATE)));
        repo.save(randomWebhook(List.of(WebhookEvent.CREDENTIAL_UPDATE)));
        repo.save(randomWebhook(List.of(WebhookEvent.DOCUMENT_UPDATE)));
        repo.save(randomWebhook(List.of()));

        final List<BPAWebhook> two = repo.findByEventType(WebhookEvent.CREDENTIAL_UPDATE);
        assertEquals(2, two.size());

        final List<BPAWebhook> one = repo.findByEventType(WebhookEvent.DOCUMENT_UPDATE);
        assertEquals(1, one.size());
    }

    private BPAWebhook randomWebhook(List<WebhookEvent> types) {
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
