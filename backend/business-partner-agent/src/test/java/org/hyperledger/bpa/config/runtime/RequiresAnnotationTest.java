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
package org.hyperledger.bpa.config.runtime;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.hyperledger.bpa.client.LedgerExplorerClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.Optional;

@MicronautTest
public class RequiresAnnotationTest {

    @Inject
    Optional<LedgerExplorerClient> ledger;

    @Test
    void testNotPresent() {
        Assertions.assertTrue(ledger.isEmpty());
    }
}
