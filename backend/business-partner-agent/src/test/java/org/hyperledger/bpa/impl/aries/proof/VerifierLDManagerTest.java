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

import org.hyperledger.aries.api.present_proof_v2.V2DIFProofRequest;
import org.hyperledger.bpa.api.aries.SchemaAPI;
import org.hyperledger.bpa.impl.aries.schema.SchemaService;
import org.hyperledger.bpa.persistence.model.BPAProofTemplate;
import org.hyperledger.bpa.persistence.model.prooftemplate.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class VerifierLDManagerTest {

    @Mock
    private SchemaService schemaService;

    @InjectMocks
    private VerifierLDManager ld = new VerifierLDManager();

    @Test
    void testPrepareRequestWithIssuerRestriction() {
        UUID schemaDbId = UUID.randomUUID();
        Mockito.when(schemaService.getSchema(schemaDbId)).thenReturn(Optional.of(SchemaAPI.builder()
                        .id(schemaDbId)
                        .expandedType("some-type")
                .build()));

        V2DIFProofRequest difProofRequest = ld.prepareRequest(BPAProofTemplate.builder()
                .attributeGroups(BPAAttributeGroups.builder()
                        .attributeGroup(BPAAttributeGroup.builder()
                                .schemaId(schemaDbId)
                                .schemaLevelRestrictions(List.of(BPASchemaRestrictions.builder()
                                        .issuerDid("did:indy:123")
                                        .build()))
                                .build())
                        .build())
                .build());

        Assertions.assertEquals(1, difProofRequest.getPresentationDefinition().getInputDescriptors().size());

        V2DIFProofRequest.PresentationDefinition.Constraints constraints = difProofRequest.getPresentationDefinition()
                .getInputDescriptors().get(0)
                .getConstraints();
        Assertions.assertEquals("did:indy:123", constraints.getFields().get(0)
                .getFilter().get_const());
        Assertions.assertEquals(constraints.getFields().get(0).getId(), constraints.getIsHolder().get(0)
                .getFieldId().get(0));
    }

    @Test
    void testPrepareRequestWithAttributeRestriction() {
        UUID schemaDbId = UUID.randomUUID();
        Mockito.when(schemaService.getSchema(schemaDbId)).thenReturn(Optional.of(SchemaAPI.builder()
                .id(schemaDbId)
                .expandedType("some-type")
                .build()));

        V2DIFProofRequest difProofRequest = ld.prepareRequest(BPAProofTemplate.builder()
                .attributeGroups(BPAAttributeGroups.builder()
                        .attributeGroup(BPAAttributeGroup.builder()
                                .schemaId(schemaDbId)
                                .attribute(BPAAttribute.builder()
                                        .name("dummy")
                                        .condition(BPACondition.builder()
                                                .operator(ValueOperators.EQUALS)
                                                .value("something")
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build());

        Assertions.assertEquals(1, difProofRequest.getPresentationDefinition().getInputDescriptors().size());

        V2DIFProofRequest.PresentationDefinition.Constraints constraints = difProofRequest.getPresentationDefinition()
                .getInputDescriptors().get(0)
                .getConstraints();
        Assertions.assertEquals("something", constraints.getFields().get(0)
                .getFilter().get_const());
        Assertions.assertEquals(constraints.getFields().get(0).getId(), constraints.getIsHolder().get(0)
                .getFieldId().get(0));
    }
}
