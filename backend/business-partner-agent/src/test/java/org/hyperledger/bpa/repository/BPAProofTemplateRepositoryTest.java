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

import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.hyperledger.bpa.impl.aries.config.SchemaService;
import org.hyperledger.bpa.model.*;
import org.hyperledger.bpa.model.prooftemplate.BPAAttribute;
import org.hyperledger.bpa.model.prooftemplate.BPAAttributeGroup;
import org.hyperledger.bpa.model.prooftemplate.BPAAttributeGroups;
import org.hyperledger.bpa.model.prooftemplate.BPACondition;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.inject.Inject;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
class BPAProofTemplateRepositoryTest {

    @Inject
    BPAProofTemplateRepository repo;
    @Inject
    SchemaService schemaService;

    @MockBean(SchemaService.class)
    SchemaService schemaService() {
        return Mockito.mock(SchemaService.class);
    }

    @Test
    void test() {
        Mockito.when(schemaService.getSchemaFor("mySchemaId"))
                .thenReturn(Optional.of(new BPASchema()));
        Mockito.when(schemaService.getSchemaAttributeNames("mySchemaId"))
                .thenReturn(Set.of("myAttribute"));
        BPAProofTemplate.BPAProofTemplateBuilder proofTemplateBuilder = BPAProofTemplate.builder()
                .name("myProofTemplate")
                .attributeGroups(
                        BPAAttributeGroups.builder()
                                .attributeGroup(
                                        BPAAttributeGroup.builder()
                                                .schemaId("mySchemaId")
                                                .attribute(
                                                        BPAAttribute.builder()
                                                                .name("myAttribute")
                                                                .condition(
                                                                        BPACondition.builder()
                                                                                .value("myValue")
                                                                                .operator("<=")
                                                                                .build())
                                                                .build())
                                                .build())
                                .build());
        BPAProofTemplate proofTemplateToSave = proofTemplateBuilder
                .build();

        UUID newEntityId = repo.save(proofTemplateToSave).getId();
        BPAProofTemplate expectedProofTemplate = proofTemplateBuilder
                .id(newEntityId)
                .build();

        Optional<BPAProofTemplate> savedProofTemplate = repo.findById(newEntityId);
        assertTrue(savedProofTemplate.isPresent());
        assertEquals(expectedProofTemplate, savedProofTemplate.get());
    }
}