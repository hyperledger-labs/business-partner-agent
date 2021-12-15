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
import jakarta.inject.Inject;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.model.BPACredentialDefinition;
import org.hyperledger.bpa.model.BPASchema;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

@MicronautTest
public class BPACredentialDefinitionRepositoryTest {

    @Inject
    BPACredentialDefinitionRepository credRepo;

    @Inject
    BPASchemaRepository schemaRepo;

    @Test
    void testJoinCredDefOnSchema() {
        String schemaId = "my-schema";
        BPASchema schema = schemaRepo.save(BPASchema
                .builder()
                .schemaId(schemaId)
                .schemaAttributeNames(Set.of("name"))
                .seqNo(1)
                .type(CredentialType.INDY)
                .build());
        credRepo.save(BPACredentialDefinition
                .builder()
                .credentialDefinitionId("my-cred-def-01")
                .schema(schema)
                .tag("bob-issuer")
                .revocationRegistrySize(200)
                .build());
        credRepo.save(BPACredentialDefinition
                .builder()
                .credentialDefinitionId("my-cred-def-02")
                .schema(schema)
                .tag("oscar-issuer")
                .revocationRegistrySize(200)
                .build());

        List<BPACredentialDefinition> bySchemaId = credRepo.findBySchemaId(schemaId);
        Assertions.assertEquals(2, bySchemaId.size());
    }
}
