/**
 * Copyright (c) 2020 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/organizational-agent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hyperledger.oa.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.hyperledger.oa.model.Partner;
import org.junit.jupiter.api.Test;

import io.micronaut.test.annotation.MicronautTest;

@MicronautTest
class PartnerRepositoryTest {

    @Inject
    PartnerRepository repo;

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
    void testUpdateStateWithoutAffectingTimestamp() throws Exception {
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

}
