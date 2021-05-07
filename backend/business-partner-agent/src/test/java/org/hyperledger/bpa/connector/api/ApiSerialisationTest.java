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
package org.hyperledger.bpa.connector.api;

import org.hyperledger.aries.api.resolver.DIDDocument;
import org.hyperledger.bpa.BaseTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ApiSerialisationTest extends BaseTest {

    @Test
    void testVerificationMethodFlat() throws Exception {
        String didDocument = loader.load("files/didDocument.json");
        DIDDocument didDoc = mapper.readValue(didDocument, DIDDocument.class);

        assertNotNull(didDoc);
        assertNotNull(didDoc.getService());
        assertEquals(2, didDoc.getService().size());

        List<DIDDocument.VerificationMethod> meth = didDoc.getVerificationMethod();
        assertEquals(1, meth.size());
        assertTrue(meth.get(0).getPublicKeyBase58().startsWith("AWrdq"));

        assertTrue(didDoc.hasAriesEndpoint());

        Optional<String> publicProfileUrl = didDoc.findPublicProfileUrl();
        assertTrue(publicProfileUrl.isPresent());
        assertTrue(publicProfileUrl.get().startsWith("https://bob.iil"));
    }

    @Test
    void testVerificationMethodList() throws Exception {
        String didDocument = loader.load("files/didLocal.json");
        DIDDocument didDoc = mapper.readValue(didDocument, DIDDocument.class);

        assertNotNull(didDoc);
        assertNotNull(didDoc.getService());
        assertEquals(1, didDoc.getService().size());

        assertFalse(didDoc.hasAriesEndpoint());

        List<DIDDocument.VerificationMethod> meth = didDoc.getVerificationMethod();
        assertEquals(2, meth.size());
        assertTrue(meth.get(1).getPublicKeyBase58().startsWith("C2VBLJf"));
    }
}
