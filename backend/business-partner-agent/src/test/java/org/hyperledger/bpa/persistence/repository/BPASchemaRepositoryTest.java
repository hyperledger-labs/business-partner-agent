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
