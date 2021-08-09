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
package org.hyperledger.bpa.repository;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;
import org.hyperledger.aries.config.GsonConfig;
import org.hyperledger.bpa.BaseTest;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.model.MyCredential;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@MicronautTest
class MyCredentialRepositoryTest extends BaseTest {

    @Inject
    MyCredentialRepository repo;

    @Inject
    Converter conv;

    @Test
    void testSaveCredential() {
        String schemaId = "M6Mbe3qx7vB4wpZF4sBRjt:2:bank_account:1.0";
        String credDefId = "VoSfM3eGaPxduty34ySygw:3:CL:571:sparta_bank";

        final String json = loader.load("files/credentialExchange.json");
        final V1CredentialExchange ex = GsonConfig.defaultConfig().fromJson(json, V1CredentialExchange.class);

        MyCredential cred = MyCredential
                .builder()
                .type(CredentialType.SCHEMA_BASED)
                .isPublic(Boolean.TRUE)
                .connectionId("1")
                .state(CredentialExchangeState.CREDENTIAL_ACKED)
                .threadId("1")
                .credential(conv.toMap(ex.getCredential()))
                .build();
        final MyCredential saved = repo.save(cred);

        final List<MyCredential> credLoaded = repo.findBySchemaIdAndCredentialDefinitionId(schemaId, credDefId);
        assertFalse(credLoaded.isEmpty());
        assertEquals(1, credLoaded.size());
        assertEquals(saved.getId(), credLoaded.get(0).getId());
    }

    @Test
    void testUpdateByConnectionId() {
        String connectionId = UUID.randomUUID().toString();
        repo.save(createDummyCredential(connectionId));
        repo.save(createDummyCredential(connectionId));
        repo.save(createDummyCredential("other"));

        final List<MyCredential> findByConnectionId = repo.findByConnectionId(connectionId);
        assertEquals(2, findByConnectionId.size());

        Number updated = repo.updateByConnectionId(connectionId, null);
        assertEquals(2, updated.intValue());

        updated = repo.updateByConnectionId("some", null);
        assertEquals(0, updated.intValue());

        updated = repo.updateByConnectionId("other", "something", "My Bank");
        assertEquals(1, updated.intValue());

        final List<MyCredential> cred = repo.findByConnectionId("something");
        assertEquals(1, cred.size());
        assertEquals("My Bank", cred.get(0).getIssuer());
    }

    @Test
    void testCountByState() {
        String connectionId = UUID.randomUUID().toString();
        repo.save(createDummyCredential(connectionId));
        repo.save(createDummyCredential(connectionId).setState(CredentialExchangeState.CREDENTIAL_ISSUED));
        repo.save(createDummyCredential("other"));

        assertEquals(2, repo.countByStateEquals(CredentialExchangeState.CREDENTIAL_ACKED));
    }

    @Test
    void testFindNotRevoked() {
        String connectionId = UUID.randomUUID().toString();
        repo.save(createDummyCredential(connectionId));
        repo.save(createDummyCredential(connectionId).setType(CredentialType.ORGANIZATIONAL_PROFILE_CREDENTIAL));
        repo.save(createDummyCredential(connectionId).setType(CredentialType.SCHEMA_BASED));
        repo.save(createDummyCredential(connectionId).setType(CredentialType.SCHEMA_BASED).setRevoked(Boolean.FALSE));
        repo.save(createDummyCredential(connectionId).setType(CredentialType.SCHEMA_BASED).setRevoked(Boolean.TRUE));
        repo.save(createDummyCredential(connectionId).setType(CredentialType.SCHEMA_BASED).setReferent("1"));
        repo.save(createDummyCredential(connectionId).setType(CredentialType.SCHEMA_BASED).setReferent("2")
                .setRevoked(Boolean.FALSE));

        Assertions.assertEquals(2, repo.findNotRevoked().size());
    }

    private static MyCredential createDummyCredential(String connectionId) {
        return MyCredential
                .builder()
                .connectionId(connectionId)
                .threadId(UUID.randomUUID().toString())
                .state(CredentialExchangeState.CREDENTIAL_ACKED)
                .isPublic(Boolean.FALSE)
                .build();
    }

}
