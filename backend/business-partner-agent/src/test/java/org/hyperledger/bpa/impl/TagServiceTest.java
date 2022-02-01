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
package org.hyperledger.bpa.impl;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.hyperledger.bpa.api.TagAPI;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.persistence.model.Partner;
import org.hyperledger.bpa.persistence.model.Tag;
import org.hyperledger.bpa.persistence.repository.PartnerRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

@MicronautTest
public class TagServiceTest {

    @Inject
    TagService tagService;

    @Inject
    PartnerRepository partnerRepo;

    @Test
    void testDeleteTag() {
        TagAPI t1 = tagService.addTag("tag1");
        TagAPI t2 = tagService.addTag("tag2");

        Assertions.assertNotNull(t1);
        Assertions.assertNotNull(t2);

        Partner p = partnerRepo.save(Partner
                .builder()
                .ariesSupport(Boolean.TRUE)
                .did("did:indy:private")
                .connectionId("con1")
                .tags(Set.of(Tag.builder().id(t1.getId()).name(t1.getName()).build()))
                .build());

        tagService.deleteTag(t2.getId(), false);
        Assertions.assertThrows(WrongApiUsageException.class, () -> tagService.deleteTag(t1.getId(), false));
        tagService.deleteTag(t1.getId(), true);

        Optional<Partner> dbP = partnerRepo.findById(p.getId());
        Assertions.assertTrue(dbP.isPresent());
        Assertions.assertEquals(Set.of(), dbP.get().getTags());
    }
}
