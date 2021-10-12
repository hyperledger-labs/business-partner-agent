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
import com.google.gson.Gson;
import org.hyperledger.aries.api.credentials.Credential;
import org.hyperledger.aries.api.jsonld.VerifiableCredential;
import org.hyperledger.aries.config.GsonConfig;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.impl.aries.config.SchemaService;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.model.BPACredentialExchange;
import org.hyperledger.bpa.model.BPASchema;
import org.hyperledger.bpa.model.MyDocument;
import org.hyperledger.bpa.repository.PartnerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VPManagerTest {

    private final ObjectMapper m = new ObjectMapper();
    private final Converter c = new Converter();
    private final Gson gson = GsonConfig.defaultConfig();

    @Mock
    private SchemaService schemaService;

    @Mock
    private Identity identity;

    @Mock
    private PartnerRepository partnerRepository;

    @InjectMocks
    private final VPManager vpm = new VPManager();

    @BeforeEach
    void setup() {
        c.setMapper(m);
        vpm.setConverter(c);
        vpm.setSchemaService(Optional.of(schemaService));
    }

    @Test
    void testBuildFromDocumentOrg() throws Exception {
        String json = "{\"id\":\"did:sov:sadfafs\",\"type\":\"LegalEntity\"}";
        final Map<String, Object> d = createMap(json);

        MyDocument doc = buildDefault()
                .setType(CredentialType.ORGANIZATIONAL_PROFILE_CREDENTIAL)
                .setDocument(d);
        final VerifiableCredential vp = vpm.buildFromDocument(doc, "xxyyyzzz");

        assertNotNull(vp.getCredentialSubject());
        assertEquals("{type=LegalEntity, id=xxyyyzzz}",
                vp.getCredentialSubject().toString());
        assertEquals(CredentialType.ORGANIZATIONAL_PROFILE_CREDENTIAL.getContext(), vp.getContext());
    }

    @Test
    void testBuildFromDocumentIndyNoSchema() throws Exception {
        String json = "{\"iban\":\"1234\",\"bic\":\"4321\"}";
        final Map<String, Object> d = createMap(json);

        MyDocument doc = buildDefault()
                .setType(CredentialType.INDY)
                .setDocument(d);
        String myDid = "xxyyyzzz";
        final VerifiableCredential vp = vpm.buildFromDocument(doc, myDid);

        assertNotNull(vp.getCredentialSubject());
        assertEquals("{iban=1234, bic=4321, id=xxyyyzzz}",
                vp.getCredentialSubject().toString());
        assertEquals(myDid, vp.getIssuer());
        assertEquals(CredentialType.INDY.getContext(), vp.getContext());
    }

    @Test
    void testBuildFromDocumentIndyWithSchema() throws Exception {
        String json = "{\"key1\":\"1234\",\"key2\":\"4321\"}";
        final Map<String, Object> d = createMap(json);

        Set<String> attributeNames = new LinkedHashSet<>();
        attributeNames.add("key1");
        attributeNames.add("key2");

        when(schemaService.getSchemaFor(anyString())).thenReturn(Optional.of(
                BPASchema.builder()
                        .schemaAttributeNames(attributeNames)
                        .schemaId("1234")
                        .build()));

        when(identity.getDidPrefix()).thenReturn("did:iil:");

        MyDocument doc = buildDefault()
                .setType(CredentialType.INDY)
                .setSchemaId("testSchema")
                .setDocument(d);

        final VerifiableCredential vp = vpm.buildFromDocument(doc, "xxyyyzzz");

        String actual = gson.toJson(vp.getContext());
        String expected = "[\"https://www.w3.org/2018/credentials/v1\",{\"@context\":{\"sc\":\"did:iil:1234\"," +
                "\"key1\":{\"@id\":\"sc:key1\"},\"key2\":{\"@id\":\"sc:key2\"}}}]";

        assertEquals(expected, actual);
    }

    @Test
    void buildFromCredentialCommReg() {
        String ariesCredential = "{\"attrs\":{\"did\":\"did:sov:9iDuvPqcGmpLrn67BMHwwB\"," +
                "\"companyName\":\"ALDI AHEAD GmbH\"},\"referent\":\"3512fa49-1bce-42d6-b73f-79742645a9cc\"," +
                "\"schemaId\":\"8faozNpSjFfPJXYtgcPtmJ:2:commercialregister:1.2\",\"credentialDefinitionId\":" +
                "\"8faozNpSjFfPJXYtgcPtmJ:3:CL:1041:Commercial Registry Entry (Open Corporates)\"}";

        Credential credential = gson.fromJson(ariesCredential, Credential.class);
        BPACredentialExchange myCredential = BPACredentialExchange
                .builder()
                .id(UUID.randomUUID())
                .credential(credential)
                .type(CredentialType.INDY)
                .build();
        VerifiableCredential.VerifiableIndyCredential indyCred = vpm.buildFromCredential(myCredential);
        assertNotNull(indyCred.getCredentialSubject());
        assertEquals(2, c.toMap(indyCred.getCredentialSubject()).size());
        assertEquals(2, indyCred.getType().size());
        assertEquals(2, indyCred.getContext().size());
        // System.out.println(GsonConfig.prettyPrinter().toJson(indyCred));
    }

    private Map<String, Object> createMap(String json) throws JsonProcessingException {
        final JsonNode node = m.readTree(json);
        return c.toMap(node);
    }

    private static MyDocument buildDefault() {
        return new MyDocument()
                .setCreatedAt(Instant.now())
                .setId(UUID.randomUUID())
                .setIsPublic(Boolean.TRUE)
                .setUpdatedAt(Instant.now());
    }

}
