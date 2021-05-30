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

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.hyperledger.aries.api.jsonld.VerifiableCredential.VerifiableIndyCredential;
import org.hyperledger.aries.api.jsonld.VerifiablePresentation;
import org.hyperledger.aries.api.jsonld.VerifiablePresentation.VerifiablePresentationBuilder;
import org.hyperledger.bpa.RunWithAries;
import org.hyperledger.bpa.client.CachingAriesClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
class CryptoManagerTest extends RunWithAries {

    @Inject
    CryptoManager mgmt;

    @Inject
    Identity id;

    @Inject
    CachingAriesClient cAC;

    @BeforeEach
    public void setupCryptoManager() {
        mgmt.setAcaPy(ac);
        id.setAcaPy(ac);
        cAC.setAc(ac);
    }

    @Test
    void testSignEmptyDefault() {
        VerifiableIndyCredential vc = VerifiableIndyCredential
                .builder()
                .build();
        final VerifiablePresentationBuilder<VerifiableIndyCredential> builder = VerifiablePresentation.builder();
        VerifiablePresentation<VerifiableIndyCredential> vp = builder
                .verifiableCredential(List.of(vc))
                .build();
        final Optional<VerifiablePresentation<VerifiableIndyCredential>> signed = mgmt.sign(vp);
        assertTrue(signed.isPresent());
        assertNotNull(signed.get().getProof());
        assertEquals("authentication", signed.get().getProof().getProofPurpose());
    }

    @Test
    void testSignWithCommercialRegister() throws Exception {
        VerifiableIndyCredential vc = loadAndConvertTo(
                "files/verifiableCredentialAdHocContext.json", VerifiableIndyCredential.class);
        final VerifiablePresentationBuilder<VerifiableIndyCredential> builder = VerifiablePresentation.builder();
        VerifiablePresentation<VerifiableIndyCredential> vp = builder
                .verifiableCredential(List.of(vc))
                .build();
        final Optional<VerifiablePresentation<VerifiableIndyCredential>> signed = mgmt.sign(vp);
        assertTrue(signed.isPresent());
        assertNotNull(signed.get().getProof());
    }

    @Test
    void testSignWithMasterdata() throws Exception {
        VerifiableIndyCredential vc = loadAndConvertTo(
                "files/verifiableCredential.json", VerifiableIndyCredential.class);
        final VerifiablePresentationBuilder<VerifiableIndyCredential> builder = VerifiablePresentation.builder();
        VerifiablePresentation<VerifiableIndyCredential> vp = builder
                .verifiableCredential(List.of(vc))
                .build();
        final Optional<VerifiablePresentation<VerifiableIndyCredential>> signed = mgmt.sign(vp);
        assertTrue(signed.isPresent());
        assertNotNull(signed.get().getProof());
        assertEquals("authentication", signed.get().getProof().getProofPurpose());
        assertEquals("did:sov:iil:asdfsafs", signed.get().getVerifiableCredential().get(0).getIssuer());
    }

}
