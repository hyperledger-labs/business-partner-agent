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
import org.hyperledger.bpa.repository.BPAProofTemplateRepository;
import org.hyperledger.bpa.util.SchemaMockFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.inject.Inject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@MicronautTest()
class ProofTemplateControllerTest {
    @Inject
    @Client("/api/proof-templates")
    HttpClient client;

    @MockBean(SchemaService.class)
    SchemaService schemaService() {
        return Mockito.mock(SchemaService.class);
    }

    @Inject
    SchemaMockFactory.SchemaMock schemaMock;

    @Inject
    BPAProofTemplateRepository repository;

    @BeforeEach
    public void setup() {
        repository.deleteAll();
    }

    @Test
    void testAddProofTemplateRequestsAreHandledCorrectly() {
        UUID schemaId = schemaMock.prepareSchemaWithAttributes("mySchemaId", "myAttribute");

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
        Assertions.assertTrue(addedTemplate.getBody()
                .flatMap(p -> p.getAttributeGroups().stream().findAny())
                .flatMap(ag -> ag.getAttributes().stream()
                        .map(Attribute::getName)
                        .filter("myAttribute"::equals)
                        .findAny())
                .isPresent());
        Assertions.assertEquals(1, repository.count());
    }

    @Test
    void testThatListProofTemplatesReturnTheCorrectDateFormat() {
        UUID schemaId = schemaMock.prepareSchemaWithAttributes("mySchemaId", "myAttribute");
        Assertions.assertEquals(0, repository.count());
        client.toBlocking().exchange(
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
        Assertions.assertEquals(1, repository.count());

        Consumer<String> assertDateFormat = (dateString) -> {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            try {
                Assertions.assertNotNull(sdf.parse(dateString));

            } catch (ParseException e) {
                Assertions.fail(dateString + " does not match the pattern \"yyyy-MM-dd'T'HH:mm:ss.SSSZ\"");
            }
        };
        HttpResponse<String> addedTemplate = client.toBlocking().exchange(HttpRequest.GET(""), String.class);
        Pattern dataExtractionPattern = Pattern.compile("\"createdAt\"\\s*:\\s*\"([^,]+)\",");
        addedTemplate.getBody()
                .map(dataExtractionPattern::matcher)
                .filter(Matcher::find)
                .map(m -> m.group(1))
                .ifPresent(assertDateFormat);
    }
}