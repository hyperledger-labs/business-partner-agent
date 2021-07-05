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
package org.hyperledger.bpa.client;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.hyperledger.aries.api.jsonld.VerifiableCredential.VerifiableIndyCredential;
import org.hyperledger.aries.api.jsonld.VerifiablePresentation;
import org.hyperledger.aries.api.resolver.DIDDocument;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
class URClientTest {

    @Inject
    private URClient c;

    @Test
    @Disabled
    void testGetDidDocument() {
        String did = "did:evan:testcore:0x521a97b8baecde51038e2c83f5a37890690e9118";
        Optional<DIDDocument> didDocument = c.getDidDocument(did);
        assertTrue(didDocument.isPresent());
        System.err.println(didDocument);
    }

    @Test
    @Disabled
    void testGetMasterdata() {
        String url = "https://ipfs.test.evan.network/ipfs/QmQGGjnY88gwvD6xSKXKbxz6n1Vdk9Q5WAMtar3WdEXmun";
        // String url2 = "https://acme.iil.network/md.json";
        Optional<VerifiablePresentation<VerifiableIndyCredential>> masterdata = c.getPublicProfile(url);
        assertTrue(masterdata.isPresent());
        System.err.println(masterdata.get());
    }

}
