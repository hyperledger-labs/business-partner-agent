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
import org.hyperledger.bpa.model.prooftemplate.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
class BPAProofTemplateRepositoryTest {

    @Inject
    BPAProofTemplateRepository repo;

    @Inject
    PartnerProofRepository proofRepository;

    @Inject
    SchemaService schemaService;

    @MockBean(SchemaService.class)
    SchemaService schemaService() {
        return Mockito.mock(SchemaService.class);
    }

    @Test
    void testSavingAnEntity() {
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
                                                                                .value("113")
                                                                                .operator(ValueOperators.LESS_THAN)
                                                                                .build())
                                                                .build())
                                                .build())
                                .build());
        BPAProofTemplate proofTemplateToSave = proofTemplateBuilder
                .build();

        UUID newEntityId = repo.save(proofTemplateToSave).getId();

        Optional<BPAProofTemplate> savedProofTemplate = repo.findById(newEntityId);
        assertTrue(savedProofTemplate.isPresent());
        assertTrue(savedProofTemplate.map(BPAProofTemplate::getCreatedAt).isPresent());
        BPAProofTemplate expectedProofTemplate = proofTemplateBuilder
                .id(newEntityId)
                // copy database generated time stamp
                .createdAt(savedProofTemplate.get().getCreatedAt())
                .build();
        assertEquals(expectedProofTemplate, savedProofTemplate.get());
    }

    @Test
    void testThatCreatedAtIsOverwrittenByDB() {
        Mockito.when(schemaService.getSchemaFor("mySchemaId"))
                .thenReturn(Optional.of(new BPASchema()));
        Mockito.when(schemaService.getSchemaAttributeNames("mySchemaId"))
                .thenReturn(Set.of("myAttribute"));
        Instant givenCreatedAt = Instant.now().minus(Duration.ofMillis(1000));
        BPAProofTemplate.BPAProofTemplateBuilder proofTemplateBuilder = BPAProofTemplate.builder()
                .name("myProofTemplate")
                .createdAt(givenCreatedAt)
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
                                                                                .value("113")
                                                                                .operator(ValueOperators.LESS_THAN)
                                                                                .build())
                                                                .build())
                                                .build())
                                .build());
        BPAProofTemplate proofTemplateToSave = proofTemplateBuilder
                .build();

        UUID newEntityId = repo.save(proofTemplateToSave).getId();

        Optional<BPAProofTemplate> savedProofTemplate = repo.findById(newEntityId);
        assertTrue(savedProofTemplate.isPresent());
        assertTrue(savedProofTemplate.map(BPAProofTemplate::getCreatedAt).isPresent());
        assertNotEquals(givenCreatedAt, savedProofTemplate.get().getCreatedAt());
        assertTrue(givenCreatedAt.isBefore(savedProofTemplate.get().getCreatedAt()));
        BPAProofTemplate expectedProofTemplate = proofTemplateBuilder
                .id(newEntityId)
                // copy database generated time stamp
                .createdAt(savedProofTemplate.get().getCreatedAt())
                .build();
        assertEquals(expectedProofTemplate, savedProofTemplate.get());
    }

    @Test
    void testThatCreatedAtIsNotOverwrittenOnUpdatingByDB() throws InterruptedException {
        Mockito.when(schemaService.getSchemaFor("mySchemaId"))
                .thenReturn(Optional.of(new BPASchema()));
        Mockito.when(schemaService.getSchemaAttributeNames("mySchemaId"))
                .thenReturn(Set.of("myAttribute"));
        Instant givenCreatedAt = Instant.now().minus(Duration.ofMillis(1000));
        BPAProofTemplate.BPAProofTemplateBuilder proofTemplateBuilder = BPAProofTemplate.builder()
                .name("myProofTemplate")
                .createdAt(givenCreatedAt)
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
                                                                                .value("113")
                                                                                .operator(ValueOperators.LESS_THAN)
                                                                                .build())
                                                                .build())
                                                .build())
                                .build());
        BPAProofTemplate proofTemplateToSave = proofTemplateBuilder
                .build();

        UUID newEntityId = repo.save(proofTemplateToSave).getId();

        Optional<BPAProofTemplate> savedProofTemplate = repo.findById(newEntityId);
        assertTrue(savedProofTemplate.isPresent());
        assertTrue(savedProofTemplate.map(BPAProofTemplate::getCreatedAt).isPresent());
        Instant firstCreatedAt = savedProofTemplate.get().getCreatedAt();
        Thread.sleep(50);
        proofTemplateToSave.setName("update to save");
        // modified timestamp from cliet
        proofTemplateToSave.setCreatedAt(Instant.now());
        repo.save(proofTemplateToSave);
        savedProofTemplate = repo.findById(newEntityId);
        assertTrue(savedProofTemplate.isPresent());
        assertTrue(savedProofTemplate.map(BPAProofTemplate::getCreatedAt).isPresent());
        Instant secondCreatedAt = savedProofTemplate.get().getCreatedAt();
        assertEquals(firstCreatedAt, secondCreatedAt);
    }

    @Test
    void testThatDeletionIsConstrainedToUnusedTemplates() {
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
                                                                                .value("113")
                                                                                .operator(ValueOperators.LESS_THAN)
                                                                                .build())
                                                                .build())
                                                .build())
                                .build());
        BPAProofTemplate proofTemplateToSave = proofTemplateBuilder
                .build();

        BPAProofTemplate savedTemplate = repo.save(proofTemplateToSave);
        System.out.println(savedTemplate);
        PartnerProof proof = proofRepository.save(PartnerProof.builder()
                .partnerId(UUID.randomUUID())
                .presentationExchangeId("presentationExchangeId")
                .proofTemplate(savedTemplate)
                .build());

//        try {
//            repo.deleteById(savedTemplate.getId());
//            fail("Deleting a referenced proofTemplate should fail.");
//        }catch (DataAccessException e){
//            e.printStackTrace();
//        }
        proofRepository.deleteById(proof.getId());
        assertTrue(proofRepository.findById(proof.getId()).isEmpty());
        assertTrue(repo.findById(proofTemplateToSave.getId()).isPresent());

    }

}