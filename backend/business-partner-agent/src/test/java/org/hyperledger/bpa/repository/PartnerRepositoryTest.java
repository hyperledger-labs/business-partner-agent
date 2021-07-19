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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.bpa.controller.api.partner.PartnerCredentialType;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.model.Partner;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
class PartnerRepositoryTest {

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    TagRepository tagRepo;

    @Inject
    Converter conv;

    @Test
    void testUpdateAlias() {
        Partner dbP = partnerRepo
                .save(Partner.builder().did("dummy").alias("alias").ariesSupport(Boolean.FALSE).build());
        assertEquals("alias", dbP.getAlias());

        int uCount = partnerRepo.updateAlias(UUID.fromString(dbP.getId().toString()), "newAlias");
        assertEquals(1, uCount);

        Optional<Partner> updatedP = partnerRepo.findByDid("dummy");
        assertTrue(updatedP.isPresent());
        assertEquals("newAlias", updatedP.get().getAlias());

        int nonExistingP = partnerRepo.updateAlias(UUID.randomUUID(), "dummy");
        assertEquals(0, nonExistingP);
    }

    @Test
    void testUpdateStateWithoutAffectingTimestamp() {
        final String connectionId = "id-123";
        partnerRepo.save(Partner
                .builder()
                .ariesSupport(Boolean.FALSE)
                .did("did:fit:123")
                .connectionId(connectionId)
                .build());

        Optional<Partner> reload = partnerRepo.findByConnectionId(connectionId);
        assertTrue(reload.isPresent());

        partnerRepo.updateStateByConnectionId(connectionId, ConnectionState.PING_RESPONSE);

        Optional<Partner> mod = partnerRepo.findByConnectionId(connectionId);

        assertTrue(mod.isPresent());
        assertEquals(0, reload.get().getUpdatedAt().compareTo(mod.get().getUpdatedAt()));
        assertEquals(ConnectionState.PING_RESPONSE, mod.get().getState());

        partnerRepo.updateStateByConnectionId(connectionId, ConnectionState.PING_NO_RESPONSE);

        mod = partnerRepo.findByConnectionId(connectionId);

        assertTrue(mod.isPresent());
        assertEquals(0, reload.get().getUpdatedAt().compareTo(mod.get().getUpdatedAt()));
        assertEquals(ConnectionState.PING_NO_RESPONSE, mod.get().getState());
    }

    @Test
    void testUpdateStateShouldNotChangeStateOfOtherConnections() {
        final String p1CId = "id-1";
        partnerRepo.save(Partner
                .builder()
                .ariesSupport(Boolean.FALSE)
                .did("did:fit:123")
                .connectionId(p1CId)
                .state(ConnectionState.ACTIVE)
                .build());

        final String p2Cid = "id-2";
        partnerRepo.save(Partner
                .builder()
                .ariesSupport(Boolean.FALSE)
                .did("did:bit:321")
                .connectionId(p2Cid)
                .state(ConnectionState.INIT)
                .build());

        final String p3Cid = "id-3";
        partnerRepo.save(Partner
                .builder()
                .ariesSupport(Boolean.FALSE)
                .did("did:foo:321")
                .connectionId(p3Cid)
                .build());

        partnerRepo.updateStateByConnectionId(p1CId, ConnectionState.PING_NO_RESPONSE);

        Optional<Partner> p1 = partnerRepo.findByConnectionId(p1CId);
        assertTrue(p1.isPresent());
        assertEquals(ConnectionState.PING_NO_RESPONSE, p1.get().getState());

        Optional<Partner> p2 = partnerRepo.findByConnectionId(p2Cid);
        assertTrue(p2.isPresent());
        assertEquals(ConnectionState.INIT, p2.get().getState());

        Optional<Partner> p3 = partnerRepo.findByConnectionId(p3Cid);
        assertTrue(p3.isPresent());
        assertNull(p3.get().getState());
        assertNull(p3.get().getState());
    }

    @Test
    void testUpdateLastSeen() {
        final String p1CId = "id-1";
        partnerRepo.save(Partner
                .builder()
                .ariesSupport(Boolean.FALSE)
                .did("did:fit:123")
                .connectionId(p1CId)
                .build());

        final String p2Cid = "id-2";
        partnerRepo.save(Partner
                .builder()
                .ariesSupport(Boolean.FALSE)
                .did("did:bit:321")
                .connectionId(p2Cid)
                .build());

        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        partnerRepo.updateStateAndLastSeenByConnectionId(p1CId, ConnectionState.PING_RESPONSE, now);

        Optional<Partner> p1 = partnerRepo.findByConnectionId(p1CId);
        assertTrue(p1.isPresent());
        assertEquals(ConnectionState.PING_RESPONSE, p1.get().getState());
        assertEquals(now, p1.get().getLastSeen());

        Optional<Partner> p2 = partnerRepo.findByConnectionId(p2Cid);
        assertTrue(p2.isPresent());
        assertNull(p2.get().getLastSeen());
        assertNull(p2.get().getState());
    }

