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
package org.hyperledger.bpa.impl.activity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.api.MyDocumentAPI;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.impl.aries.config.SchemaService;
import org.hyperledger.bpa.model.BPASchema;
import org.hyperledger.bpa.model.MyDocument;
import org.hyperledger.bpa.repository.MyDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DocumentValidatorTest {

    private final ObjectMapper m = new ObjectMapper();

    @Mock
    private MyDocumentRepository docRepo;

    @Mock
    private SchemaService schemaService;

    @InjectMocks
    private DocumentValidator validator;

    @BeforeEach
    void setup() {
        validator.setSchemaService(schemaService);
    }

    @Test
    void testOnlyOneOrgProfile() {
        when(docRepo.findAll()).thenReturn(List.of(new MyDocument()
                .setType(CredentialType.ORGANIZATIONAL_PROFILE_CREDENTIAL)));

        assertThrows(WrongApiUsageException.class, () -> validator.validateNew(MyDocumentAPI.builder()
                .type(CredentialType.ORGANIZATIONAL_PROFILE_CREDENTIAL).build()));
    }

    @Test
    void testIndyCredentialWthNoSchemaId() {
        assertThrows(WrongApiUsageException.class, () -> validator.validateNew(MyDocumentAPI.builder()
                .type(CredentialType.INDY).build()));
    }

    @Test
    void testIndyCredentialWithSchema() throws JsonProcessingException {
        when(schemaService.getSchemaFor(anyString())).thenReturn(buildSchema(Set.of("iban", "bic")));

        String json = "{\"iban\": \"iban\", \"bic\": \"bic\"}";
        JsonNode jsonNode = m.readTree(json);

        validator.validateNew(buildMyDocument(jsonNode));
    }

    @Test
    void testIndyCredentialWithNoMatchingSchema() throws JsonProcessingException {
        when(schemaService.getSchemaFor(anyString())).thenReturn(buildSchema(Set.of("iban", "bic")));

        String json = "{\"foo\": \"iban\", \"bar\": \"bic\"}";
        JsonNode jsonNode = m.readTree(json);

        assertThrows(WrongApiUsageException.class, () -> validator.validateNew(buildMyDocument(jsonNode)));
    }

    private Optional<BPASchema> buildSchema(Set<String> attr) {
        return Optional.of(BPASchema
                .builder()
                .schemaId("123")
                .schemaAttributeNames(attr)
                .build());
    }

    private MyDocumentAPI buildMyDocument(JsonNode jsonNode) {
        return MyDocumentAPI
                .builder()
                .type(CredentialType.INDY)
                .schemaId("123")
                .documentData(jsonNode)
                .build();
    }
}
