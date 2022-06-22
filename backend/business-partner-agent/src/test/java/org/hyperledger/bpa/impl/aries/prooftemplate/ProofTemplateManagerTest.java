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

import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.hyperledger.aries.api.ExchangeVersion;
import org.hyperledger.bpa.api.exception.ProofTemplateException;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.impl.aries.proof.ProofManager;
import org.hyperledger.bpa.impl.aries.prooftemplates.ProofTemplateManager;
import org.hyperledger.bpa.impl.aries.schema.SchemaService;
import org.hyperledger.bpa.persistence.model.BPAProofTemplate;
import org.hyperledger.bpa.persistence.model.prooftemplate.BPAAttributeGroups;
import org.hyperledger.bpa.persistence.repository.BPAProofTemplateRepository;
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
    private BPAProofTemplateRepository repo;

    @Inject
    ProofManager proofManager;

    @MockBean(ProofManager.class)
    ProofManager proofManager() {
        return Mockito.mock(ProofManager.class);
    }

    @MockBean(SchemaService.class)
    SchemaService schemaService() {
        return Mockito.mock(SchemaService.class);
    }

    @Inject
    BPAMessageSource.DefaultMessageSource msg;

    @Test
    void testThatProofManagerIsInvokeWithPartnerIdAndProofTemplate() {
        UUID partnerId = UUID.randomUUID();
        BPAProofTemplate template = repo.save(
                BPAProofTemplate.builder()
                        .name("myTemplate")
                        .attributeGroups(
                                BPAAttributeGroups.builder()
                                        .build())
                        .build());
        // reset created at with value from db., because Java's value is more detailed
        // that the database's.
        repo.findById(template.getId()).map(BPAProofTemplate::getCreatedAt).ifPresent(template::setCreatedAt);
        doNothing().when(proofManager).sendPresentProofRequestIndy(eq(partnerId), eq(template), ExchangeVersion.V1);

        ProofTemplateManager sut = new ProofTemplateManager(repo, proofManager, msg);
        sut.invokeProofRequestByTemplate(template.getId(), partnerId);

        verify(proofManager, times(1)).sendPresentProofRequestIndy(partnerId, template, ExchangeVersion.V1);
    }

    @Test
    void testThatProofManagerIsNotInvokedIfProofTemplateDoesNotExist() {
        ProofTemplateManager sut = new ProofTemplateManager(repo, proofManager, msg);

        Assertions.assertThrows(
                ProofTemplateException.class,
                () -> sut.invokeProofRequestByTemplate(UUID.randomUUID(), UUID.randomUUID()),
                "Expected a ProofTemplateException if there is ProofTemplate with the given id.");
        verify(proofManager, never()).sendPresentProofRequestIndy(any(UUID.class), any(BPAProofTemplate.class),
                ExchangeVersion.V1);
    }

    @Test
    void testAddProofTemplate() {
        BPAProofTemplate template = BPAProofTemplate.builder()
                .name("myTemplate")
                .attributeGroups(
                        BPAAttributeGroups.builder()
                                .build())
                .build();

        ProofTemplateManager sut = new ProofTemplateManager(repo, proofManager, msg);

        Assertions.assertEquals(0, repo.count(), "There should be no templates initially.");
        BPAProofTemplate expected = sut.addProofTemplate(template);
        Assertions.assertEquals(1, repo.count(), "There should be one template.");
        Optional<BPAProofTemplate> actual = repo.findById(expected.getId());
        assertTrue(actual.isPresent());
        assertTrue(actual.map(BPAProofTemplate::getCreatedAt).isPresent());
        // equalize the time stamp, because Java's value is more detailed that the
        // database's.
        actual.ifPresent(t -> t.setCreatedAt(expected.getCreatedAt()));
        Assertions.assertEquals(expected, actual.get(), "The passed proof template should be persisted with an id.");
    }

    @Test
    void testThatAddProofTemplateRejectInvalidProofTemplates() {
        BPAProofTemplate template = BPAProofTemplate.builder().build();

        Assertions.assertThrows(
                ConstraintViolationException.class,
                () -> new ProofTemplateManager(repo, proofManager, msg).addProofTemplate(template),
                "ProofTemplateManager#addProofTemplate should reject invalid templates with a ConstraintViolationException");
        Assertions.assertEquals(0, repo.count(), "There should be no templates persisted.");
    }

    @Test
    void listProofTemplates() {
        BPAProofTemplate.BPAProofTemplateBuilder templateBuilder = BPAProofTemplate.builder()
                .name("myFirstTemplate")
                .attributeGroups(
                        BPAAttributeGroups.builder()
                                .build());
        repo.save(templateBuilder
                .name("myFirstTemplate")
                .build());
        repo.save(templateBuilder
                .name("mySecondTemplate")
                .build());

        ProofTemplateManager sut = new ProofTemplateManager(repo, proofManager, msg);

        List<String> allTemplates = sut.listProofTemplates().map(BPAProofTemplate::getName).toList();
        assertEquals(2, allTemplates.size(), "Expected exactly 2 persisted proof templates.");
        assertTrue(allTemplates.contains("myFirstTemplate"), "Expected myFirstTemplate in the listed proof templates");
        assertTrue(allTemplates.contains("mySecondTemplate"),
                "Expected mySecondTemplate in the listed proof templates");
    }

    @Test
    void removeProofTemplate() {
        UUID templateId = repo.save(
                BPAProofTemplate.builder()
                        .name("myTemplate")
                        .attributeGroups(
                                BPAAttributeGroups.builder()
                                        .build())
                        .build())
                .getId();

        ProofTemplateManager sut = new ProofTemplateManager(repo, proofManager, msg);
        assertTrue(repo.findById(templateId).isPresent(), "The to-be-removed proof template should exist.");
        sut.removeProofTemplate(templateId);
        assertTrue(repo.findById(templateId).isEmpty(), "The proof template was not removed.");
    }
}