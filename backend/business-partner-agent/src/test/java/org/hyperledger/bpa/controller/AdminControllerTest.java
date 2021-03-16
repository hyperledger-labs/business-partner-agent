/*
  Copyright (c) 2020 - for information on the respective copyright owner
  see the NOTICE file and/or the repository at
  https://github.com/hyperledger-labs/business-partner-agent

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package org.hyperledger.bpa.controller;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.ledger.DidVerkeyResponse;
import org.hyperledger.aries.api.schema.SchemaSendResponse;
import org.hyperledger.bpa.api.aries.SchemaAPI;
import org.hyperledger.bpa.controller.api.admin.AddTrustedIssuerRequest;
import org.hyperledger.bpa.controller.api.admin.AddSchemaRequest;
import org.hyperledger.bpa.controller.api.admin.TrustedIssuer;
import org.hyperledger.bpa.controller.api.admin.UpdateTrustedIssuerRequest;
import org.hyperledger.bpa.repository.BPASchemaRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.inject.Inject;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@MicronautTest
public class AdminControllerTest {

    @Value("${bpa.did.prefix}")
    String didPrefix;

    @Inject
    @Client("/api/admin/schema")
    HttpClient client;

    @Inject
    BPASchemaRepository schemaRepo;

    @Inject
    AriesClient ac; // already a mock

    @Test
    void testAddSchemaWithRestriction() throws Exception {
        mockGetSchemaAndVerkey();
        String schemaId = "schema1";

        // add schema
        HttpResponse<SchemaAPI> addedSchema = addSchemaWithRestriction(schemaId);
        Assertions.assertEquals(HttpStatus.OK, addedSchema.getStatus());
        Assertions.assertTrue(addedSchema.getBody().isPresent());

        // check added schema
        SchemaAPI schema = getSchema(addedSchema.getBody().get().getId());
        Assertions.assertEquals(schemaId, schema.getSchemaId());
        Assertions.assertNotNull(schema.getTrustedIssuer());
        Assertions.assertEquals(1, schema.getTrustedIssuer().size());
        Assertions.assertEquals(didPrefix + "issuer1", schema.getTrustedIssuer().get(0).getIssuerDid());

        // add a restriction to the schema
        URI uri = UriBuilder.of("/{id}/trustedIssuer")
                .expand(Map.of("id", schema.getId().toString()));
        client.toBlocking()
                .exchange(HttpRequest.POST(uri,
                        AddTrustedIssuerRequest.builder()
                                .issuerDid("issuer2")
                                .label("Demo Bank")
                                .build()),
                        TrustedIssuer.class);

        // check if the restriction was added
        schema = getSchema(addedSchema.getBody().get().getId());
        Assertions.assertNotNull(schema.getTrustedIssuer());
        Assertions.assertEquals(2, schema.getTrustedIssuer().size());
        Assertions.assertEquals(didPrefix + "issuer2", schema.getTrustedIssuer().get(1).getIssuerDid());

        // delete the first restriction
        URI delete = UriBuilder.of("/{id}/trustedIssuer/{trustedIssuerId}")
                .expand(Map.of(
                        "id", schema.getId().toString(),
                        "trustedIssuerId", schema.getTrustedIssuer().get(0).getId().toString()));
        client.toBlocking().exchange(HttpRequest.DELETE(delete.toString()));

        // check if the first restriction was deleted
        schema = getSchema(addedSchema.getBody().get().getId());
        Assertions.assertNotNull(schema.getTrustedIssuer());
        Assertions.assertEquals(1, schema.getTrustedIssuer().size());
        Assertions.assertEquals(didPrefix + "issuer2", schema.getTrustedIssuer().get(0).getIssuerDid());

        // update the remaining restriction
        URI put = UriBuilder.of("/{id}/trustedIssuer/{trustedIssuerId}")
                .expand(Map.of(
                        "id", schema.getId().toString(),
                        "trustedIssuerId", schema.getTrustedIssuer().get(0).getId().toString()));
        client.toBlocking().exchange(HttpRequest.PUT(put, new UpdateTrustedIssuerRequest("Dummy Bank")));

        // check if the label was updated
        schema = getSchema(addedSchema.getBody().get().getId());
        Assertions.assertEquals("Dummy Bank", schema.getTrustedIssuer().get(0).getLabel());

        // delete schema
        UUID deleteId = schema.getId();
        client.toBlocking().exchange(HttpRequest.DELETE("/" + deleteId));
        // check if the schema was deleted
        Assertions.assertThrows(HttpClientResponseException.class, () -> getSchema(deleteId));
    }

    private SchemaAPI getSchema(@NonNull UUID id) {
        return client.toBlocking()
                .retrieve(HttpRequest.GET("/" + id), SchemaAPI.class);
    }

    private HttpResponse<SchemaAPI> addSchemaWithRestriction(@NonNull String schemaId) {
        return client.toBlocking()
                .exchange(HttpRequest.POST("",
                        AddSchemaRequest.builder()
                                .schemaId(schemaId)
                                .defaultAttributeName("name")
                                .label("Demo Bank")
                                .trustedIssuer(List.of(AddTrustedIssuerRequest
                                        .builder()
                                        .issuerDid("issuer1")
                                        .label("Demo Issuer")
                                        .build()))
                                .build()),
                        SchemaAPI.class);
    }

    private void mockGetSchemaAndVerkey() throws IOException {
        Mockito.when(ac.schemasGetById(Mockito.anyString())).thenReturn(Optional.of(SchemaSendResponse.Schema
                .builder()
                .id("schema1")
                .seqNo(1)
                .attrNames(List.of("name"))
                .name("dummy")
                .build()));

        Mockito.when(ac.ledgerDidVerkey(Mockito.anyString()))
                .thenReturn(Optional.of(new DidVerkeyResponse("verkey")));
    }
}
