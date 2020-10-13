package org.hyperledger.oa.repository;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.hyperledger.oa.model.PartnerProof;
import org.junit.jupiter.api.Test;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

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
