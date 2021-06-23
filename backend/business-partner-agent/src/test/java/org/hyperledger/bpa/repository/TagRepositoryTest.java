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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
class TagRepositoryTest {

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

        final Optional<Tag> byName = tagRepo.findByName(tagName);
        assertTrue(byName.isPresent());
        assertEquals(saved.getId(), byName.get().getId());
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
        String myTag = "MyTag";
        Tag tag = Tag
                .builder()
                .name(myTag)
                .build();

        Partner partner = partnerRepo.save(
                buildPartnerWithoutTag()
                        .tags(Set.of(tag))
                        .build());

        Optional<Tag> dbTag = tagRepo.findByName(myTag);

        assertEquals(1, tagRepo.count());
        assertTrue(dbTag.isPresent());
        assertEquals(myTag, dbTag.get().getName());
        assertEquals(partner.getId(), dbTag.get().getPartners().stream().iterator().next().getId());
    }

    @Test
    void testAddExistingTagToPartner() {
        String myTag = "MyTag";
        Tag tag = tagRepo.save(Tag
                .builder()
                .name(myTag)
                .build());

        Partner partner = buildPartnerWithoutTag().build();
        partner = partnerRepo.save(partner);

        partner.setTags(Set.of(tag));
        Partner p2 = partnerRepo.save(partner);

        Optional<Partner> dbPartner = partnerRepo.findById(p2.getId());
        assertEquals(1, tagRepo.count());
        assertTrue(dbPartner.isPresent());
        assertFalse(dbPartner.get().getTags().isEmpty());
        assertEquals(myTag, dbPartner.get().getTags().iterator().next().getName());
    }

    @Test
    void testDeleteTagWhenPartnerHasTag() {
        Tag tag = tagRepo.save(Tag
                .builder()
                .name("MyTag")
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

        assertEquals(2, partnerRepo.count());
        partnerRepo.findAll().forEach(p -> assertEquals(Set.of(tag), p.getTags()));

        tagRepo.deleteById(tag.getId());

        assertEquals(0, tagRepo.count());
        assertEquals(2, partnerRepo.count());
        partnerRepo.findAll().forEach(p -> assertEquals(Set.of(), p.getTags()));
    }

    @Test
    void testDeleteTagFromPartner() {

        Tag tag = tagRepo.save(Tag
                .builder()
                .name("MyTag")
                .partners(Set.of())
                .build());

        Set<Tag> tags = Set.of(tag);
        Set<Tag> empty = Set.of();

        Partner partner = partnerRepo.save(
                buildPartnerWithoutTag()
                        .tags(tags)
                        .build());

        Optional<Partner> dbPartner = partnerRepo.findById(partner.getId());
        assertTrue(dbPartner.isPresent());
        assertEquals(tags, dbPartner.get().getTags());

        partnerRepo.update(dbPartner.get().setTags(empty));
        tagRepo.deletePartnerToTagMapping(dbPartner.get().getId(), tag.getId());
        dbPartner = partnerRepo.findById(partner.getId());
        assertTrue(dbPartner.isPresent());
        // Tag should removed from partner, but tag should not be removed from tag repo
        assertEquals(empty, dbPartner.get().getTags());
        Optional<Tag> reloadedTag = tagRepo.findById(tag.getId());
        assertTrue(reloadedTag.isPresent());
        assertEquals(tag.getId(), reloadedTag.get().getId());
        assertEquals(Set.of(), reloadedTag.get().getPartners());
    }

    @Test
    void testDeletePartner() {
        String t = "MyTag";
        Tag tag = tagRepo.save(Tag
                .builder()
                .name(t)
                .build());

        Set<Tag> tags = Set.of(tag);
        Set<Tag> empty = Set.of();

        Partner partner = partnerRepo.save(
                buildPartnerWithoutTag()
                        .tags(tags)
                        .build());

        Optional<Tag> myTag = tagRepo.findByName(t);
        assertTrue(myTag.isPresent());
        assertEquals(1, myTag.get().getPartners().size());

        partnerRepo.deleteById(partner.getId());

        myTag = tagRepo.findByName(t);
        assertTrue(myTag.isPresent());
        assertEquals(0, myTag.get().getPartners().size());
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
