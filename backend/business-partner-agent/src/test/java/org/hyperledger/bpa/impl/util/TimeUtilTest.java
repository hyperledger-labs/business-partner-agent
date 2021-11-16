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
package org.hyperledger.bpa.impl.util;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TimeUtilTest {

    @Test
    void testISONoMilli() {
        final String ts = "2020-07-17T14:32:34Z";
        Instant i = TimeUtil.fromISOInstant(ts);
        String time = TimeUtil.toISOInstantTruncated(i);
        assertEquals(ts, time);
    }

    @Test
    void testISOWithMilli() {
        final String ts = "2021-11-16T12:02:41.930486Z";
        final Instant i = TimeUtil.fromISOInstant(ts);
        assertNotNull(i);
        String time = TimeUtil.toISOInstant(i);
        assertEquals(ts, time);
    }
}
