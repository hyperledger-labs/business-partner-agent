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
package org.hyperledger.bpa.persistence.repository;

import io.micronaut.data.exceptions.DataAccessException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.hyperledger.bpa.persistence.model.Partner;
import org.hyperledger.bpa.persistence.model.Tag;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
class TagRepositoryTest {

    private static final String MY_TAG = "MyTag";

    @Inject
    TagRepository tagRepo;

    @Inject
    PartnerRepository partnerRepo;

    @Test
    void testGetByName() {
        final String tagName = "verifiedOrg";
        Tag tag = Tag
                .builder()
                .name(tagName)
                .build();
        Tag saved = tagRepo.save(tag);

        Tag byName = tagRepo.findByName(tagName).orElseThrow();
        assertEquals(saved.getId(), byName.getId());
    }

    @Test
    void testPersistTagWithSameNameTwice() {
        final String tagName = "verifiedOrg";
        Tag tag = Tag
                .builder()
                .name(tagName)
                .build();
        tagRepo.save(tag);
        assertThrows(DataAccessException.class, () -> tagRepo.save(tag));
    }

    @Test
    void testAddNoneExistingTagToPartner() {
        Tag tag = Tag
                .builder()
                .name(MY_TAG)
                .build();

        Partner partner = partnerRepo.save(
                buildPartnerWithoutTag()
                        .tags(Set.of(tag))
                        .build());

        Tag dbTag = tagRepo.findByName(MY_TAG).orElseThrow();

        assertEquals(MY_TAG, dbTag.getName());
        assertEquals(partner.getId(), dbTag.getPartners().stream().iterator().next().getId());
    }

    @Test
    void testAddExistingTagToPartner() {
        Tag tag = tagRepo.save(Tag
                .builder()
                .name(MY_TAG)
                .build());

        Partner partner = buildPartnerWithoutTag().build();
        partnerRepo.save(partner);

        Partner dbP = partnerRepo.findById(partner.getId()).orElseThrow();
        tagRepo.createPartnerToTagMapping(dbP.getId(), tag.getId());

        dbP = partnerRepo.findById(partner.getId()).orElseThrow();
        assertEquals(1, tagRepo.count());
        assertEquals(1, partnerRepo.count());
        assertFalse(dbP.getTags().isEmpty());
        assertEquals(MY_TAG, dbP.getTags().iterator().next().getName());
    }

    @Test
    void testDeleteTagWhenPartnerHasTag() {
        Tag tag = tagRepo.save(Tag
                .builder()
                .name(MY_TAG)
                .partners(Set.of())
                .build());

        partnerRepo.save(
                buildPartnerWithoutTag()
                        .tags(Set.of(tag))
                        .build());

        partnerRepo.save(
                buildPartnerWithoutTag()
                        .tags(Set.of(tag))
                        .build());

        assertEquals(2, tagRepo.countReferencesToPartner(tag.getId()));

        assertEquals(2, partnerRepo.count());
        partnerRepo.findAll().forEach(p -> assertEquals(Set.of(tag), p.getTags()));

        tagRepo.deleteByTagId(tag.getId());

        assertEquals(0, tagRepo.count());
        assertEquals(0, tagRepo.countReferencesToPartner(tag.getId()));
        assertEquals(2, partnerRepo.count());
        partnerRepo.findAll().forEach(p -> assertEquals(Set.of(), p.getTags()));
    }

    @Test
    void testDeleteTagFromPartner() {

        Tag tag = tagRepo.save(Tag
                .builder()
                .name(MY_TAG)
                .partners(Set.of())
                .build());

        Set<Tag> tags = Set.of(tag);
        Set<Tag> empty = Set.of();

        Partner partner = partnerRepo.save(
                buildPartnerWithoutTag()
                        .tags(tags)
                        .build());

        Partner dbPartner = partnerRepo.findById(partner.getId()).orElseThrow();
        assertEquals(tags, dbPartner.getTags());

        partnerRepo.update(dbPartner.setTags(empty));
        tagRepo.deletePartnerToTagMapping(dbPartner.getId(), tag.getId());
        dbPartner = partnerRepo.findById(partner.getId()).orElseThrow();
        // Tag should be removed from partner, but tag should not be removed from tag
        // repo
        assertEquals(empty, dbPartner.getTags());
        Optional<Tag> reloadedTag = tagRepo.findById(tag.getId());
        assertTrue(reloadedTag.isPresent());
        assertEquals(tag.getId(), reloadedTag.get().getId());
        assertEquals(Set.of(), reloadedTag.get().getPartners());
    }

    @Test
    void testDeletePartner() {
        Tag tag = tagRepo.save(Tag
                .builder()
                .name(MY_TAG)
                .build());

        Set<Tag> tags = Set.of(tag);

        Partner partner = partnerRepo.save(
                buildPartnerWithoutTag()
                        .tags(tags)
                        .build());

        Tag myTag = tagRepo.findByName(MY_TAG).orElseThrow();
        assertEquals(1, myTag.getPartners().size());

        partnerRepo.deleteByPartnerId(partner.getId());

        myTag = tagRepo.findByName(MY_TAG).orElseThrow();
        assertEquals(0, myTag.getPartners().size());
    }

    @Test
    void testUpdateName() {
        Tag t1 = tagRepo.save(Tag
                .builder()
                .name(MY_TAG)
                .isReadOnly(Boolean.TRUE)
                .build());
        tagRepo.updateNameById(t1.getId(), "foo");
        Tag dbTag = tagRepo.findById(t1.getId()).orElseThrow();
        assertEquals("foo", dbTag.getName());
    }

    @Test
    void testCountByName() {
        tagRepo.save(Tag
                .builder()
                .name(MY_TAG)
                .build());
        assertEquals(1, tagRepo.contByName(MY_TAG));
    }

    private Partner.PartnerBuilder buildPartnerWithoutTag() {
        return Partner
                .builder()
                .ariesSupport(Boolean.TRUE)
                .did("did:indy:private")
                .connectionId("con1")
                .tags(new HashSet<>(List.of()));
    }
}
