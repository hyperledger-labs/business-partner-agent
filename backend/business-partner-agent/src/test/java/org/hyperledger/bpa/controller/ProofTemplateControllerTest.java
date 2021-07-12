package org.hyperledger.bpa.controller;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.checkerframework.checker.units.qual.A;
import org.checkerframework.checker.units.qual.Temperature;
import org.hyperledger.bpa.controller.api.prooftemplates.Attribute;
import org.hyperledger.bpa.controller.api.prooftemplates.AttributeGroup;
import org.hyperledger.bpa.controller.api.prooftemplates.ProofTemplate;
import org.hyperledger.bpa.impl.aries.ProofTemplateConversion;
import org.hyperledger.bpa.impl.aries.config.SchemaService;
import org.hyperledger.bpa.model.BPASchema;
import org.hyperledger.bpa.repository.PartnerRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.inject.Inject;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
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


    private void prepareSchemaWithAttributes(String schemaId, String... attributes) {
        Mockito.when(schemaService.getSchemaFor(schemaId))
                .thenReturn(Optional.of(new BPASchema()));
        Mockito.when(schemaService.getSchemaAttributeNames(schemaId))
                .thenReturn(Set.of(attributes));
    }

    @Test
    void testAddProofTemplateRequestsAreHandledCorrectly() {
        prepareSchemaWithAttributes("mySchemaId", "myAttribute");

        HttpResponse<ProofTemplate> addedTemplate = client.toBlocking().exchange(
                HttpRequest.POST("",
                        ProofTemplate.builder()
                                .name("aTemplate")
                                .attributeGroup(
                                        AttributeGroup.builder()
                                                .schemaId("mySchemaId")
                                                .attribute(
                                                        Attribute.builder()
                                                                .name("myAttribute")
                                                                .build())
                                                .build()
                                )
                                .build()
                ), ProofTemplate.class);
        Assertions.assertEquals(HttpStatus.CREATED, addedTemplate.getStatus());
        Assertions.assertTrue(addedTemplate.getBody().isPresent());
        Assertions.assertTrue(addedTemplate.getBody().map(ProofTemplate::getId).isPresent());

    }
}