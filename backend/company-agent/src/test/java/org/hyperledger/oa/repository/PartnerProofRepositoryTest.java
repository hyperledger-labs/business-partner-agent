/*
  Copyright (c) 2020 - for information on the respective copyright owner
  see the NOTICE file and/or the repository at
  https://github.com/hyperledger-labs/business-partner-agent

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
import org.hyperledger.oa.model.PartnerProof;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
class PartnerProofRepositoryTest {

    @Inject
    PartnerProofRepository repo;

    @Test
    void testUpdateProof() {
        PartnerProof pp = PartnerProof
                .builder()
                .partnerId(UUID.randomUUID())
                .presentationExchangeId("pres-1")
                .build();
        pp = repo.save(pp);
        long uc = repo.updateReceivedProof(pp.getId(), Instant.now(), Boolean.TRUE, "valid",
                Map.of("testKey", "testValue"));
        assertEquals(1, uc);

        Optional<PartnerProof> updated = repo.findById(pp.getId());
        assertTrue(updated.isPresent());
        assertEquals("valid", updated.get().getState());
    }

}
