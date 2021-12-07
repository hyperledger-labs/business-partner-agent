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
package org.hyperledger.bpa.repository;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;
import org.hyperledger.aries.config.GsonConfig;
import org.hyperledger.bpa.BaseTest;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.model.BPACredentialExchange;
import org.hyperledger.bpa.model.Partner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
class HolderCredExRepositoryTest extends BaseTest {

    @Inject
    HolderCredExRepository holderCredExRepo;

    @Inject
    PartnerRepository partnerRepo;

    @Test
    void testSaveCredential() {
        String schemaId = "F6dB7dMVHUQSC64qemnBi7:2:spaces:1.0";
        String credDefId = "EraYCDJUPsChbkw7S1vV96:3:CL:4740:spaces";

        final String json = loader.load("files/v1-credex-holder/04-acked.json");
        final V1CredentialExchange ex = GsonConfig.defaultConfig().fromJson(json, V1CredentialExchange.class);

        BPACredentialExchange cred = BPACredentialExchange
                .builder()
                .type(CredentialType.INDY)
                .isPublic(Boolean.TRUE)
                .partner(createRandomPartner())
                .state(CredentialExchangeState.CREDENTIAL_ACKED)
                .threadId("1")
                .credential(ex.getCredential())
                .role(CredentialExchangeRole.HOLDER)
                .credentialExchangeId(UUID.randomUUID().toString())
                .build();
        final BPACredentialExchange saved = holderCredExRepo.save(cred);

        final List<BPACredentialExchange> credLoaded = holderCredExRepo.findBySchemaIdAndCredentialDefinitionId(
                schemaId,
                credDefId);
        assertFalse(credLoaded.isEmpty());
        assertEquals(1, credLoaded.size());
        assertEquals(saved.getId(), credLoaded.get(0).getId());
    }

    @Test
    void testUpdateByPartnerId() {
        Partner p = createRandomPartner();
        Partner other = createRandomPartner();
        BPACredentialExchange ex1 = holderCredExRepo.save(createDummyCredEx(p));
        holderCredExRepo.save(createDummyCredEx(p));
        holderCredExRepo.save(createDummyCredEx(other));

        final List<BPACredentialExchange> byPartnerId = holderCredExRepo.findByPartnerId(p.getId());
        assertEquals(2, byPartnerId.size());

        Number updated = holderCredExRepo.setPartnerIdToNull(p.getId());
        assertEquals(2, updated.intValue());

        updated = holderCredExRepo.setPartnerIdToNull(UUID.randomUUID());
        assertEquals(0, updated.intValue());
        ex1 = holderCredExRepo.findById(ex1.getId()).orElseThrow();
        assertNull(ex1.getPartner());

        updated = holderCredExRepo.updateIssuerByPartnerId(other.getId(), "My Bank");
        assertEquals(1, updated.intValue());

        final List<BPACredentialExchange> cred = holderCredExRepo.findByPartnerId(other.getId());
        assertEquals(1, cred.size());
        assertEquals("My Bank", cred.get(0).getIssuer());
    }

    @Test
    void testCountByState() {
        Partner p = createRandomPartner();
        holderCredExRepo.save(createDummyCredEx(p));
        holderCredExRepo.save(createDummyCredEx(p).setState(CredentialExchangeState.CREDENTIAL_ISSUED));
        holderCredExRepo.save(createDummyCredEx(createRandomPartner()));

        assertEquals(2, holderCredExRepo.countByRoleEqualsAndStateEquals(
                CredentialExchangeRole.HOLDER, CredentialExchangeState.CREDENTIAL_ACKED));
    }

    @Test
    void testFindNotRevoked() {
        Partner p = createRandomPartner();
        holderCredExRepo.save(createDummyCredEx(p));
        holderCredExRepo.save(createDummyCredEx(p).setType(CredentialType.ORGANIZATIONAL_PROFILE_CREDENTIAL));
        holderCredExRepo.save(createDummyCredEx(p).setType(CredentialType.INDY));
        holderCredExRepo.save(createDummyCredEx(p).setType(CredentialType.INDY).setRevoked(Boolean.FALSE));
        holderCredExRepo.save(createDummyCredEx(p).setType(CredentialType.INDY).setRevoked(Boolean.TRUE));
        holderCredExRepo.save(createDummyCredEx(p).setType(CredentialType.INDY).setReferent("1"));
        holderCredExRepo.save(createDummyCredEx(p).setType(CredentialType.INDY).setReferent("2")
                .setRevoked(Boolean.FALSE));

        Assertions.assertEquals(2, holderCredExRepo.findNotRevoked().size());
    }

    @Test
    void testFindByTypeAndState() {
        Partner p = createRandomPartner();
        holderCredExRepo.save(createDummyCredEx(p));
        holderCredExRepo.save(createDummyCredEx(p).setState(CredentialExchangeState.CREDENTIAL_ISSUED));
        holderCredExRepo.save(createDummyCredEx(createRandomPartner()).setState(CredentialExchangeState.DONE));

        assertEquals(2, holderCredExRepo.findByRoleEqualsAndStateIn(
                CredentialExchangeRole.HOLDER,
                List.of(CredentialExchangeState.CREDENTIAL_ACKED, CredentialExchangeState.DONE)).size());

        assertEquals(1, holderCredExRepo.findByRoleEqualsAndStateIn(
                CredentialExchangeRole.HOLDER,
                List.of(CredentialExchangeState.CREDENTIAL_ISSUED)).size());
    }

    private static BPACredentialExchange createDummyCredEx(Partner partner) {
        return BPACredentialExchange
                .builder()
                .partner(partner)
                .threadId(UUID.randomUUID().toString())
                .state(CredentialExchangeState.CREDENTIAL_ACKED)
                .credentialExchangeId(UUID.randomUUID().toString())
                .isPublic(Boolean.FALSE)
                .role(CredentialExchangeRole.HOLDER)
                .build();
    }

    public Partner createRandomPartner() {
        return partnerRepo.save(Partner
                .builder()
                .ariesSupport(Boolean.TRUE)
                .did(UUID.randomUUID().toString())
                .connectionId(UUID.randomUUID().toString())
                .state(ConnectionState.ACTIVE)
                .trustPing(Boolean.TRUE)
                .build());
    }

}
