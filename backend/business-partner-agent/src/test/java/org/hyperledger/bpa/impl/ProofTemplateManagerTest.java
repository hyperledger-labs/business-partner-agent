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

package org.hyperledger.bpa.impl;

import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.hyperledger.bpa.api.exception.ProofTemplateException;
import org.hyperledger.bpa.impl.aries.ProofManager;
import org.hyperledger.bpa.impl.aries.config.SchemaService;
import org.hyperledger.bpa.model.prooftemplate.BPAAttributeGroups;
import org.hyperledger.bpa.model.BPAProofTemplate;
import org.hyperledger.bpa.repository.BPAProofTemplateRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.inject.Inject;
import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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

    @Inject
    SchemaService schemaService;

    @MockBean(SchemaService.class)
    SchemaService schemaService() {
        return Mockito.mock(SchemaService.class);
    }

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
        doNothing().when(proofManager).sendPresentProofRequest(eq(partnerId), eq(template));

        ProofTemplateManager sut = new ProofTemplateManager(repo, proofManager);
        sut.invokeProofRequestByTemplate(template.getId(), partnerId);

        verify(proofManager, times(1)).sendPresentProofRequest(partnerId, template);
    }

    @Test
    void testThatProofManagerIsNotInvokedIfProofTemplateDoesNotExist() {
        ProofTemplateManager sut = new ProofTemplateManager(repo, proofManager);

        Assertions.assertThrows(
                ProofTemplateException.class,
                () -> sut.invokeProofRequestByTemplate(UUID.randomUUID(), UUID.randomUUID()),
                "Expected a ProofTemplateException if there is ProofTemplate with the given id.");
        verify(proofManager, never()).sendPresentProofRequest(any(UUID.class), any(BPAProofTemplate.class));
    }

    @Test
    void testAddProofTemplate() {
        BPAProofTemplate template = BPAProofTemplate.builder()
                .name("myTemplate")
                .attributeGroups(
                        BPAAttributeGroups.builder()
                                .build())
                .build();

        ProofTemplateManager sut = new ProofTemplateManager(repo, proofManager);

        Assertions.assertEquals(0, repo.count(), "There should be no templates initially.");
        BPAProofTemplate storedTemplate = sut.addProofTemplate(template);
        Assertions.assertEquals(1, repo.count(), "There should be one template.");
        Assertions.assertEquals(
                storedTemplate,
                repo.findById(storedTemplate.getId()).orElse(null),
                "The passed proof template should be persisted with an id.");
    }

    @Test
    void testThatAddProofTemplateRejectInvalidProofTemplates() {
        BPAProofTemplate template = BPAProofTemplate.builder().build();

        Assertions.assertThrows(
                ConstraintViolationException.class,
                () -> new ProofTemplateManager(repo, proofManager).addProofTemplate(template),
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
        BPAProofTemplate template1 = repo.save(templateBuilder
                .name("myFirstTemplate")
                .build());
        BPAProofTemplate template2 = repo.save(templateBuilder
                .name("mySecondTemplate")
                .build());

        ProofTemplateManager sut = new ProofTemplateManager(repo, proofManager);

        List<BPAProofTemplate> allTemplates = sut.listProofTemplates().collect(Collectors.toList());
        assertEquals(2, allTemplates.size(), "Expected exactly 2 persisted proof templates.");
        assertTrue(allTemplates.contains(template1), "Expected template1 in the listed proof templates");
        assertTrue(allTemplates.contains(template2), "Expected template2 in the listed proof templates");
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

        ProofTemplateManager sut = new ProofTemplateManager(repo, proofManager);
        assertTrue(repo.findById(templateId).isPresent(), "The to-be-removed proof template should exist.");
        sut.removeProofTemplate(templateId);
        assertTrue(repo.findById(templateId).isEmpty(), "The proof template was not removed.");
    }
}