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
package org.hyperledger.bpa.impl.aries.jsonld;

import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecordByFormat;
import org.hyperledger.aries.api.jsonld.VerifiableCredential;
import org.hyperledger.bpa.api.CredentialType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
public class LDContextHelperTest {

    @Test
    void testFindLDSchemaId() {
        String sId = "https://w3id.org/citizenship/v1";
        List<Object> ctx = new ArrayList<>(CredentialType.JSON_LD.getContext());
        ctx.add(sId);
        String resolvedId = LDContextHelper.findSchemaId(V20CredExRecordByFormat.LdProof.builder()
                .credential(VerifiableCredential.builder()
                        .context(ctx)
                        .build())
                .build());
        Assertions.assertEquals(sId, resolvedId);
    }

    @Test
    void testFindSchemaIdInObjectList() {
        String expected = "https://w3id.org/citizenship/v1";
        List<Object> ol = List.of(expected, "https://www.w3.org/2018/credentials/v1");
        Assertions.assertEquals(expected, LDContextHelper.findSchemaId(ol));
    }
}
