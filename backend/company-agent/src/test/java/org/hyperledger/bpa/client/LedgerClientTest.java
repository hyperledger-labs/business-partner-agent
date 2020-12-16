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
package org.hyperledger.bpa.client;

import org.hyperledger.bpa.BaseTest;
import org.hyperledger.bpa.controller.api.partner.PartnerCredentialType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

class LedgerClientTest extends BaseTest {

    @Test
    @Disabled
    // TODO migrate to okhttp mock
    void test() {
        LedgerClient c = new LedgerClient();
        c.setUrl("https://indy-test.bosch-digital.de");
        c.setMapper(mapper);

        final Optional<List<PartnerCredentialType>> credDefIds = c.queryCredentialDefinitions(
                "CHysca6fY8n8ytCDLAJGZj");
        System.err.println(credDefIds.get());
    }
}
