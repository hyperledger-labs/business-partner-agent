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

import org.hyperledger.aries.api.present_proof_v2.DIFField;
import org.hyperledger.bpa.persistence.model.prooftemplate.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class VerifierLDManagerTest {

    @Test
    void testBuildDifFields() {
        VerifierLDManager ld = new VerifierLDManager();

        Map<UUID, DIFField> fields = ld.buildDifFieldsFromGroup(BPAAttributeGroup.builder().build());
        Assertions.assertEquals(0, fields.size());

        fields = ld.buildDifFieldsFromGroup(BPAAttributeGroup.builder()
                        .schemaLevelRestrictions(List.of(BPASchemaRestrictions.builder()
                                        .issuerDid("did:indy:123")
                                .build()))
                .build());
        Assertions.assertEquals(1, fields.size());
        Assertions.assertEquals("did:indy:123", fields.values().iterator().next().getFilter().get_const());

        fields = ld.buildDifFieldsFromGroup(BPAAttributeGroup.builder()
                .attribute(BPAAttribute.builder()
                        .name("dummy")
                        .condition(BPACondition.builder()
                                .operator(ValueOperators.EQUALS)
                                .value("something")
                                .build())
                        .build())
                .build());
        Assertions.assertEquals(1, fields.size());
        Assertions.assertEquals("something", fields.values().iterator().next().getFilter().get_const());
    }
}
