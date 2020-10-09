package org.hyperledger.oa.impl.aries;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import javax.inject.Inject;

import org.hyperledger.aries.api.credential.CredentialExchange;
import org.hyperledger.aries.config.GsonConfig;
import org.hyperledger.oa.RunWithAries;
import org.hyperledger.oa.api.CredentialType;
import org.hyperledger.oa.client.CachingAriesClient;
import org.hyperledger.oa.impl.activity.CryptoManager;
import org.hyperledger.oa.impl.activity.Identity;
import org.hyperledger.oa.impl.activity.VPManager;
import org.hyperledger.oa.model.MyCredential;
import org.hyperledger.oa.repository.MyCredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

@MicronautTest
public class CredentialManagerIntegrationTest extends RunWithAries {

    @Inject
    AriesCredentialManager mgmt;

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
        final CredentialExchange credEx = createNewCredential();
        mgmt.handleCredentialAcked(credEx);
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
        assertNull(vpMgmt.getVerifiablePresentation().get().getVerifiableCredential());
    }

    private CredentialExchange createNewCredential() {
        credRepo.save(MyCredential
                .builder()
                .type(CredentialType.BANK_ACCOUNT_CREDENTIAL)
                .isPublic(Boolean.FALSE)
                .connectionId("dummy")
                .state("dummy")
                .threadId("cab34089-446c-411d-948e-9ed39ba6777f").build());
        final String ex = loader.load("files/credentialExchange.json");
        final CredentialExchange credEx = GsonConfig.defaultConfig().fromJson(ex, CredentialExchange.class);
        return credEx;
    }
}
