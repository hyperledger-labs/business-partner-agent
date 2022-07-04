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
package org.hyperledger.bpa.impl.aries.proof;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.hyperledger.aries.api.present_proof_v2.V20PresCreateRequestRequest;
import org.hyperledger.aries.api.present_proof_v2.V20PresExRecord;
import org.hyperledger.aries.api.present_proof_v2.V20PresSendRequestRequest;
import org.hyperledger.aries.api.present_proof_v2.V2DIFProofRequest;
import org.hyperledger.aries.config.GsonConfig;
import org.hyperledger.bpa.RunWithAries;
import org.hyperledger.bpa.api.aries.SchemaAPI;
import org.hyperledger.bpa.impl.aries.jsonld.LDContextResolver;
import org.hyperledger.bpa.impl.aries.schema.SchemaService;
import org.hyperledger.bpa.persistence.model.BPAProofTemplate;
import org.hyperledger.bpa.persistence.model.prooftemplate.*;
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

    @Mock
    LDContextResolver ctx;

    @InjectMocks
    VerifierLDManager ld;

    @Test
    void testSimpleTemplate() throws IOException {
        Mockito.when(schema.getSchema(Mockito.any(UUID.class))).thenReturn(Optional.of(SchemaAPI.builder()
                .schemaId("https://w3id.org/citizenship/v1")
                .label("label")
                .build()));
        Mockito.when(ctx.resolve(Mockito.anyString(), Mockito.anyString()))
                .thenReturn("https://w3id.org/citizenship#PermanentResident");
        BPAProofTemplate t = BPAProofTemplate.builder()
                .name("test")
                .attributeGroups(BPAAttributeGroups.builder()
                        .attributeGroup(BPAAttributeGroup.builder()
                                .schemaId(UUID.randomUUID())
                                .attribute(BPAAttribute.builder()
                                        .name("givenName")
                                        .condition(BPACondition.builder()
                                                .value("Peter")
                                                .operator(ValueOperators.EQUALS)
                                                .build())
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
