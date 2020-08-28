package org.hyperledger.oa.impl.aries;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.hyperledger.aries.api.credential.CredentialExchange;
import org.hyperledger.aries.config.GsonConfig;
import org.hyperledger.oa.BaseTest;
import org.hyperledger.oa.api.CredentialType;
import org.hyperledger.oa.model.MyCredential;
import org.hyperledger.oa.repository.MyCredentialRepository;
import org.junit.jupiter.api.Test;

import io.micronaut.test.annotation.MicronautTest;

@MicronautTest
class AriesCredentialManagerTest extends BaseTest {

    @Inject
    private AriesCredentialManager mgmt;

    @Inject
    private MyCredentialRepository credRepo;

    @Test
    void testSaveNewCredential() throws Exception {
        credRepo.save(MyCredential
                .builder()
                .type(CredentialType.BANK_ACCOUNT_CREDENTIAL)
                .isPublic(Boolean.FALSE)
                .connectionId("dummy")
                .state("dummy")
                .threadId("cab34089-446c-411d-948e-9ed39ba6777f").build());
        final String ex = loader.load("files/credentialExchange.json");
        final CredentialExchange credEx = GsonConfig.defaultConfig().fromJson(ex, CredentialExchange.class);
        mgmt.handleCredentialAcked(credEx);
        assertEquals(1, credRepo.count());
    }

}
