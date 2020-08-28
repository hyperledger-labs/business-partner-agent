package org.hyperledger.oa.impl.aries;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.credential.CredentialAttributes;
import org.hyperledger.aries.api.credential.CredentialExchange;
import org.hyperledger.aries.api.credential.CredentialProposalRequest;
import org.hyperledger.aries.api.credential.CredentialProposalRequest.CredentialPreview;
import org.hyperledger.aries.api.revocation.RevRegCreateResponse;
import org.hyperledger.aries.config.GsonConfig;
import org.hyperledger.oa.api.aries.BankAccount;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Disabled
class CredentialTest {

    // contoso
    // https://limemillie.aries.bosch-digital.de
    // 48E9DEC9A3B74071A8B147C001D76B38

    // alice
    // https://limemilo.aries.bosch-digital.de
    // 6420D273383F497FB63FDF9122011AB9

    @Test
    void test() throws Exception {
        AriesClient ac = new AriesClient("https://limemillie.aries.bosch-digital.de",
                "48E9DEC9A3B74071A8B147C001D76B38");

        BankAccount ba = new BankAccount("iban", "bic");

        final Optional<CredentialExchange> cp = ac.issueCredentialSendProposal(
                CredentialProposalRequest
                        .builder()
                        .connectionId("a38a19a6-7d74-4e19-9a5e-43d7f10e8d2c")
                        .credentialProposal(new CredentialPreview(CredentialAttributes.from(ba)))
                        .schemaId("M6Mbe3qx7vB4wpZF4sBRjt:2:bank_account:1.0")
                        .credentialDefinitionId("VoSfM3eGaPxduty34ySygw:3:CL:571:sparta_bank")
                        .build());
        assertTrue(cp.isPresent());
        log.debug(GsonConfig.prettyPrinter().toJson(cp.get()));
    }

    @Test
    void testCredDef() throws Exception {
        AriesClient ac = new AriesClient("https://limemillie.aries.bosch-digital.de",
                "48E9DEC9A3B74071A8B147C001D76B38");

        RevocationRegistry reg = new RevocationRegistry();
        reg.setAc(ac);
        reg.setHost("https://limemillie.aries.bosch-digital.de");
        reg.createRevRegForCredDefIfNeeded("M6Mbe3qx7vB4wpZF4sBRjt:3:CL:571:bank_account");
    }

    @Test
    void test2() throws Exception {
        AriesClient ac = new AriesClient("https://limemilo.aries.bosch-digital.de", "6420D273383F497FB63FDF9122011AB9");

        final Optional<RevRegCreateResponse> reg = ac.revocationRegistryGetById(
                "VoSfM3eGaPxduty34ySygw:4:VoSfM3eGaPxduty34ySygw:3:CL:571:sparta_bank:CL_ACCUM:58d9bb8c-a156-42a1-980f-d2ced76e09c6");

        System.err.println(GsonConfig.prettyPrinter().toJson(reg.get()));

        final Optional<RevRegCreateResponse> pub = ac.revocationRegistryPublish(reg.get().getRevocRegId());

        System.err.println(GsonConfig.prettyPrinter().toJson(pub.get()));
    }

}
