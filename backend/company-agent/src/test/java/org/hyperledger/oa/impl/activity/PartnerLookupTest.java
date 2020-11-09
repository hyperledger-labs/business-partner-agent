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
package org.hyperledger.oa.impl.activity;

import org.hyperledger.oa.BaseTest;
import org.hyperledger.oa.api.DidDocAPI;
import org.hyperledger.oa.api.DidDocAPI.PublicKey;
import org.hyperledger.oa.client.api.DidDocument;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PartnerLookupTest extends BaseTest {

    @Test
    void testResolvePublicKeyWithKeyId() throws Exception {
        DidDocument didDoc = loadAndConvertTo("files/didEvan.json", DidDocument.class);

        final Optional<PublicKey> publicKey = PartnerLookup.resolvePublicKey(didDoc.getDidDocument().getPublicKey());
        assertTrue(publicKey.isPresent());
        assertTrue(publicKey.get().getPublicKeyBase58().startsWith("EkU6jKv"));
    }

    @Test
    void testResolvePublicKeyNoKeyId() throws Exception {
        DidDocument didDoc = loadAndConvertTo("files/didLocal.json", DidDocument.class);
        final Optional<String> matchKey = PartnerLookup.matchKey(null, didDoc.getDidDocument().getPublicKey());
        assertTrue(matchKey.isPresent());
        // expecting first match
        assertTrue(matchKey.get().startsWith("D2k3NWUD"));
    }

    @Test
    void testResolvePublicKeyKeyIdProvided() throws Exception {
        DidDocument didDoc = loadAndConvertTo("files/didLocal.json", DidDocument.class);
        final Optional<String> matchKey = PartnerLookup.matchKey(
                "did:web:localhost:8020#key-2", didDoc.getDidDocument().getPublicKey());
        assertTrue(matchKey.isPresent());
        assertTrue(matchKey.get().startsWith("C2VBLJff"));
    }

    @Test
    void testResolveEndpoint() throws Exception {
        DidDocument didDoc = loadAndConvertTo("files/didEvan.json", DidDocument.class);

        Optional<Map<String, String>> ep = PartnerLookup.filterServices(didDoc.getDidDocument());
        assertTrue(ep.isPresent());
        assertEquals(1, ep.get().size());
        assertTrue(ep.get().get("profile").startsWith("https://test.test.com"));
    }

    @Test
    void testResolveEndpointEmpty() {
        Optional<Map<String, String>> ep = PartnerLookup.filterServices(new DidDocAPI());
        assertFalse(ep.isEmpty());
        assertEquals(0, ep.get().size());
    }

}
