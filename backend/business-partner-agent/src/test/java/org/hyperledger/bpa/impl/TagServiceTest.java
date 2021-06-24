package org.hyperledger.bpa.impl;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.hyperledger.bpa.api.TagAPI;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.model.Partner;
import org.hyperledger.bpa.model.Tag;
import org.hyperledger.bpa.repository.PartnerRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        partnerRepo.save(Partner
                        .builder()
                        .ariesSupport(Boolean.TRUE)
                        .did("did:indy:private")
                        .connectionId("con1")
                        .tags(Set.of(Tag.builder().id(t1.getId()).name(t1.getName()).build()))
                        .build());

        tagService.deleteTag(t2.getId(), null);
        Assertions.assertThrows(WrongApiUsageException.class, () -> tagService.deleteTag(t1.getId(), Boolean.FALSE));
        tagService.deleteTag(t1.getId(), Boolean.TRUE);
        Assertions.assertEquals(0, partnerRepo.count());
    }
}
