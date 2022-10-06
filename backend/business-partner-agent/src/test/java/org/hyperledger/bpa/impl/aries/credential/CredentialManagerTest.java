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
package org.hyperledger.bpa.impl.aries.credential;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.hyperledger.aries.api.jsonld.VerifiableCredential.VerifiableIndyCredential;
import org.hyperledger.aries.api.jsonld.VerifiablePresentation;
import org.hyperledger.bpa.BaseTest;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.persistence.model.Partner;
import org.hyperledger.bpa.persistence.repository.PartnerRepository;
import org.hyperledger.bpa.testutil.FileLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@MicronautTest
class CredentialManagerTest extends BaseTest {

    private static final String DID = "did:sov:M6Mbe3qx7vB4wpZF4sBRjt";

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    Converter conv;

    @Test
    void testResolveIssuerDidOnly() {
        Partner p = partnerRepo.save(Partner
                .builder()
                .did(DID)
                .ariesSupport(Boolean.TRUE)
                .build());

        String iss = HolderManager.resolveIssuer(p);
        assertEquals(DID, iss);
    }

    @Test
    void testResolveIssuerByAlias() {
        Partner p = partnerRepo.save(Partner
                .builder()
                .alias("My Bank")
                .did(DID)
                .ariesSupport(Boolean.TRUE)
                .build());

        String iss = HolderManager.resolveIssuer(p);
        assertEquals("My Bank", iss);
    }

    @Test
    void testResolveIssuerByVP() throws Exception {
        final String json = FileLoader.load("files/verifiablePresentation.json");
        final VerifiablePresentation<VerifiableIndyCredential> vp = mapper.readValue(json, Converter.VP_TYPEREF);

        Partner p = partnerRepo.save(Partner
                .builder()
                .did(DID)
                .ariesSupport(Boolean.TRUE)
                .verifiablePresentation(vp)
                .build());

        String iss = HolderManager.resolveIssuer(p);
        assertEquals("Test Corp", iss);
    }

    @Test
    void testResolveIssuerByLabel() {
        Partner p = partnerRepo.save(Partner
                .builder()
                .did(DID)
                .ariesSupport(Boolean.TRUE)
                .incoming(Boolean.TRUE)
                .label("Their Label")
                .build());

        String iss = HolderManager.resolveIssuer(p);
        assertEquals("Their Label", iss);
    }

    @Test
    void testResolveIssuerNull() {
        String iss = HolderManager.resolveIssuer(null);
        assertNull(iss);
    }

}
