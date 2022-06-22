package org.hyperledger.bpa.impl.aries.proof;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.present_proof_v2.V20PresCreateRequestRequest;
import org.hyperledger.aries.api.present_proof_v2.V20PresExRecord;
import org.hyperledger.aries.api.present_proof_v2.V20PresSendRequestRequest;
import org.hyperledger.aries.api.present_proof_v2.V2DIFProofRequest;
import org.hyperledger.aries.config.GsonConfig;
import org.hyperledger.bpa.RunWithAries;
import org.hyperledger.bpa.api.aries.SchemaAPI;
import org.hyperledger.bpa.impl.aries.schema.SchemaService;
import org.hyperledger.bpa.persistence.model.BPAProofTemplate;
import org.hyperledger.bpa.persistence.model.prooftemplate.BPAAttribute;
import org.hyperledger.bpa.persistence.model.prooftemplate.BPAAttributeGroup;
import org.hyperledger.bpa.persistence.model.prooftemplate.BPAAttributeGroups;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@MicronautTest
@ExtendWith(MockitoExtension.class)
public class ProverLDManagerTest extends RunWithAries {

    @Mock
    SchemaService schema;

    @InjectMocks
    ProverLDManager ld;

    @Inject
    AriesClient ac;

    @Test
    void testSimpleTemplate() throws IOException {
        Mockito.when(schema.getSchema(Mockito.any(UUID.class))).thenReturn(Optional.of(SchemaAPI.builder()
                .schemaId("https://w3id.org/citizenship/v1")
                .label("label")
                .build()));
        BPAProofTemplate t = BPAProofTemplate.builder()
                .name("test")
                .attributeGroups(BPAAttributeGroups.builder()
                        .attributeGroup(BPAAttributeGroup.builder()
                                .schemaId(UUID.randomUUID())
                                .attribute(BPAAttribute.builder()
                                        .name("givenName")
                                        .build())
                                .build())
                        .build())
                .build();
        V2DIFProofRequest v2DIFProofRequest = ld.prepareRequest(t);
        System.out.println(GsonConfig.prettyPrinter().toJson(v2DIFProofRequest));

        V20PresCreateRequestRequest req = V20PresCreateRequestRequest.builder()
                .presentationRequest(V20PresSendRequestRequest.V20PresRequestByFormat.builder()
                        .dif(v2DIFProofRequest)
                        .build())
                .build();
        System.out.println(GsonConfig.prettyPrinter().toJson(req));
        V20PresExRecord v20PresExRecord = ac.presentProofV2CreateRequest(req)
                .orElseThrow();
        v20PresExRecord = ac.presentProofV2RecordsGetById(v20PresExRecord.getPresentationExchangeId()).orElseThrow();
        System.out.println(v20PresExRecord);
    }
}
