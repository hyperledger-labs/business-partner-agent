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
package org.hyperledger.bpa.controller;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.hyperledger.bpa.controller.api.prooftemplates.Attribute;
import org.hyperledger.bpa.controller.api.prooftemplates.AttributeGroup;
import org.hyperledger.bpa.controller.api.prooftemplates.ProofTemplate;
import org.hyperledger.bpa.impl.aries.config.SchemaService;
import org.hyperledger.bpa.model.BPASchema;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.inject.Inject;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@MicronautTest
class ProofTemplateControllerTest {
    @Inject
    @Client("/api/proof-templates")
    HttpClient client;

    @Inject
    SchemaService schemaService;

    @MockBean(SchemaService.class)
    SchemaService schemaService() {
        return Mockito.mock(SchemaService.class);
    }

    private UUID prepareSchemaWithAttributes(String... attributes) {
        UUID schemaId = UUID.randomUUID();
        Mockito.when(schemaService.getSchemaFor(schemaId.toString()))
                .thenReturn(Optional.of(new BPASchema()));
        Mockito.when(schemaService.getSchemaAttributeNames(schemaId.toString()))
                .thenReturn(Set.of(attributes));
        return schemaId;
    }

    @Test
    void testAddProofTemplateRequestsAreHandledCorrectly() {
        UUID schemaId = prepareSchemaWithAttributes("myAttribute");

        HttpResponse<ProofTemplate> addedTemplate = client.toBlocking().exchange(
                HttpRequest.POST("",
                        ProofTemplate.builder()
                                .name("aTemplate")
                                .attributeGroup(
                                        AttributeGroup.builder()
                                                .schemaId(schemaId.toString())
                                                .attribute(
                                                        Attribute.builder()
                                                                .name("myAttribute")
                                                                .build())
                                                .build())
                                .build()),
                ProofTemplate.class);
        Assertions.assertEquals(HttpStatus.CREATED, addedTemplate.getStatus());
        Assertions.assertTrue(addedTemplate.getBody().isPresent());
        Assertions.assertTrue(addedTemplate.getBody().map(ProofTemplate::getId).isPresent());

    }
}