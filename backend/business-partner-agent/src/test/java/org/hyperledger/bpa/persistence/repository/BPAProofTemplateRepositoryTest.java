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
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.hyperledger.bpa.impl.aries.schema.SchemaService;
import org.hyperledger.bpa.persistence.model.BPAProofTemplate;
import org.hyperledger.bpa.persistence.model.PartnerProof;
import org.hyperledger.bpa.persistence.model.prooftemplate.*;
import org.hyperledger.bpa.testutil.SchemaMockFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
class BPAProofTemplateRepositoryTest {

    @Inject
    BPAProofTemplateRepository repo;

    @Inject
    PartnerProofRepository proofRepository;

    @MockBean(SchemaService.class)
    SchemaService schemaService() {
        return Mockito.mock(SchemaService.class);
    }

    @Inject
    SchemaMockFactory.SchemaMock schemaMock;

    @BeforeEach
    public void setup() {
        proofRepository.deleteAll();
        repo.deleteAll();
    }

    @Test
    void testSavingAnEntity() {
        BPAProofTemplate.BPAProofTemplateBuilder proofTemplateBuilder = getBpaProofTemplateBuilder();
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
        Instant givenCreatedAt = Instant.now().minus(Duration.ofMillis(1000));
        BPAProofTemplate.BPAProofTemplateBuilder proofTemplateBuilder = getBpaProofTemplateBuilder()
                .createdAt(givenCreatedAt);
        BPAProofTemplate proofTemplateToSave = proofTemplateBuilder
                .build();

        UUID newEntityId = repo.save(proofTemplateToSave).getId();

        BPAProofTemplate savedProofTemplate = repo.findById(newEntityId).orElseThrow();
        assertNotNull(savedProofTemplate.getCreatedAt());
        assertNotEquals(givenCreatedAt, savedProofTemplate.getCreatedAt());
        assertTrue(givenCreatedAt.isBefore(savedProofTemplate.getCreatedAt()));
        BPAProofTemplate expectedProofTemplate = proofTemplateBuilder
                .id(newEntityId)
                // copy database generated time stamp
                .createdAt(savedProofTemplate.getCreatedAt())
                .build();
        assertEquals(expectedProofTemplate, savedProofTemplate);
    }

    @Test
    void testThatCreatedAtIsNotOverwrittenOnUpdatingByDB() throws InterruptedException {
        Instant givenCreatedAt = Instant.now().minus(Duration.ofMillis(1000));
        BPAProofTemplate.BPAProofTemplateBuilder proofTemplateBuilder = getBpaProofTemplateBuilder()
                .createdAt(givenCreatedAt);
        BPAProofTemplate proofTemplateToSave = proofTemplateBuilder
                .build();

        UUID newEntityId = repo.save(proofTemplateToSave).getId();

        Optional<BPAProofTemplate> savedProofTemplate = repo.findById(newEntityId);
        assertTrue(savedProofTemplate.isPresent());
        assertTrue(savedProofTemplate.map(BPAProofTemplate::getCreatedAt).isPresent());
        Instant firstCreatedAt = savedProofTemplate.get().getCreatedAt();
        Thread.sleep(50);
        proofTemplateToSave.setName("update to save");
        // modified timestamp from client
        proofTemplateToSave.setCreatedAt(Instant.now());
        repo.save(proofTemplateToSave);
        savedProofTemplate = repo.findById(newEntityId);
        assertTrue(savedProofTemplate.isPresent());
        assertTrue(savedProofTemplate.map(BPAProofTemplate::getCreatedAt).isPresent());
        Instant secondCreatedAt = savedProofTemplate.get().getCreatedAt();
        assertEquals(firstCreatedAt, secondCreatedAt);
    }

    @Test
    void testThatPartnerProofResolvesItsTemplate() {
        BPAProofTemplate.BPAProofTemplateBuilder proofTemplateBuilder = getBpaProofTemplateBuilder();
        BPAProofTemplate proofTemplateToSave = proofTemplateBuilder
                .build();

        BPAProofTemplate savedTemplate = repo.save(proofTemplateToSave);
        System.out.println(savedTemplate);
        PartnerProof proof = proofRepository.save(PartnerProof.builder()
                .partnerId(UUID.randomUUID())
                .presentationExchangeId("presentationExchangeId")
                .proofTemplate(savedTemplate)
                .build());

        assertTrue(proofRepository.findById(proof.getId()).map(PartnerProof::getProofTemplate).isPresent());
    }

    @Test
    void testThatDeletionIsConstrainedToUnusedTemplates() {
        BPAProofTemplate.BPAProofTemplateBuilder proofTemplateBuilder = getBpaProofTemplateBuilder();
        BPAProofTemplate proofTemplateToSave = proofTemplateBuilder
                .build();

        BPAProofTemplate savedTemplate = repo.save(proofTemplateToSave);
        System.out.println(savedTemplate);
        PartnerProof proof = proofRepository.save(PartnerProof.builder()
                .partnerId(UUID.randomUUID())
                .presentationExchangeId("presentationExchangeId")
                .proofTemplate(savedTemplate)
                .build());

        assertThrows(DataAccessException.class, () -> repo.deleteById(savedTemplate.getId()));
        proofRepository.deleteById(proof.getId());
        assertTrue(proofRepository.findById(proof.getId()).isEmpty());
        assertTrue(repo.findById(proofTemplateToSave.getId()).isPresent());
        repo.deleteById(savedTemplate.getId());
        assertEquals(0, repo.count());
    }

    private BPAProofTemplate.BPAProofTemplateBuilder getBpaProofTemplateBuilder() {
        UUID schemaId = schemaMock.prepareSchemaWithAttributes("mySchemaId", "myAttribute");
        return BPAProofTemplate.builder()
                .name("myProofTemplate")
                .attributeGroups(
                        BPAAttributeGroups.builder()
                                .attributeGroup(
                                        BPAAttributeGroup.builder()
                                                .schemaId(schemaId.toString())
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
    }
}