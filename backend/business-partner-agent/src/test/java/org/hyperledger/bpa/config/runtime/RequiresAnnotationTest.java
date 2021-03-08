package org.hyperledger.bpa.config.runtime;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.hyperledger.bpa.client.LedgerClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.Optional;

@MicronautTest
public class RequiresAnnotationTest {

    @Inject
    Optional<LedgerClient> ledger;

    @Test
    void testNotPresent() {
        Assertions.assertTrue(ledger.isEmpty());
    }
}
