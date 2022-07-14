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
package org.hyperledger.bpa.impl.rules;

import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.extern.slf4j.Slf4j;
import jakarta.inject.Inject;
import org.hyperledger.bpa.api.notification.PartnerAddedEvent;
import org.hyperledger.bpa.impl.PartnerManager;
import org.hyperledger.bpa.impl.aries.connection.ConnectionManager;
import org.hyperledger.bpa.persistence.model.Partner;
import org.hyperledger.bpa.persistence.repository.PartnerRepository;
import org.hyperledger.bpa.persistence.model.Tag;
import org.hyperledger.bpa.impl.aries.AriesEventHandler;
import org.hyperledger.bpa.persistence.repository.RulesRepository;
import org.hyperledger.bpa.persistence.repository.TagRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@MicronautTest(transactional = false)
public class PartnerRuleEventHandlerTest {

    @Inject
    PartnerRuleEventHandler partnerRuleEventHandler;

    @Inject
    RulesService rulesService;

    @Inject
    AriesEventHandler ariesEventHandler;

    @Inject
    ConnectionManager manager;

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    RulesRepository rulesRepo;

    @Inject
    TagRepository tagRepo;

    @Inject
    PartnerManager partnerManager;

    @Inject
    ApplicationEventPublisher<PartnerAddedEvent> eventPublisher;

    @Test
    void testTagPartnerOnConnection() throws InterruptedException {

        String connectionId = "de0d51e8-4c7f-4dc9-8b7b-a8f57182d822";
        String did = "did:1";
        String did2 = "did:2";
        String tag = "some-tag";

        RulesData data = rulesService.add(new RulesData.Trigger.EventTrigger(PartnerAddedEvent.class.getSimpleName()),
                new RulesData.Action.TagConnection(connectionId, tag));

        log.debug(data.toString());
        assertEquals(rulesService.getAll().size(), 1);
        log.debug(rulesService.getAll().toString());

        Partner partner = partnerRepo.save(Partner
                .builder()
                .ariesSupport(Boolean.FALSE)
                .did(did)
                .connectionId(connectionId)
                .build());

        Assertions.assertEquals(partnerRepo.count(), 1);

        Optional<Partner> pBefore = partnerRepo.findById(partner.getId());
        assert (pBefore.isPresent());
        Assertions.assertEquals(Set.of(), pBefore.get().getTags());

        eventPublisher.publishEvent(PartnerAddedEvent.builder().partner(pBefore.get()).build());

        Assertions.assertEquals(partnerRepo.count(), 1);
        while (tagRepo.count() < 1) {
            TimeUnit.MILLISECONDS.sleep(100);
        }
        Assertions.assertEquals(tagRepo.count(), 1);
        Optional<Partner> pAfter = partnerRepo.findById(partner.getId());
        assert (pAfter.isPresent());
        Assertions.assertNotEquals(Set.of(), pAfter.get().getTags());
        checkTagOnPartner(pAfter.get().getId(), tag);

        Partner partner2 = partnerRepo.save(Partner
                .builder()
                .ariesSupport(Boolean.FALSE)
                .did(did2)
                .build());
        eventPublisher.publishEvent(PartnerAddedEvent.builder().partner(partner2).build());
        TimeUnit.MILLISECONDS.sleep(100);
        Assertions.assertEquals(tagRepo.count(), 1);
        Optional<Partner> p2After = partnerRepo.findById(partner2.getId());
        assert (p2After.isPresent());
        checkTagOnPartner(p2After.get().getId(), tag);
    }

    private void checkTagOnPartner(UUID partnerId, String... tagName) {
        Optional<Partner> dbP = partnerRepo.findById(partnerId);
        Assertions.assertTrue(dbP.isPresent());
        Assertions.assertNotNull(dbP.get().getTags());
        List<String> pTags = dbP.get().getTags().stream().map(Tag::getName).toList();
        Assertions.assertEquals(tagName.length, pTags.size());
        Arrays.stream(tagName).forEach(tn -> Assertions.assertTrue(pTags.contains(tn)));
    }
}
