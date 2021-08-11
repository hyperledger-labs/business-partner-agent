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
package org.hyperledger.bpa.impl.aries;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;
import org.hyperledger.aries.config.GsonConfig;
import org.hyperledger.bpa.RunWithAries;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.client.CachingAriesClient;
import org.hyperledger.bpa.impl.activity.CryptoManager;
import org.hyperledger.bpa.impl.activity.Identity;
import org.hyperledger.bpa.impl.activity.VPManager;
import org.hyperledger.bpa.model.MyCredential;
import org.hyperledger.bpa.repository.MyCredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
public class CredentialManagerIntegrationTest extends RunWithAries {

    @Inject
    HolderCredentialManager mgmt;

    @Inject
    MyCredentialRepository credRepo;

    @Inject
    VPManager vpMgmt;

    @Inject
    CryptoManager crypto;

    @Inject
    Identity id;

    @Inject
    CachingAriesClient acaCache;

    @BeforeEach
    public void injectAries() {
        mgmt.setAc(ac);
        crypto.setAcaPy(ac);
        acaCache.setAc(ac);
        id.setAcaPy(ac);
        id.setAcaCache(acaCache);
    }

    @Test
    void testDeleteCredential() throws Exception {
        // create credential
        final V1CredentialExchange credEx = createNewCredential();
        mgmt.handleV1CredentialExchangeAcked(credEx);
        assertEquals(1, credRepo.count());
        assertTrue(vpMgmt.getVerifiablePresentation().isEmpty());

        // make it public
        final UUID credId = credRepo.findAll().iterator().next().getId();
        mgmt.toggleVisibility(credId);
        waitForVP(vpMgmt, true);
        assertTrue(vpMgmt.getVerifiablePresentation().isPresent());
        assertNotNull(vpMgmt.getVerifiablePresentation().get().getVerifiableCredential());
        assertEquals(1, vpMgmt.getVerifiablePresentation().get().getVerifiableCredential().size());

        // delete it again
        mgmt.deleteCredentialById(credId);
        waitForVCDeletion(vpMgmt);
        assertTrue(vpMgmt.getVerifiablePresentation().isPresent());
        assertNotNull(vpMgmt.getVerifiablePresentation().get().getVerifiableCredential());
        assertTrue(vpMgmt.getVerifiablePresentation().get().getVerifiableCredential().isEmpty());
    }

    private V1CredentialExchange createNewCredential() {
        credRepo.save(MyCredential
                .builder()
                .type(CredentialType.INDY)
                .isPublic(Boolean.FALSE)
                .connectionId("dummy")
                .state(CredentialExchangeState.CREDENTIAL_ISSUED)
                .threadId("cab34089-446c-411d-948e-9ed39ba6777f").build());
        final String ex = loader.load("files/credentialExchange.json");
        return GsonConfig.defaultConfig().fromJson(ex, V1CredentialExchange.class);
    }
}
