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
package org.hyperledger.bpa.impl.aries;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.hyperledger.aries.api.credential.Credential;
import org.hyperledger.aries.api.credential.CredentialExchange;
import org.hyperledger.aries.api.jsonld.VerifiableCredential.VerifiableIndyCredential;
import org.hyperledger.aries.api.jsonld.VerifiablePresentation;
import org.hyperledger.aries.config.GsonConfig;
import org.hyperledger.bpa.BaseTest;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.model.MyCredential;
import org.hyperledger.bpa.model.Partner;
import org.hyperledger.bpa.repository.MyCredentialRepository;
import org.hyperledger.bpa.repository.PartnerRepository;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@MicronautTest
class CredentialManagerTest extends BaseTest {

    private static final String CRED_DEF_ID = "M6Mbe3qx7vB4wpZF4sBRjt:3:CL:571:ba";
    private static final String DID = "did:sov:iil:M6Mbe3qx7vB4wpZF4sBRjt";

    @Inject
    CredentialManager mgmt;

    @Inject
    MyCredentialRepository credRepo;

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    Converter conv;

    @Test
    void testSaveNewCredential() {
        credRepo.save(MyCredential
                .builder()
                .type(CredentialType.SCHEMA_BASED)
                .isPublic(Boolean.FALSE)
                .connectionId("dummy")
                .state("dummy")
                .threadId("cab34089-446c-411d-948e-9ed39ba6777f").build());
        final String ex = loader.load("files/credentialExchange.json");
        final CredentialExchange credEx = GsonConfig.defaultConfig().fromJson(ex, CredentialExchange.class);
        mgmt.handleCredentialAcked(credEx);
        assertEquals(1, credRepo.count());
    }

    @Test
    void testResolveIssuerDidOnly() {
        Credential c = new Credential();
        c.setCredentialDefinitionId(CRED_DEF_ID);
        String iss = mgmt.resolveIssuer(c);
        assertEquals(DID, iss);
    }

    @Test
    void testResolveIssuerByAlias() {
        Credential c = new Credential();
        c.setCredentialDefinitionId(CRED_DEF_ID);

        partnerRepo.save(Partner
                .builder()
                .alias("My Bank")
                .did(DID)
                .ariesSupport(Boolean.TRUE)
                .build());

        String iss = mgmt.resolveIssuer(c);
        assertEquals("My Bank", iss);
    }

    @Test
    void testResolveIssuerByVP() throws Exception {
        Credential c = new Credential();
        c.setCredentialDefinitionId(CRED_DEF_ID);

        final String json = loader.load("files/verifiablePresentation.json");
        final VerifiablePresentation<VerifiableIndyCredential> vp = mapper.readValue(json, Converter.VP_TYPEREF);

        partnerRepo.save(Partner
                .builder()
                .did(DID)
                .ariesSupport(Boolean.TRUE)
                .verifiablePresentation(conv.toMap(vp))
                .build());

        String iss = mgmt.resolveIssuer(c);
        assertEquals("Bosch Healthcare", iss);
    }

    @Test
    void testResolveIssuerByLabel() {
        Credential c = new Credential();
        c.setCredentialDefinitionId(CRED_DEF_ID);

        partnerRepo.save(Partner
                .builder()
                .did(DID)
                .ariesSupport(Boolean.TRUE)
                .incoming(Boolean.TRUE)
                .label("Their Label")
                .build());

        String iss = mgmt.resolveIssuer(c);
        assertEquals("Their Label", iss);
    }

    @Test
    void testResolveIssuerNull() {
        String iss = mgmt.resolveIssuer(null);
        assertNull(iss);
    }

}
