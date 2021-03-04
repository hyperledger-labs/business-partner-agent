package org.hyperledger.bpa.impl.aries.config;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.creddef.CredentialDefinition;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.controller.api.admin.CredentialDefinitionConfiguration;
import org.hyperledger.bpa.model.BPASchema;
import org.hyperledger.bpa.repository.BPASchemaRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.inject.Inject;
import java.util.Optional;
import java.util.UUID;

@MicronautTest
public class CredentialDefinitionManagerTest {

    private final AriesClient ac = Mockito.mock(AriesClient.class);

    @Inject
    CredentialDefinitionManager mgmt;

    @Inject
    BPASchemaRepository schemaRepo;

    @BeforeEach
    void setup() {
        mgmt.setAc(ac);
    }

    @Test
    void testAddConfigurationNoSchema() {
        Assertions.assertThrows(WrongApiUsageException.class,
                () -> mgmt.addCredentialDefinition(UUID.randomUUID(), "123", null));
    }

    @Test
    void testNoCredDefOnLedger() {
        String schemaId = "schemaId";
        BPASchema dbSchema = schemaRepo.save(BPASchema.builder()
                .schemaId(schemaId)
                .seqNo(571)
                .build());
        Optional<CredentialDefinitionConfiguration> credDefId = mgmt
                .addCredentialDefinition(dbSchema.getId(), "credDefId", null);
        Assertions.assertTrue(credDefId.isEmpty());
    }

    @Test
    void testSeqNoMissMatch() throws Exception {
        CredentialDefinition definition = new CredentialDefinition();
        definition.setSchemaId("11");
        Mockito.when(ac.credentialDefinitionsGetById(Mockito.anyString()))
                .thenReturn(Optional.of(definition));
        BPASchema dbSchema = schemaRepo.save(BPASchema.builder()
                .schemaId("1234")
                .seqNo(571)
                .build());
        Optional<CredentialDefinitionConfiguration> credDefId = mgmt
                .addCredentialDefinition(dbSchema.getId(), "credDefId", null);
        Assertions.assertTrue(credDefId.isEmpty());
    }

    @Test
    void testAddCredentialConfigSuccess() throws Exception {
        CredentialDefinition definition = new CredentialDefinition();
        definition.setSchemaId("571");
        Mockito.when(ac.credentialDefinitionsGetById(Mockito.anyString()))
                .thenReturn(Optional.of(definition));
        BPASchema dbSchema = schemaRepo.save(BPASchema.builder()
                .schemaId("1234")
                .seqNo(571)
                .build());
        Optional<CredentialDefinitionConfiguration> credDefId = mgmt
                .addCredentialDefinition(dbSchema.getId(), "credDefId", null);
        Assertions.assertTrue(credDefId.isPresent());

        Optional<BPASchema> reloaded = schemaRepo.findBySchemaId(dbSchema.getSchemaId());
        System.out.println(reloaded.get());
    }
}
