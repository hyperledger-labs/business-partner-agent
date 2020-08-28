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
package org.hyperledger.oa.repository;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import javax.inject.Inject;

import org.hyperledger.aries.api.credential.CredentialExchange;
import org.hyperledger.aries.config.GsonConfig;
import org.hyperledger.oa.BaseTest;
import org.hyperledger.oa.api.CredentialType;
import org.hyperledger.oa.impl.util.Converter;
import org.hyperledger.oa.model.MyCredential;
import org.junit.jupiter.api.Test;

import io.micronaut.test.annotation.MicronautTest;

@MicronautTest
class MyCredentialRepositoryTest extends BaseTest {

    @Inject
    private MyCredentialRepository repo;

    @Inject
    private Converter conv;

    @Test
    void test() throws Exception {
        String schemaId = "M6Mbe3qx7vB4wpZF4sBRjt:2:bank_account:1.0";
        String credDefId = "VoSfM3eGaPxduty34ySygw:3:CL:571:sparta_bank";

        final String json = loader.load("files/credentialExchange.json");
        final CredentialExchange ex = GsonConfig.defaultConfig().fromJson(json, CredentialExchange.class);

        MyCredential cred = MyCredential
                .builder()
                .type(CredentialType.BANK_ACCOUNT_CREDENTIAL)
                .isPublic(Boolean.TRUE)
                .connectionId("1")
                .state("active")
                .threadId("1")
                .credential(conv.toMap(ex.getCredential()))
                .build();
        final MyCredential saved = repo.save(cred);

        final List<MyCredential> credLoaded = repo.findBySchemaIdAndCredentialDefinitionId(schemaId, credDefId);
        assertFalse(credLoaded.isEmpty());
        assertEquals(1, credLoaded.size());
        assertEquals(saved.getId(), credLoaded.get(0).getId());
    }

}
