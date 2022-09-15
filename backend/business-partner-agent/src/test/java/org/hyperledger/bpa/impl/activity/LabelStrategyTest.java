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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import org.hyperledger.aries.api.credentials.Credential;
import org.hyperledger.aries.api.credentials.CredentialAttributes;
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecordByFormat;
import org.hyperledger.aries.api.jsonld.VerifiableCredential;
import org.hyperledger.bpa.api.MyDocumentAPI;
import org.hyperledger.bpa.api.aries.AriesCredential;
import org.hyperledger.bpa.impl.aries.schema.SchemaService;
import org.hyperledger.bpa.persistence.model.BPASchema;
import org.hyperledger.bpa.persistence.model.converter.ExchangePayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    private static final List<CredentialAttributes> BA = new ArrayList<>(Arrays.asList(
            new CredentialAttributes("bic", "123", null),
            new CredentialAttributes("iban", "test123", null)));

    @Test
    void testHappyLabel() throws Exception {
        when(schemaService.getSchemaFor(anyString())).thenReturn(Optional.of(BPASchema
                .builder()
                .defaultAttributeName("iban")
                .build()));

        MyDocumentAPI doc = MyDocumentAPI
                .builder()
                .documentData(BA)
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
                .documentData(BA)
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
        credential.setAttrs(
                new ArrayList<>(Arrays.asList(
                        new CredentialAttributes("iban", "test123", null),
                        new CredentialAttributes("bic", "1234", null))));
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
        credential.setCredentialData(
                new ArrayList<>(Arrays.asList(
                        new CredentialAttributes("iban", "test123", null),
                        new CredentialAttributes("bic", "1234", null))));
        String label = labelStrategy.apply("", credential);
        assertEquals("test123", label);
    }

    @Test
    void testFindLabelForLDProof() {
        String schemaId = "https://foo.com/person";
        when(schemaService.getSchemaFor(anyString())).thenReturn(Optional.of(BPASchema
                .builder()
                .defaultAttributeName("name")
                .schemaId(schemaId)
                .build()));
        JsonObject jo = new JsonObject();
        jo.addProperty("name", "myTest");
        jo.addProperty("email", "test@bar.com");
        VerifiableCredential vc = VerifiableCredential
                .builder()
                .context(List.of(schemaId))
                .type(List.of("person"))
                .credentialSubject(jo)
                .build();
        String label = labelStrategy.apply(ExchangePayload
                .jsonLD(V20CredExRecordByFormat.LdProof.builder()
                        .credential(vc)
                        .build()));
        assertEquals("myTest", label);
    }
}
