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
package org.hyperledger.bpa.testutil;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.api.aries.SchemaAPI;
import org.hyperledger.bpa.impl.aries.schema.SchemaService;
import org.hyperledger.bpa.persistence.model.BPASchema;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Factory
public class SchemaMockFactory {

    @Bean
    public SchemaMock prepareSchemaWithAttributes(SchemaService schemaService) {
        return (ledgerSchemaId, attributes) -> {
            UUID schemaId = UUID.randomUUID();
            if (attributes.length > 0) {
                Mockito.when(schemaService.getSchema(schemaId))
                        .thenReturn(Optional.of(SchemaAPI.builder()
                                .schemaId(ledgerSchemaId).id(schemaId).type(CredentialType.INDY).build()));
                Mockito.when(schemaService.getSchemaFor(ledgerSchemaId))
                        .thenReturn(Optional.of(BPASchema.builder().schemaId(ledgerSchemaId).id(schemaId).build()));
                Mockito.when(schemaService.getSchemaAttributeNames(ledgerSchemaId))
                        .thenReturn(Set.of(attributes));
            } else {
                Mockito.when(schemaService.getSchema(schemaId))
                        .thenReturn(Optional.empty());
                Mockito.when(schemaService.getSchemaFor(ledgerSchemaId))
                        .thenReturn(Optional.empty());
                Mockito.when(schemaService.getSchemaAttributeNames(ledgerSchemaId))
                        .thenReturn(Set.of());
            }
            Mockito.when(schemaService.distinctSchemaType(Mockito.anyList())).thenReturn(true);
            return schemaId;
        };
    }

    public interface SchemaMock {
        /**
         * Mocks {@link SchemaService#getSchema(UUID)} ,
         * {@link SchemaService#getSchemaFor(String)} and
         * {@link SchemaService#getSchemaAttributeNames(String)}
         *
         * @param ledgerSchemaId the ledger schemaId of the schema to be mocked.
         * @param attributes     if no attribute names are given, the schema is mocked
         *                       as not existent.
         * @return the database schemaId of the mocked schema.
         */
        UUID prepareSchemaWithAttributes(String ledgerSchemaId, String... attributes);
    }
}
