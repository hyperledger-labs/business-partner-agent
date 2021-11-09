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
package org.hyperledger.bpa.util;

import org.hyperledger.aries.api.credentials.CredentialAttributes;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class CryptoUtilTest {

    @Test
    void testMatch() {
        V1CredentialExchange.CredentialProposalDict.CredentialProposal p1 = create("test");
        V1CredentialExchange.CredentialProposalDict.CredentialProposal p2 = create("test");
        Assertions.assertTrue(CryptoUtil.hashCompare(p1, p2));
    }

    @Test
    void testNoMatch() {
        V1CredentialExchange.CredentialProposalDict.CredentialProposal p1 = create("test");
        V1CredentialExchange.CredentialProposalDict.CredentialProposal p2 = create("other");
        Assertions.assertFalse(CryptoUtil.hashCompare(p1, p2));
    }

    private V1CredentialExchange.CredentialProposalDict.CredentialProposal create(String value) {
        return V1CredentialExchange.CredentialProposalDict.CredentialProposal
                .builder()
                .attributes(CredentialAttributes.from(Map.of("name", value)))
                .build();
    }
}
