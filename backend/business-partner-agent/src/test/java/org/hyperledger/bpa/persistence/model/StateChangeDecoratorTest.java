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
package org.hyperledger.bpa.persistence.model;

import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.bpa.persistence.model.StateChangeDecorator;
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

        StateChangeDecorator.StateToTimestamp<ConnectionState> changes = StateChangeDecorator.StateToTimestamp
                .<ConnectionState>builder()
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
