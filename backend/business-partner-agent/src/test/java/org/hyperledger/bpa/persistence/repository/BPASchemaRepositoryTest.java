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

@MicronautTest
public class BPASchemaRepositoryTest {

    @Inject
    BPASchemaRepository schemaRepository;

    @Test
    void testSetDefaultAttribute(){
        BPASchema schema1 = BPASchema.builder().schemaId("testSchema").schemaAttributeName("name").defaultAttributeName("name").type(CredentialType.INDY).build();
        schema1 = schemaRepository.save(schema1);
        BPASchema schema2 = schemaRepository.findById(schema1.getId()).orElseThrow();
        Assertions.assertEquals(schema1.getId(), schema2.getId());

        schemaRepository.updateDefaultAttributeName(schema1.getId(), null);

        BPASchema schema3 = schemaRepository.findById(schema1.getId()).orElseThrow();
        Assertions.assertNull(schema3.getDefaultAttributeName());
    }
}
