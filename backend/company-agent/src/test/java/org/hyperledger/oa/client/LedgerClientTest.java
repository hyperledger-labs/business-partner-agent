package org.hyperledger.oa.client;

import java.util.List;
import java.util.Optional;

import org.hyperledger.oa.BaseTest;
import org.hyperledger.oa.controller.api.partner.PartnerCredentialType;

class LedgerClientTest extends BaseTest {

    // @Test
    void test() {
        LedgerClient c = new LedgerClient();
        c.setMapper(mapper);

        final Optional<List<PartnerCredentialType>> credDefIds = c.getCredentialDefinitionIdsForDid(
                "CHysca6fY8n8ytCDLAJGZj");
        System.err.println(credDefIds.get());
    }
}
