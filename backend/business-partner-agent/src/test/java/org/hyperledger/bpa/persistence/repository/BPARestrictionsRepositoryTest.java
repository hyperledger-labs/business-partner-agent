/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
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
package org.hyperledger.bpa.persistence.repository;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.persistence.model.BPARestrictions;
import org.hyperledger.bpa.persistence.model.BPASchema;
import org.hyperledger.bpa.persistence.repository.BPARestrictionsRepository;
import org.hyperledger.bpa.persistence.repository.BPASchemaRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

@MicronautTest
public class BPARestrictionsRepositoryTest {

    @Inject
    BPARestrictionsRepository repo;

    @Inject
    BPASchemaRepository schemaRepo;

    @Test
    void testExistsBySchemaIdAndIssuerDid() {
        String schema1Id = "schema-1";
        String schema2Id = "schema-2";
        String issuer1Did = "issuer-1";
        String issuer2Did = "issuer-2";

        BPASchema schema1 = schemaRepo.save(BPASchema
                .builder()
                .schemaId(schema1Id)
                .schemaAttributeNames(List.of("name"))
                .seqNo(1)
                .type(CredentialType.INDY)
                .build());

        BPASchema schema2 = schemaRepo.save(BPASchema
                .builder()
                .schemaId(schema2Id)
                .schemaAttributeNames(List.of("other"))
                .seqNo(1)
                .type(CredentialType.INDY)
                .build());

        BPARestrictions restriction = repo.save(BPARestrictions
                .builder()
                .schema(schema1)
                .issuerDid(issuer1Did)
                .build());

        repo.save(BPARestrictions
                .builder()
                .schema(schema1)
                .issuerDid(issuer2Did)
                .build());

        BPARestrictions exists = repo.findBySchemaIdAndIssuerDid(schema1.getId(), issuer1Did).orElseThrow();
        Assertions.assertEquals(restriction.getId(), exists.getId());

        Optional<BPARestrictions> empty = repo.findBySchemaIdAndIssuerDid(schema1.getId(), "issuer-3");
        Assertions.assertTrue(empty.isEmpty());

        empty = repo.findBySchemaIdAndIssuerDid(schema2.getId(), issuer2Did);
        Assertions.assertTrue(empty.isEmpty());
    }
}
