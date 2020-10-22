package org.hyperledger.oa.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

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
