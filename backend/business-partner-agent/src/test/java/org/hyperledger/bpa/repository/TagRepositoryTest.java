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
import org.hyperledger.bpa.model.Partner;
import org.hyperledger.bpa.model.Tag;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.swing.text.html.Option;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
class TagRepositoryTest {

    @Inject
    TagRepository repo;

    @Inject
    PartnerRepository partnerRepo;

    @Test
    void testGetByName() {
        final String tagName = "verifiedOrg";
        Tag tag = Tag
                .builder()
                .name(tagName)
                .build();
        Tag saved = repo.save(tag);

        final Optional<Tag> byName = repo.findByName(tagName);
        assertTrue(byName.isPresent());
        assertEquals(saved.getId(), byName.get().getId());
    }

    @Test
    void testPersistTagWithSameNameTwice() {
        final String tagName = "verifiedOrg";
        Tag tag1 = repo.save(Tag
                .builder()
                .name(tagName)
                .build());
        Tag tag2 = Tag
                .builder()
                .name(tagName)
                .build();

        assertThrows(DataAccessException.class, () -> repo.save(tag2));
    }

    @Test
    void testAddTagToPartner() {

        String myTag = "MyTag";
        Tag tag = Tag
                .builder()
                .name(myTag)
                .build();

        Set<Tag> tags = new HashSet<>() {{
            add(tag);
        }};

        Partner partner = partnerRepo.save(Partner
                .builder()
                .ariesSupport(Boolean.TRUE)
                .did("did:indy:private")
                .connectionId("con1")
                .tags(tags)
                .build());

        Optional<Tag> dbTag = repo.findByName(myTag);

        assertTrue(dbTag.isPresent());
        assertEquals(partner.getId(), dbTag.get().getPartners().stream().iterator().next().getId());

    }

}
