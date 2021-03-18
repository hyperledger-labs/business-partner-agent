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
package org.hyperledger.bpa.impl.activity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hyperledger.aries.api.credential.Credential;
import org.hyperledger.bpa.api.MyDocumentAPI;
import org.hyperledger.bpa.api.aries.AriesCredential;
import org.hyperledger.bpa.impl.aries.config.SchemaService;
import org.hyperledger.bpa.model.BPASchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LabelStrategyTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private static final String SCHEMA_ID = "testSchemaId";

    @Mock
    SchemaService schemaService;

    @InjectMocks
    LabelStrategy labelStrategy;

    private static final String BA = "{\"bic\": \"123\", \"iban\": \"test123\"}";

    @Test
    void testHappyLabel() throws Exception {
        when(schemaService.getSchemaFor(anyString())).thenReturn(Optional.of(BPASchema
                .builder()
                .defaultAttributeName("iban")
                .build()));

        MyDocumentAPI doc = MyDocumentAPI
                .builder()
                .documentData(mapper.readValue(BA, JsonNode.class))
                .schemaId(SCHEMA_ID)
                .build();
        labelStrategy.apply(doc);
        assertEquals("test123", doc.getLabel());
    }

    @Test
    void testLabelSetByUser() {
        MyDocumentAPI doc = MyDocumentAPI
                .builder()
                .label("Bank Account")
                .build();
        labelStrategy.apply(doc);
        assertEquals("Bank Account", doc.getLabel());
    }

    @Test
    void testNoSchemaConfig() throws Exception {
        MyDocumentAPI doc = MyDocumentAPI
                .builder()
                .schemaId(SCHEMA_ID)
                .documentData(mapper.readValue(BA, JsonNode.class))
                .build();
        labelStrategy.apply(doc);
        assertNull(doc.getLabel());
    }

    @Test
    void testHappyLabelFromCredential() {
        when(schemaService.getSchemaFor(anyString())).thenReturn(Optional.of(BPASchema
                .builder()
                .defaultAttributeName("iban")
                .schemaId(SCHEMA_ID)
                .build()));

        Credential credential = new Credential();
        credential.setAttrs(Map.of("iban", "test123", "bic", "1234"));
        credential.setSchemaId(SCHEMA_ID);
        String label = labelStrategy.apply(credential);
        assertEquals("test123", label);
    }

    @Test
    void testSetLabelOnCredentialUpdate() {
        String myLabel = labelStrategy.apply("My Label", new AriesCredential());
        assertEquals("My Label", myLabel);
    }

    @Test
    void testResetLabelOnCredentialUpdate() {
        when(schemaService.getSchemaFor(anyString())).thenReturn(Optional.of(BPASchema
                .builder()
                .defaultAttributeName("iban")
                .schemaId(SCHEMA_ID)
                .build()));
        AriesCredential credential = new AriesCredential();
        credential.setSchemaId(SCHEMA_ID);
        credential.setCredentialData(Map.of("iban", "test123", "bic", "1234"));
        String label = labelStrategy.apply("", credential);
        assertEquals("test123", label);
    }
}
