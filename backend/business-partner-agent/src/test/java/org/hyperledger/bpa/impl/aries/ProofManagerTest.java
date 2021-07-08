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

package org.hyperledger.bpa.impl.aries;

import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.hyperledger.aries.api.present_proof.PresentProofRequest;
import org.hyperledger.bpa.RunWithAries;
import org.hyperledger.bpa.impl.aries.config.SchemaService;
import org.hyperledger.bpa.model.*;
import org.hyperledger.bpa.repository.PartnerRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@MicronautTest
public class ProofManagerTest extends RunWithAries {
    @Inject
    SchemaService schemaService;
    @MockBean(SchemaService.class)
    SchemaService schemaService() {
        return Mockito.mock(SchemaService.class);
    }

    @Inject
    PartnerRepository partnerRepo;

    @MockBean(PartnerRepository.class)
    PartnerRepository partnerRepository() {
        return Mockito.mock(PartnerRepository.class);
    }

    @Inject
    ProofTemplateConverion proofTemplateConverion;


    @Test
    public void testProofManagerCreateValidRequestFromProofTemplate() throws IOException {

        Mockito.when(schemaService.getSchemaFor("mySchemaId"))
                .thenReturn(Optional.of(new BPASchema()));
        Mockito.when(schemaService.getSchemaAttributeNames("mySchemaId"))
                .thenReturn(Set.of("anotherAttributeName"));
        UUID partnerId = UUID.randomUUID();
        Mockito.when(partnerRepo.findById(partnerId))
                .thenReturn(Optional.of(Partner.builder()
                        .connectionId("myConnectionId")
                        .build())
                );
        BPAProofTemplate template = BPAProofTemplate.builder()
                .id(UUID.randomUUID())
                .name("MyTestTemplate")
                .attributeGroups(
                        BPAAttributeGroups.builder()
                                .attributeGroup(BPAAttributeGroup.builder()
                                        .schemaId("notExistingSchema")
                                        .attribute(BPAAttribute.builder()
                                                .name("myAttributeName")
                                                .build())
                                        .build())
                                .attributeGroup(BPAAttributeGroup.builder()
                                        .schemaId("mySchemaId")
                                        .attribute(BPAAttribute.builder()
                                                .name("notASchemaAttribute")
                                                .build())
                                        .build())
                                .build())
                .build();

        PresentProofRequest actual = proofTemplateConverion.proofRequestFrom(partnerId, template);
        ac.presentProofCreateRequest(actual);
    }
}