    @Test
    void testFindBySupportedCredentials() {
        createPartnerWithCredentialType(571);
        createPartnerWithCredentialType(573);
        createPartnerWithCredentialType(573);
        createPartnerWithCredentialType(575);
        createPartnerWithCredentialType(575);
        createPartnerWithCredentialType(575);

        List<Partner> found = partnerRepo.findBySupportedCredential("571");
        assertEquals(1, found.size());

        found = partnerRepo.findBySupportedCredential("573");
        assertEquals(2, found.size());

        found = partnerRepo.findBySupportedCredential("575");
        assertEquals(3, found.size());
    }

    @Test
    void testFindByDidIn() {
        partnerRepo.save(Partner.builder().ariesSupport(Boolean.TRUE).did("did1").connectionId("con1").build());
        partnerRepo.save(Partner.builder().ariesSupport(Boolean.TRUE).did("did2").connectionId("con2").build());
        partnerRepo.save(Partner.builder().ariesSupport(Boolean.TRUE).did("did3").connectionId("con3").build());

        List<Partner> partner = partnerRepo.findByDidIn(List.of("did1", "did2"));
        assertEquals(2, partner.size());
    }

    @Test
    void testFindByDidInNoResult() {
        List<Partner> partner = partnerRepo.findByDidIn(List.of());
        assertEquals(0, partner.size());
    }

    private void createPartnerWithCredentialType(int seqno) {
        final String did = RandomStringUtils.random(16);
        partnerRepo.save(Partner
                .builder()
                .ariesSupport(Boolean.TRUE)
                .did(did)
                .connectionId(did)
                .build());

        final List<PartnerCredentialType> sc = List.of(
                PartnerCredentialType.fromCredDefId("M6Mbe3qx7vB4wpZF4sBRj1:3:CL:" + seqno + ":ba"),
                PartnerCredentialType.fromCredDefId("M6Mbe3qx7vB4wpZF4sBRj2:3:CL:" + ++seqno + ":bank_account"));
        Map<String, Object> map = conv.toMap(new Foo(sc));
        partnerRepo.updateByDid(did, map);
    }

    @Test
    void testUpdateVerifiablePresentation() {
        Partner partner = partnerRepo.save(Partner
                .builder()
                .ariesSupport(Boolean.TRUE)
                .did("did:indy:private")
                .connectionId("con1")
                .build());

        partnerRepo.updateVerifiablePresentation(partner.getId(), Map.of(), Boolean.TRUE, "alias", "did:indy:public");

        Optional<Partner> reload = partnerRepo.findById(partner.getId());
        assertTrue(reload.isPresent());
        assertEquals("alias", reload.get().getLabel());
        assertEquals("did:indy:public", reload.get().getDid());
        assertEquals(Boolean.TRUE, reload.get().getValid());
    }

    @Test
    void testFindByStatesAndTrustPing() {
        partnerRepo.save(Partner.builder().ariesSupport(Boolean.TRUE).did("did1").connectionId("con1")
                .state(ConnectionState.ACTIVE).trustPing(Boolean.TRUE).build());
        partnerRepo.save(Partner.builder().ariesSupport(Boolean.TRUE).did("did2").connectionId("con2")
                .state(ConnectionState.ACTIVE).trustPing(Boolean.FALSE).build());
        partnerRepo.save(Partner.builder().ariesSupport(Boolean.TRUE).did("did3").connectionId("con3")
                .state(ConnectionState.COMPLETED).trustPing(Boolean.TRUE).build());
        partnerRepo.save(Partner.builder().ariesSupport(Boolean.TRUE).did("did4").connectionId("con4")
                .state(ConnectionState.ACTIVE).trustPing(Boolean.FALSE).build());
        partnerRepo.save(Partner.builder().ariesSupport(Boolean.TRUE).did("did5").connectionId("con5")
                .state(ConnectionState.ACTIVE).trustPing(null).build());

        List<Partner> pingTrue = partnerRepo
                .findByStateInAndTrustPingTrueAndAriesSupportTrue(List.of(ConnectionState.ACTIVE));
        assertEquals(1, pingTrue.size());
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Foo {
        List<PartnerCredentialType> wrapped;
    }

}
