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
package org.hyperledger.bpa.impl.aries.prooftemplate;

import io.micronaut.context.BeanContext;
import io.micronaut.data.model.Pageable;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.hyperledger.aries.api.ExchangeVersion;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.api.exception.EntityNotFoundException;
import org.hyperledger.bpa.controller.api.prooftemplates.ProofTemplate;
import org.hyperledger.bpa.impl.aries.proof.ProofManager;
import org.hyperledger.bpa.impl.aries.prooftemplates.ProofTemplateManager;
import org.hyperledger.bpa.impl.aries.schema.SchemaService;
import org.hyperledger.bpa.persistence.model.BPAProofTemplate;
import org.hyperledger.bpa.persistence.model.prooftemplate.BPAAttribute;
import org.hyperledger.bpa.persistence.model.prooftemplate.BPAAttributeGroup;
import org.hyperledger.bpa.persistence.model.prooftemplate.BPAAttributeGroups;
import org.hyperledger.bpa.persistence.repository.BPAProofTemplateRepository;
import org.hyperledger.bpa.testutil.SchemaMockFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@MicronautTest
class ProofTemplateManagerTest {
    @Inject
    BeanContext beanContext;

    @MockBean(ProofManager.class)
    ProofManager proofManager() {
        return Mockito.mock(ProofManager.class);
    }

    @MockBean(SchemaService.class)
    SchemaService schemaService() {
        return Mockito.mock(SchemaService.class);
    }

    @Inject
    ProofManager proofManager;

    @Inject
    SchemaMockFactory.SchemaMock schemaMock;

    @Inject
    BPAProofTemplateRepository repo;

    @Inject
    ProofTemplateManager sut;

    @Test
    void testThatProofManagerIsInvokeWithPartnerIdAndProofTemplate() {
        UUID schemaId = schemaMock.prepareSchemaWithAttributes("mySchemaId", "something");

        UUID partnerId = UUID.randomUUID();
        BPAProofTemplate template = repo.save(
                BPAProofTemplate.builder()
                        .name("myTemplate")
                        .type(CredentialType.INDY)
                        .attributeGroups(
                                BPAAttributeGroups.builder()
                                        .attributeGroup(BPAAttributeGroup.builder()
                                                .schemaId(schemaId)
                                                .attribute(BPAAttribute.builder()
                                                        .name("something")
                                                        .build())
                                                .build())
                                        .build())
                        .build());
        // reset created at with value from db., because Java's value is more detailed
        // that the database's.
        repo.findById(template.getId()).map(BPAProofTemplate::getCreatedAt).ifPresent(template::setCreatedAt);
        doNothing().when(proofManager).sendPresentProofRequestIndy(eq(partnerId), eq(template), eq(ExchangeVersion.V1));

        sut.invokeProofRequestByTemplate(template.getId(), partnerId);

        verify(proofManager, times(1)).sendPresentProofRequestIndy(partnerId, template, ExchangeVersion.V1);
    }

    @Test
    void testThatProofManagerIsNotInvokedIfProofTemplateDoesNotExist() {
        Assertions.assertThrows(
                EntityNotFoundException.class,
                () -> sut.invokeProofRequestByTemplate(UUID.randomUUID(), UUID.randomUUID()),
                "Expected a ProofTemplateException if there is ProofTemplate with the given id.");
        verify(proofManager, never()).sendPresentProofRequestIndy(any(UUID.class), any(BPAProofTemplate.class),
                any(ExchangeVersion.class));
    }

    @Test
    void testAddProofTemplate() {
        UUID schemaId = schemaMock.prepareSchemaWithAttributes("mySchemaId", "something");
        BPAProofTemplate template = BPAProofTemplate.builder()
                .name("myTemplate")
                .type(CredentialType.INDY)
                .attributeGroups(
                        BPAAttributeGroups.builder()
                                .attributeGroup(BPAAttributeGroup.builder()
                                        .schemaId(schemaId)
                                        .attribute(BPAAttribute.builder()
                                                .name("something")
                                                .build())
                                        .build())
                                .build())
                .build();

        assertEquals(0, repo.count(), "There should be no templates initially.");
        BPAProofTemplate expected = sut.addProofTemplate(template);
        assertEquals(1, repo.count(), "There should be one template.");
        Optional<BPAProofTemplate> actual = repo.findById(expected.getId());
        assertTrue(actual.isPresent());
        assertTrue(actual.map(BPAProofTemplate::getCreatedAt).isPresent());
        // equalize the time stamp, because Java's value is more detailed that the
        // database's.
        actual.ifPresent(t -> t.setCreatedAt(expected.getCreatedAt()));
        assertEquals(expected, actual.get(), "The passed proof template should be persisted with an id.");
    }

    @Test
    void testThatAddProofTemplateRejectInvalidProofTemplates() {
        BPAProofTemplate template = BPAProofTemplate.builder().build();

        Assertions.assertThrows(
                ConstraintViolationException.class,
                () -> sut.addProofTemplate(template),
                "ProofTemplateManager#addProofTemplate should reject invalid templates with a ConstraintViolationException");
        assertEquals(0, repo.count(), "There should be no templates persisted.");
    }

    @Test
    void listProofTemplates() {
        UUID schemaId = schemaMock.prepareSchemaWithAttributes("mySchemaId", "something");
        BPAProofTemplate.BPAProofTemplateBuilder templateBuilder = BPAProofTemplate.builder()
                .name("myFirstTemplate")
                .type(CredentialType.INDY)
                .attributeGroups(
                        BPAAttributeGroups.builder()
                                .attributeGroup(BPAAttributeGroup.builder()
                                        .schemaId(schemaId)
                                        .attribute(BPAAttribute.builder()
                                                .name("something")
                                                .build())
                                        .build())
                                .build());
        repo.save(templateBuilder
                .name("myFirstTemplate")
                .build());
        repo.save(templateBuilder
                .name("mySecondTemplate")
                .build());

        List<String> allTemplates = sut.listProofTemplates("", Pageable.unpaged())
                .map(BPAProofTemplate::toRepresentation)
                .map(ProofTemplate::getName)
                .getContent();
        assertEquals(2, allTemplates.size(), "Expected exactly 2 persisted proof templates.");
        assertTrue(allTemplates.contains("myFirstTemplate"), "Expected myFirstTemplate in the listed proof templates");
        assertTrue(allTemplates.contains("mySecondTemplate"),
                "Expected mySecondTemplate in the listed proof templates");
    }

    @Test
    void removeProofTemplate() {
        UUID schemaId = schemaMock.prepareSchemaWithAttributes("mySchemaId", "something");
        UUID templateId = repo.save(
                BPAProofTemplate.builder()
                        .name("myFirstTemplate")
                        .type(CredentialType.INDY)
                        .attributeGroups(
                                BPAAttributeGroups.builder()
                                        .attributeGroup(BPAAttributeGroup.builder()
                                                .schemaId(schemaId)
                                                .attribute(BPAAttribute.builder()
                                                        .name("something")
                                                        .build())
                                                .build())
                                        .build())
                        .build())
                .getId();

        assertTrue(repo.findById(templateId).isPresent(), "The to-be-removed proof template should exist.");
        sut.removeProofTemplate(templateId);
        assertTrue(repo.findById(templateId).isEmpty(), "The proof template was not removed.");
    }
}