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
import org.hyperledger.bpa.controller.api.partner.UpdatePartnerRequest;
import org.hyperledger.bpa.model.Partner;
import org.hyperledger.bpa.model.Tag;
import org.hyperledger.bpa.repository.PartnerRepository;
import org.hyperledger.bpa.repository.TagRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

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
                .name("tag1")
                .build());
        Tag t2 = tagRepo.save(Tag
                .builder()
                .name("tag2")
                .build());
        Tag t3 = tagRepo.save(Tag
                .builder()
                .name("tag3")
                .build());
        Tag t4 = Tag
                .builder()
                .name("tag4")
                .build();

        Partner partner = partnerRepo.save(buildPartnerWithoutTag().build());

        partnerManager.updatePartner(partner.getId(), UpdatePartnerRequest.builder().tag(List.of(t1)).build());
        Assertions.assertEquals(3, tagRepo.count());
        Assertions.assertEquals(1, partnerRepo.count());
        checkTagOnPartner(partner.getId(), "tag1");

        partnerManager.updatePartner(partner.getId(), UpdatePartnerRequest.builder().tag(List.of(t1, t2)).build());
        Assertions.assertEquals(3, tagRepo.count());
        checkTagOnPartner(partner.getId(), "tag1", "tag2");

        partnerManager.updatePartner(partner.getId(), UpdatePartnerRequest.builder().tag(List.of(t1, t3)).build());
        Assertions.assertEquals(3, tagRepo.count());
        checkTagOnPartner(partner.getId(), "tag1", "tag3");

        partnerManager.updatePartner(partner.getId(), UpdatePartnerRequest.builder().tag(List.of(t2, t3)).build());
        Assertions.assertEquals(3, tagRepo.count());
        checkTagOnPartner(partner.getId(), "tag2", "tag3");

        partnerManager.updatePartner(partner.getId(), UpdatePartnerRequest.builder().tag(List.of(t1)).build());
        Assertions.assertEquals(3, tagRepo.count());
        checkTagOnPartner(partner.getId(), "tag1");

        partnerManager.updatePartner(partner.getId(), UpdatePartnerRequest.builder().tag(List.of(t4)).build());
        Assertions.assertEquals(4, tagRepo.count());
        checkTagOnPartner(partner.getId(), "tag4");

        Optional<Tag> dbTag2 = tagRepo.findById(t2.getId());
        Assertions.assertTrue(dbTag2.isPresent());
        Assertions.assertEquals(0, dbTag2.get().getPartners().size());

        partnerManager.updatePartner(partner.getId(), UpdatePartnerRequest.builder().build());
        Assertions.assertEquals(4, tagRepo.count());
        Optional<Tag> dbTag1 = tagRepo.findById(t1.getId());
        Assertions.assertTrue(dbTag1.isPresent());
        Assertions.assertEquals(0, dbTag1.get().getPartners().size());
    }

    private void checkTagOnPartner(UUID partnerId, String... tagName) {
        Optional<Partner> dbP = partnerRepo.findById(partnerId);
        Assertions.assertTrue(dbP.isPresent());
        Assertions.assertNotNull(dbP.get().getTags());
        List<String> pTags = dbP.get().getTags().stream().map(Tag::getName).collect(Collectors.toList());
        Assertions.assertEquals(tagName.length, pTags.size());
        Arrays.stream(tagName).forEach(tn -> Assertions.assertTrue(pTags.contains(tn)));
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
