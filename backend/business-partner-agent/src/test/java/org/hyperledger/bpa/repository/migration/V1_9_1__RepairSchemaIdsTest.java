/*
  Copyright (c) 2020 - for information on the respective copyright owner
  see the NOTICE file and/or the repository at
  https://github.com/hyperledger-labs/organizational-agent

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
package org.hyperledger.bpa.repository.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.api.aries.SchemaAPI;
import org.hyperledger.bpa.impl.aries.config.SchemaService;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.model.MyDocument;
import org.hyperledger.bpa.repository.MyDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class V1_9_1__RepairSchemaIdsTest {

    @Mock
    MyDocumentRepository docRepo;

    @Mock
    SchemaService schemaService;

    @InjectMocks
    V1_9_1__RepairSchemaIds migration;

    @BeforeEach
    public void setup() {
        Converter conv = new Converter();
        conv.setMapper(new ObjectMapper());
        migration.setConv(conv);
    }

    @Test
    void testMigration() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        String s1 = "did:iil:1";
        String s2 = "did:iil:2";

        Mockito.when(docRepo.findAll()).thenReturn(List.of(
                new MyDocument()
                        .setId(id1)
                        .setDocument(Map.of("iban", "123", "bic", "bic"))
                        .setType(CredentialType.SCHEMA_BASED),
                new MyDocument()
                        .setId(id2)
                        .setDocument(Map.of("did", "did:sov:123"))
                        .setType(CredentialType.SCHEMA_BASED)));

        Mockito.when(schemaService.listSchemas()).thenReturn(List.of(
                SchemaAPI
                        .builder()
                        .schemaId(s1)
                        .label("Bank Account")
                        .build(),
                SchemaAPI
                        .builder()
                        .schemaId(s2)
                        .label("Commercial")
                        .build()));

        migration.setSchemaIdsWhereNull();

        Mockito.verify(docRepo, Mockito.times(1)).updateSchemaId(id1, s1);
        Mockito.verify(docRepo, Mockito.times(1)).updateSchemaId(id2, s2);
    }
}
