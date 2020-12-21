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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hyperledger.bpa.BaseTest;
import org.hyperledger.bpa.client.api.DidDocument;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PartnerLookupTest extends BaseTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testResolvePublicKeyNoKeyId() throws Exception {
        DidDocument didDoc = loadAndConvertTo("files/didLocal.json", DidDocument.class);
        final Optional<String> matchKey = PartnerLookup.matchKey(null,
                didDoc.getDidDocument().getVerificationMethod(mapper));
        assertTrue(matchKey.isPresent());
        // expecting first match
        assertTrue(matchKey.get().startsWith("D2k3NWUD"));
    }

    @Test
    void testResolvePublicKeyKeyIdProvided() throws Exception {
        DidDocument didDoc = loadAndConvertTo("files/didLocal.json", DidDocument.class);
        final Optional<String> matchKey = PartnerLookup.matchKey(
                "did:web:localhost:8020#key-2", didDoc.getDidDocument().getVerificationMethod(mapper));
        assertTrue(matchKey.isPresent());
        assertTrue(matchKey.get().startsWith("C2VBLJff"));
    }

}
