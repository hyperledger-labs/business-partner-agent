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
package org.hyperledger.bpa.impl.activity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hyperledger.aries.api.credentials.CredentialAttributes;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.api.MyDocumentAPI;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.impl.aries.schema.SchemaService;
import org.hyperledger.bpa.persistence.model.BPASchema;
import org.hyperledger.bpa.persistence.model.MyDocument;
import org.hyperledger.bpa.persistence.repository.MyDocumentRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

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

    @Mock
    BPAMessageSource.DefaultMessageSource msg;

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

        List<CredentialAttributes> document = new ArrayList<>(Arrays.asList(
                new CredentialAttributes("iban", "iban", null),
                new CredentialAttributes("bic", "bic", null)));

        validator.validateNew(buildMyDocument(document));
    }

    @Test
    void testIndyCredentialWithNoMatchingSchema() throws JsonProcessingException {
        when(schemaService.getSchemaFor(anyString())).thenReturn(buildSchema(Set.of("iban", "bic")));

        List<CredentialAttributes> document = new ArrayList<>(Arrays.asList(
                new CredentialAttributes("foo", "iban", null),
                new CredentialAttributes("bar", "bic", null)));

        assertThrows(WrongApiUsageException.class, () -> validator.validateNew(buildMyDocument(document)));
    }

    @Test
    void testSchemaValidationSuccess() {
        validator.validateAttributesAgainstLDSchema(BPASchema.builder()
                .schemaAttributeNames(Set.of("name", "some"))
                .build(), Map.of("name", "me", "id", "did:sov:123"));
    }

    @Test
    void testSchemaValidationFailure() {
        Assertions.assertThrows(WrongApiUsageException.class,
                () -> validator.validateAttributesAgainstLDSchema(BPASchema.builder()
                        .schemaAttributeNames(Set.of("name", "some"))
                        .build(), Map.of("other", "123")));
    }

    private Optional<BPASchema> buildSchema(Set<String> attr) {
        return Optional.of(BPASchema
                .builder()
                .schemaId("123")
                .schemaAttributeNames(attr)
                .build());
    }

    private MyDocumentAPI buildMyDocument(List<CredentialAttributes> attributes) {
        return MyDocumentAPI
                .builder()
                .type(CredentialType.INDY)
                .schemaId("123")
                .documentData(attributes)
                .build();
    }
}
