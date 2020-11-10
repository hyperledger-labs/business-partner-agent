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
package org.hyperledger.oa.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CredentialTypeTest {

    @Test
    void testCredentialTypes() {
        assertEquals(CredentialType.BANK_ACCOUNT_CREDENTIAL, CredentialType.fromSchemaId("a:1:BankAccount:1"));
        assertEquals(CredentialType.BANK_ACCOUNT_CREDENTIAL, CredentialType.fromSchemaId("a:1:Bank_Account:1"));
        assertEquals(CredentialType.BANK_ACCOUNT_CREDENTIAL, CredentialType.fromSchemaId("a:1:bankaccount:1"));
        assertEquals(CredentialType.BANK_ACCOUNT_CREDENTIAL, CredentialType.fromSchemaId("a:1:bank_account:1"));
        assertEquals(CredentialType.OTHER, CredentialType.fromSchemaId("a:1:foo:1"));
        assertEquals(CredentialType.ORGANIZATIONAL_PROFILE_CREDENTIAL, CredentialType.fromSchemaId("a:1:Masterdata:1"));
        assertEquals(CredentialType.ORGANIZATIONAL_PROFILE_CREDENTIAL, CredentialType.fromSchemaId("a:1:masterdata:1"));
    }

}
