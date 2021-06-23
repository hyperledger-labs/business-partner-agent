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
package org.hyperledger.bpa.impl;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.hyperledger.bpa.model.Partner;
import org.hyperledger.bpa.model.Tag;
import org.hyperledger.bpa.repository.PartnerRepository;
import org.hyperledger.bpa.repository.TagRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

@MicronautTest
public class PartnerManagerTest {

    @Inject
    PartnerManager partnerManager;

    @Inject
    TagRepository tagRepo;

    @Inject
    PartnerRepository partnerRepo;

    @Test
    void testAddAndRemovePartnerTag() {
        Tag t1 = tagRepo.save(Tag
                .builder()
                .name("MyTag")
                .build());
        Tag t2 = tagRepo.save(Tag
                .builder()
                .name("Other Tag")
                .build());
        Partner partner = partnerRepo.save(buildPartnerWithoutTag().build());

        partnerManager.updatePartnerTag(partner.getId(), List.of(t1));
        Assertions.assertEquals(2, tagRepo.count());

        partnerManager.updatePartnerTag(partner.getId(), List.of(t1, t2));
        Assertions.assertEquals(2, tagRepo.count());

        partnerManager.updatePartnerTag(partner.getId(), List.of(t1));
        Assertions.assertEquals(2, tagRepo.count());
        Optional<Tag> dbTag2 = tagRepo.findById(t2.getId());
        Assertions.assertTrue(dbTag2.isPresent());
        Assertions.assertEquals(0, dbTag2.get().getPartners().size());

        partnerManager.updatePartnerTag(partner.getId(), null);
        Assertions.assertEquals(2, tagRepo.count());
        Optional<Tag> dbTag1 = tagRepo.findById(t1.getId());
        Assertions.assertTrue(dbTag1.isPresent());
        Assertions.assertEquals(0, dbTag1.get().getPartners().size());
    }

    // TODO move to test model builder
    private Partner.PartnerBuilder buildPartnerWithoutTag() {
        return Partner
                .builder()
                .ariesSupport(Boolean.TRUE)
                .did("did:indy:private")
                .connectionId("con1")
                .tags(new HashSet<>(List.of()));
    }
}
