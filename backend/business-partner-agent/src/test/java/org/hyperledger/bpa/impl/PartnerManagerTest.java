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
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.controller.api.partner.UpdatePartnerRequest;
import org.hyperledger.bpa.persistence.model.BPACredentialExchange;
import org.hyperledger.bpa.persistence.model.BPASchema;
import org.hyperledger.bpa.persistence.model.Partner;
import org.hyperledger.bpa.persistence.model.Tag;
import org.hyperledger.bpa.persistence.repository.BPASchemaRepository;
import org.hyperledger.bpa.persistence.repository.HolderCredExRepository;
import org.hyperledger.bpa.persistence.repository.PartnerRepository;
import org.hyperledger.bpa.persistence.repository.TagRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;

@MicronautTest
public class PartnerManagerTest {

    @Inject
    PartnerManager partnerManager;

    @Inject
    TagRepository tagRepo;

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    HolderCredExRepository holderRepo;

    @Inject
    BPASchemaRepository schemaRepo;

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

    @Test
    void testRemovePartner() {
        Partner p = partnerRepo.save(buildPartnerWithoutTag().build());
        BPASchema schema = schemaRepo.save(BPASchema.builder()
                .schemaId("mySchema")
                .schemaAttributeName("test")
                .type(CredentialType.INDY)
                .build());
        BPACredentialExchange asIssuerSuccess = holderRepo.save(BPACredentialExchange
                .builder()
                .partner(p)
                .schema(schema)
                .credentialExchangeId("1")
                .threadId("1")
                .state(CredentialExchangeState.CREDENTIAL_ACKED)
                .build());
        BPACredentialExchange asHolderSuccess = holderRepo.save(BPACredentialExchange
                .builder()
                .partner(p)
                .schema(schema)
                .credentialExchangeId("2")
                .threadId("2")
                .role(CredentialExchangeRole.HOLDER)
                .state(CredentialExchangeState.CREDENTIAL_ACKED)
                .build());
        BPACredentialExchange asHolderFailure = holderRepo.save(BPACredentialExchange
                .builder()
                .partner(p)
                .schema(schema)
                .credentialExchangeId("3")
                .threadId("3")
                .role(CredentialExchangeRole.HOLDER)
                .state(CredentialExchangeState.ABANDONED)
                .build());

        partnerManager.removePartnerById(p.getId());

        Assertions.assertTrue(partnerRepo.findById(p.getId()).isEmpty());
        Assertions.assertTrue(holderRepo.findById(asIssuerSuccess.getId()).isEmpty());
        Assertions.assertTrue(holderRepo.findById(asHolderFailure.getId()).isEmpty());
        Assertions.assertNull(holderRepo.findById(asHolderSuccess.getId()).orElseThrow().getPartner());
        Assertions.assertEquals(1, holderRepo.count());
    }

    private void checkTagOnPartner(UUID partnerId, String... tagName) {
        Optional<Partner> dbP = partnerRepo.findById(partnerId);
        Assertions.assertTrue(dbP.isPresent());
        Assertions.assertNotNull(dbP.get().getTags());
        List<String> pTags = dbP.get().getTags().stream().map(Tag::getName).toList();
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
