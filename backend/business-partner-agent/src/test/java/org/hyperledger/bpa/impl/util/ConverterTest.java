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
        VerifiablePresentation<VerifiableIndyCredential> vp = loadAndConvertTo("files/verifiablePresentation.json",
                Converter.VP_TYPEREF);
        final PartnerAPI partner = conv.toAPIObject(vp);

        assertEquals(3, partner.getCredential().size());

        PartnerAPI.PartnerCredential c1 = partner.getCredential().get(0);
        assertEquals(CredentialType.INDY, c1.getType());
        assertEquals("did:sov:Ni2hE7fEHJ25xUBc7ZESf6", c1.getIssuer());
        assertFalse(c1.getIndyCredential());
        assertNotNull(c1.getTypeLabel());

        PartnerAPI.PartnerCredential c2 = partner.getCredential().get(1);
        assertEquals(CredentialType.ORGANIZATIONAL_PROFILE_CREDENTIAL, c2.getType());
        assertFalse(c2.getIndyCredential());
        assertNotNull(c2.getTypeLabel());

        PartnerAPI.PartnerCredential c3 = partner.getCredential().get(2);
        assertEquals(CredentialType.INDY, c3.getType());
        assertEquals("did:sov:M6Mbe3qx7vB4wpZF4sBRjt", c3.getIssuer());
        assertNotNull(c3.getTypeLabel());
        assertTrue(c3.getIndyCredential());
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
