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
package org.hyperledger.bpa.impl.util;

import com.fasterxml.jackson.databind.JsonNode;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.hyperledger.aries.api.jsonld.VerifiableCredential.VerifiableIndyCredential;
import org.hyperledger.aries.api.jsonld.VerifiablePresentation;
import org.hyperledger.bpa.BaseTest;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.api.MyDocumentAPI;
import org.hyperledger.bpa.api.PartnerAPI;
import org.hyperledger.bpa.impl.CredentialTestUtils;
import org.hyperledger.bpa.model.MyDocument;
import org.hyperledger.bpa.model.Partner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
class ConverterTest extends BaseTest {

    @Inject
    Converter conv;

    private CredentialTestUtils utils;

    @BeforeEach
    public void setupUtils() {
        utils = new CredentialTestUtils(mapper);
    }

    @Test
    void testConvertWalletDocumentRequest() throws Exception {
        final String content = "{\"test\":\"test\"}";
        final JsonNode node = mapper.readValue(content, JsonNode.class);

        final Map<String, Object> map = conv.toMap(node);
        final JsonNode fromMap = conv.fromMap(map, JsonNode.class);

        String contentResult = mapper.writeValueAsString(fromMap);
        assertEquals(content, contentResult);
    }

    @Test
    void testConvertVPToPartnerApi() throws Exception {
        String issuerIsSelf = "did:sov:F6dB7dMVHUQSC64qemnBi7";
        String expectedIndySchemaId = "M6Mbe3qx7vB4wpZF4sBRjt:2:bank_account:1.0";
        VerifiablePresentation<VerifiableIndyCredential> vp = loadAndConvertTo("files/verifiablePresentation.json",
                Converter.VP_TYPEREF);
        final PartnerAPI partner = conv.toAPIObject(vp);

        // expecting all four types that are currently supported
        assertEquals(4, partner.getCredential().size());

        // JSON-LD Type Document
        PartnerAPI.PartnerCredential c1 = partner.getCredential().get(0);
        assertEquals(CredentialType.JSON_LD, c1.getType());
        assertEquals(issuerIsSelf, c1.getIssuer());
        assertFalse(c1.getIndyCredential());
        assertEquals("Person", c1.getTypeLabel());
        assertTrue(c1.getSchemaId().startsWith("https://schema.org/"));

        // Org Profile Document
        PartnerAPI.PartnerCredential c2 = partner.getCredential().get(1);
        assertEquals(CredentialType.ORGANIZATIONAL_PROFILE_CREDENTIAL, c2.getType());
        assertEquals(issuerIsSelf, c2.getIssuer());
        assertFalse(c2.getIndyCredential());
        assertEquals("Organizational Profile", c2.getTypeLabel());
        assertNull(c2.getSchemaId());

        // Indy based Document
        PartnerAPI.PartnerCredential c3 = partner.getCredential().get(2);
        assertEquals(CredentialType.INDY, c3.getType());
        assertEquals(issuerIsSelf, c3.getIssuer());
        assertNotNull(c3.getTypeLabel());
        assertEquals("bank_account", c3.getTypeLabel());
        assertFalse(c3.getIndyCredential());
        assertEquals(expectedIndySchemaId, c3.getSchemaId());

        // Indy based credential
        PartnerAPI.PartnerCredential c4 = partner.getCredential().get(3);
        assertEquals(CredentialType.INDY, c4.getType());
        assertEquals("did:sov:Uv53vZ1SnS3NPYMMSr4BaQ", c4.getIssuer());
        assertNotNull(c4.getTypeLabel());
        assertEquals("bank_account", c4.getTypeLabel());
        assertTrue(c4.getIndyCredential());
        assertEquals(expectedIndySchemaId, c4.getSchemaId());
    }

    @Test
    void testConvertVPToPartnerModel() throws Exception {
        VerifiablePresentation<VerifiableIndyCredential> vp = loadAndConvertTo("files/verifiablePresentation.json",
                Converter.VP_TYPEREF);
        final PartnerAPI partner = conv.toAPIObject(vp);
        final Partner model = conv.toModelObject("did:web:test.foo", partner);
        assertTrue(model.getDid().startsWith("did"));
        assertNotNull(model.getVerifiablePresentation());
        assertEquals(vp, conv.fromMap(model.getVerifiablePresentation(), Converter.VP_TYPEREF));
    }

    @Test
    void testConvertCredentialToModelObject() throws Exception {
        MyDocumentAPI c = utils.createDummyCred(CredentialType.ORGANIZATIONAL_PROFILE_CREDENTIAL, Boolean.TRUE);
        c.setCreatedDate(123L); // should not be used but set by database layer
        c.setId(UUID.randomUUID()); // should not be used but set by database layer

        MyDocument result = conv.toModelObject(c);

        assertEquals(c.getType(), result.getType());
        assertEquals(c.getIsPublic(), result.getIsPublic());
        assertNotNull(result.getDocument());
        assertTrue(result.getDocument().containsValue("Hello"));
        assertEquals(c.getIsPublic(), result.getIsPublic());
        assertNull(result.getCreatedAt());
        assertNotEquals(c.getId(), result.getId());
    }
}
