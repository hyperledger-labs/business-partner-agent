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
package org.hyperledger.oa.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;

import org.hyperledger.oa.BaseTest;
import org.hyperledger.oa.api.DidDocAPI;
import org.hyperledger.oa.client.api.DidDocument;
import org.junit.jupiter.api.Test;

public class PartnerFlowTest extends BaseTest {

    @Test
    void testResolveEndpoint() throws Exception {
        DidDocument didDoc = loadAndConvertTo("files/didEvan.json", DidDocument.class);

        Optional<Map<String, String>> ep = PartnerManager.filterServices(didDoc.getDidDocument());
        assertTrue(ep.isPresent());
        assertEquals(1, ep.get().size());
        assertTrue(ep.get().get("profile").startsWith("https://test.test.com"));
    }

    @Test
    void testResolveEndpointEmpty() {
        Optional<Map<String, String>> ep = PartnerManager.filterServices(new DidDocAPI());
        assertFalse(ep.isEmpty());
        assertEquals(0, ep.get().size());
    }
}
