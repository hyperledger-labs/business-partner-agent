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
package org.hyperledger.oa.impl.activity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hyperledger.aries.api.jsonld.VerifiableCredential;
import org.hyperledger.oa.api.CredentialType;
import org.hyperledger.oa.impl.util.Converter;
import org.hyperledger.oa.model.MyDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VPManagerTest {

    private final ObjectMapper m = new ObjectMapper();
    private final Converter c = new Converter();
    private final VPManager vpm = new VPManager();

    @BeforeEach
    void setup() {
        c.setMapper(m);
        vpm.setConverter(c);
    }

    @Test
    void testBuildFromDocumentOrg() throws Exception {
        String json = "{\"id\":\"did:sov:iil:sadfafs\",\"type\":\"LegalEntity\"}";
        final Map<String, Object> d = createMap(json);

        MyDocument doc = buildDefault()
                .setType(CredentialType.ORGANIZATIONAL_PROFILE_CREDENTIAL)
                .setDocument(d);
        final VerifiableCredential vp = vpm.buildFromDocument(doc, "xxyyyzzz");
        assertEquals("{type=LegalEntity, id=xxyyyzzz}",
                vp.getCredentialSubject().toString());
    }

    @Test
    void testBuildFromDocumentBA() throws Exception {
        String json = "{\"iban\":\"1234\",\"bic\":\"4321\"}";
        final Map<String, Object> d = createMap(json);

        MyDocument doc = buildDefault()
                .setType(CredentialType.BANK_ACCOUNT_CREDENTIAL)
                .setDocument(d);
        final VerifiableCredential vp = vpm.buildFromDocument(doc, "xxyyyzzz");
        assertEquals("BankAccountVC(id=xxyyyzzz, bankAccount=BankAccount(iban=1234, bic=4321))",
                vp.getCredentialSubject().toString());
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
