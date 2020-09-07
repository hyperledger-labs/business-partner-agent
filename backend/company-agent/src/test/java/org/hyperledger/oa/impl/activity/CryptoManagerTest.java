/**
 * Copyright (c) 2020 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/organizational-agent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hyperledger.oa.impl.activity;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.hyperledger.aries.api.jsonld.VerifiableCredential.VerifiableIndyCredential;
import org.hyperledger.aries.api.jsonld.VerifiablePresentation;
import org.hyperledger.aries.api.jsonld.VerifiablePresentation.VerifiablePresentationBuilder;
import org.hyperledger.oa.RunWithAries;
import org.hyperledger.oa.client.CachingAriesClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.micronaut.test.annotation.MicronautTest;

@MicronautTest
class CryptoManagerTest extends RunWithAries {

    @Inject
    private CryptoManager mgmt;

    @Inject
    private Identity id;

    @Inject
    private CachingAriesClient cAC;

    @BeforeEach
    public void setupCryptoManager() {
        mgmt.setAcaPy(ac);
        id.setAcaPy(ac);
        cAC.setAc(ac);
    }

    @Test
    void testSignEmptyDefault() throws Exception {
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
