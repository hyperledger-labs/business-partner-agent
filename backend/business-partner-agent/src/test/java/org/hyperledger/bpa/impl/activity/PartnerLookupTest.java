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

import org.hyperledger.aries.api.resolver.DIDDocument;
import org.hyperledger.bpa.BaseTest;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PartnerLookupTest extends BaseTest {

    @Test
    void testResolvePublicKeyNoKeyId() throws Exception {
        DIDDocument didDoc = loadAndConvertTo("files/didLocal.json", DIDDocument.class);
        final Optional<String> matchKey = PartnerLookup.matchKey(null,
                didDoc.getVerificationMethod());
        assertTrue(matchKey.isPresent());
        // expecting first match
        assertTrue(matchKey.get().startsWith("D2k3NWUD"));
    }

    @Test
    void testResolvePublicKeyKeyIdProvided() throws Exception {
        DIDDocument didDoc = loadAndConvertTo("files/didLocal.json", DIDDocument.class);
        final Optional<String> matchKey = PartnerLookup.matchKey(
                "did:web:localhost:8020#key-2", didDoc.getVerificationMethod());
        assertTrue(matchKey.isPresent());
        assertTrue(matchKey.get().startsWith("C2VBLJff"));
    }

}
