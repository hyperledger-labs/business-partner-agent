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
package org.hyperledger.bpa.persistence.repository;

import io.micronaut.data.model.Pageable;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.aries.api.credentials.CredentialAttributes;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;
import org.hyperledger.aries.config.GsonConfig;
import org.hyperledger.bpa.BaseTest;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.persistence.model.BPACredentialExchange;
import org.hyperledger.bpa.persistence.model.Partner;
import org.hyperledger.bpa.persistence.model.converter.ExchangePayload;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
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
                .indyCredential(ex.getCredential())
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

        assertEquals(2, holderCredExRepo.countByPartnerId(p.getId()));

        Number updated = holderCredExRepo.setPartnerIdToNull(p.getId());
        assertEquals(2, updated.intValue());

        updated = holderCredExRepo.setPartnerIdToNull(UUID.randomUUID());
        assertEquals(0, updated.intValue());
        ex1 = holderCredExRepo.findById(ex1.getId()).orElseThrow();
        assertNull(ex1.getPartner());

        assertEquals(1, holderCredExRepo.countByPartnerId(other.getId()));
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
        holderCredExRepo.save(createDummyCredEx(p).setType(CredentialType.INDY).setReferent("2").setRevRegId("2:2"));
        holderCredExRepo.save(createDummyCredEx(p).setType(CredentialType.INDY).setReferent("3").setRevRegId("3:3")
                .setRevoked(Boolean.FALSE));

        Assertions.assertEquals(2, holderCredExRepo.findNotRevoked(Pageable.UNPAGED).getNumberOfElements());
    }

    @Test
    void testFindByTypeAndState() {
        Partner p = createRandomPartner();
        holderCredExRepo.save(createDummyCredEx(p));
        holderCredExRepo.save(createDummyCredEx(p).setState(CredentialExchangeState.CREDENTIAL_ISSUED));
        holderCredExRepo.save(createDummyCredEx(createRandomPartner()).setState(CredentialExchangeState.DONE));

        assertEquals(2, holderCredExRepo.findByRoleEqualsAndStateInAndTypeIn(
                CredentialExchangeRole.HOLDER,
                List.of(CredentialExchangeState.CREDENTIAL_ACKED, CredentialExchangeState.DONE),
                List.of(CredentialType.INDY), Pageable.unpaged()).getTotalSize());

        assertEquals(1, holderCredExRepo.findByRoleEqualsAndStateInAndTypeIn(
                CredentialExchangeRole.HOLDER,
                List.of(CredentialExchangeState.CREDENTIAL_ISSUED),
                List.of(CredentialType.INDY), Pageable.unpaged()).getTotalSize());

        assertEquals(0, holderCredExRepo.findByRoleEqualsAndStateInAndTypeIn(
                CredentialExchangeRole.HOLDER,
                List.of(CredentialExchangeState.CREDENTIAL_ISSUED),
                List.of(CredentialType.JSON_LD), Pageable.unpaged()).getTotalSize());
    }

    @Test
    void testUpdateCredentialOffer() {
        Partner p = createRandomPartner();
        BPACredentialExchange saved = holderCredExRepo.save(createDummyCredEx(p));
        saved.pushStates(CredentialExchangeState.OFFER_RECEIVED);
        holderCredExRepo.updateOnCredentialOfferEvent(saved.getId(), saved.getState(), saved.getStateToTimestamp(),
                ExchangePayload
                        .indy(V1CredentialExchange.CredentialProposalDict.CredentialProposal.builder()
                                .attributes(CredentialAttributes.from(Map.of("attr1", "value1")))
                                .build()));
        BPACredentialExchange exchange = holderCredExRepo.findById(saved.getId()).orElseThrow();
        assertNotNull(exchange.getCredentialOffer());
        assertTrue(exchange.getCredentialOffer().typeIsIndy());
        assertEquals("value1", exchange.getCredentialOffer().getIndy().getAttributes().get(0).getValue());
    }

    @Test
    void testSetPartnerIdToNull() {
        Partner p = createRandomPartner();
        Partner p2 = createRandomPartner();
        holderCredExRepo.save(createDummyCredEx(p));
        BPACredentialExchange done = createDummyCredEx(p).setState(CredentialExchangeState.DONE);
        holderCredExRepo.save(done);
        holderCredExRepo.save(createDummyCredEx(p).setState(CredentialExchangeState.CREDENTIAL_RECEIVED));
        holderCredExRepo.save(createDummyCredEx(p).setState(CredentialExchangeState.PROBLEM));

        Assertions.assertEquals(4, holderCredExRepo.count());

        holderCredExRepo.setPartnerIdToNull(p.getId());
        partnerRepo.deleteByPartnerId(p.getId());

        Assertions.assertEquals(2, holderCredExRepo.count());
        Assertions.assertNull(holderCredExRepo.findById(done.getId()).orElseThrow().getPartner());

        partnerRepo.deleteByPartnerId(p2.getId());
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
                .type(CredentialType.INDY)
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
