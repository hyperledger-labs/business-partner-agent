package org.hyperledger.bpa.model;

import org.hyperledger.aries.api.connection.ConnectionState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StateChangeDecoratorTest {

    @Test
    void testFindLatestStateChange() {
        Instant i1 = Instant.ofEpochMilli(1633426742074L);
        Instant i2 = i1.plusMillis(33);
        Instant i3 = i2.plusMillis(32);

        StateChangeDecorator.StateToTimestamp<ConnectionState> changes = StateChangeDecorator
                .StateToTimestamp.<ConnectionState>builder()
                .stateToTimestamp(Map.of(
                        ConnectionState.RESPONSE, i2,
                        ConnectionState.ACTIVE, i3,
                        ConnectionState.REQUEST, i1))
                .build();

        Map.Entry<ConnectionState, Instant> latestTimestamp = changes.findLatestEntry();
        Assertions.assertNotNull(latestTimestamp);
        Assertions.assertEquals(ConnectionState.ACTIVE, latestTimestamp.getKey());

        List<ConnectionState> states = new ArrayList<>(changes.toApi().keySet());
        Assertions.assertEquals(ConnectionState.REQUEST, states.get(0));
        Assertions.assertEquals(ConnectionState.RESPONSE, states.get(1));
        Assertions.assertEquals(ConnectionState.ACTIVE, states.get(2));
    }
}
