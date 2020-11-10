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
package org.hyperledger.oa.repository;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.hyperledger.oa.controller.api.partner.PartnerCredentialType;
import org.hyperledger.oa.impl.util.Converter;
import org.hyperledger.oa.model.Partner;
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
    PartnerRepository repo;

    @Inject
    Converter conv;

    @Test
    void testUpdateAlias() {
        Partner dbP = repo.save(Partner.builder().did("dummy").alias("alias").ariesSupport(Boolean.FALSE).build());
        assertEquals("alias", dbP.getAlias());

        int uCount = repo.updateAlias(UUID.fromString(dbP.getId().toString()), "newAlias");
        assertEquals(1, uCount);

        Optional<Partner> updatedP = repo.findByDid("dummy");
        assertTrue(updatedP.isPresent());
        assertEquals("newAlias", updatedP.get().getAlias());

        int nonExistingP = repo.updateAlias(UUID.randomUUID(), "dummy");
        assertEquals(0, nonExistingP);
    }

    @Test
    void testUpdateStateWithoutAffectingTimestamp() {
        final String connectionId = "id-123";
        repo.save(Partner
                .builder()
                .ariesSupport(Boolean.FALSE)
                .did("did:fit:123")
                .connectionId(connectionId)
                .build());

        Optional<Partner> reload = repo.findByConnectionId(connectionId);
        assertTrue(reload.isPresent());

        repo.updateStateByConnectionId(connectionId, "custom");

        Optional<Partner> mod = repo.findByConnectionId(connectionId);

        assertTrue(mod.isPresent());
        assertEquals(0, reload.get().getUpdatedAt().compareTo(mod.get().getUpdatedAt()));
        assertEquals("custom", mod.get().getState());

        repo.updateStateByConnectionId(connectionId, "custom2");

        mod = repo.findByConnectionId(connectionId);

        assertTrue(mod.isPresent());
        assertEquals(0, reload.get().getUpdatedAt().compareTo(mod.get().getUpdatedAt()));
        assertEquals("custom2", mod.get().getState());
    }

    @Test
    void testUpdateStateShouldNotChangeStateOfOtherConnections() {
        final String p1CId = "id-1";
        repo.save(Partner
                .builder()
                .ariesSupport(Boolean.FALSE)
                .did("did:fit:123")
                .connectionId(p1CId)
                .state("state-1")
                .build());

        final String p2Cid = "id-2";
        repo.save(Partner
                .builder()
                .ariesSupport(Boolean.FALSE)
                .did("did:bit:321")
                .connectionId(p2Cid)
                .state("state-2")
                .build());

        final String p3Cid = "id-3";
        repo.save(Partner
                .builder()
                .ariesSupport(Boolean.FALSE)
                .did("did:foo:321")
                .connectionId(p3Cid)
                .build());

        repo.updateStateByConnectionId(p1CId, "custom");

        Optional<Partner> p1 = repo.findByConnectionId(p1CId);
        assertTrue(p1.isPresent());
        assertEquals("custom", p1.get().getState());

        Optional<Partner> p2 = repo.findByConnectionId(p2Cid);
        assertTrue(p2.isPresent());
        assertEquals("state-2", p2.get().getState());

        Optional<Partner> p3 = repo.findByConnectionId(p3Cid);
        assertTrue(p3.isPresent());
        assertNull(p3.get().getState());
    }

    @Test
    void testUpdateLastSeen() {
        final String p1CId = "id-1";
        repo.save(Partner
                .builder()
                .ariesSupport(Boolean.FALSE)
                .did("did:fit:123")
                .connectionId(p1CId)
                .build());

        final String p2Cid = "id-2";
        repo.save(Partner
                .builder()
                .ariesSupport(Boolean.FALSE)
                .did("did:bit:321")
                .connectionId(p2Cid)
                .build());

        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        repo.updateStateAndLastSeenByConnectionId(p1CId, "state-1", now);

        Optional<Partner> p1 = repo.findByConnectionId(p1CId);
        assertTrue(p1.isPresent());
        assertEquals("state-1", p1.get().getState());
        assertEquals(now, p1.get().getLastSeen());

        Optional<Partner> p2 = repo.findByConnectionId(p2Cid);
        assertTrue(p2.isPresent());
        assertNull(p2.get().getLastSeen());
        assertNull(p2.get().getState());
    }

    @Test
    void testFinBySupportedCredentials() {
        createPartnerWithCredentialType(571);
        createPartnerWithCredentialType(573);
        createPartnerWithCredentialType(573);
        createPartnerWithCredentialType(575);
        createPartnerWithCredentialType(575);
        createPartnerWithCredentialType(575);

        List<Partner> found = repo.findBySupportedCredential("571");
        assertEquals(1, found.size());

        found = repo.findBySupportedCredential("573");
        assertEquals(2, found.size());

        found = repo.findBySupportedCredential("575");
        assertEquals(3, found.size());
    }

    private void createPartnerWithCredentialType(int seqno) {
        final String did = RandomStringUtils.random(16);
        repo.save(Partner
                .builder()
                .ariesSupport(Boolean.TRUE)
                .did(did)
                .connectionId(did)
                .build());

        final List<PartnerCredentialType> sc = List.of(
                PartnerCredentialType.fromCredDefId("M6Mbe3qx7vB4wpZF4sBRj1:3:CL:" + seqno + ":ba"),
                PartnerCredentialType.fromCredDefId("M6Mbe3qx7vB4wpZF4sBRj2:3:CL:" + ++seqno + ":bank_account"));
        Map<String, Object> map = conv.toMap(new Foo(sc));
        repo.updateByDid(did, map);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Foo {
        List<PartnerCredentialType> wrapped;
    }

}
