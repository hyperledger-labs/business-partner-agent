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
import org.hyperledger.bpa.persistence.model.BPASchema;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import java.util.List;

@MicronautTest
public class BPASchemaRepositoryTest {

    @Inject
    BPASchemaRepository schemaRepo;

    @Test
    void testCountSchemaTypes() {
        BPASchema schema1 = schemaRepo.save(BPASchema
                .builder()
                .schemaId("1")
                .schemaAttributeNames(List.of("name"))
                .seqNo(1)
                .type(CredentialType.INDY)
                .build());

        BPASchema schema2 = schemaRepo.save(BPASchema
                .builder()
                .schemaId("2")
                .schemaAttributeNames(List.of("street"))
                .seqNo(1)
                .type(CredentialType.INDY)
                .build());

        BPASchema schema3 = schemaRepo.save(BPASchema
                .builder()
                .schemaId("3")
                .schemaAttributeNames(List.of("city"))
                .type(CredentialType.JSON_LD)
                .build());

        BPASchema schema4 = schemaRepo.save(BPASchema
                .builder()
                .schemaId("4")
                .schemaAttributeNames(List.of("country"))
                .type(CredentialType.JSON_LD)
                .build());

        Assertions.assertEquals(1, schemaRepo.countSchemaTypes(List.of(schema1.getId(), schema2.getId())));
        Assertions.assertEquals(1, schemaRepo.countSchemaTypes(List.of(schema3.getId(), schema4.getId())));
        Assertions.assertEquals(2, schemaRepo.countSchemaTypes(List.of(schema1.getId(), schema3.getId())));
        Assertions.assertEquals(2, schemaRepo
                .countSchemaTypes(List.of(schema1.getId(), schema2.getId(), schema3.getId(), schema4.getId())));
    }

    @Test
    void testSetDefaultAttribute(){
        BPASchema schema1 = BPASchema.builder()
                .schemaId("testSchema").schemaAttributeName("name")
                .defaultAttributeName("name").type(CredentialType.INDY)
                .build();
        schema1 = schemaRepo.save(schema1);
        BPASchema schema2 = schemaRepo.findById(schema1.getId()).orElseThrow();
        Assertions.assertEquals(schema1.getId(), schema2.getId());

        schemaRepo.updateDefaultAttributeName(schema1.getId(), null);

        BPASchema schema3 = schemaRepo.findById(schema1.getId()).orElseThrow();
        Assertions.assertNull(schema3.getDefaultAttributeName());
    }
}
