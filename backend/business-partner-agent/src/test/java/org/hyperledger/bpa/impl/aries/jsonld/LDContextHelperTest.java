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
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.persistence.model.BPASchema;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
public class LDContextHelperTest {

    @Mock
    private BPAMessageSource.DefaultMessageSource ms;

    @InjectMocks
    private final LDContextHelper h = new LDContextHelper();

    @Test
    void testSchemaValidationSuccess() {
        h.validateDocumentAgainstSchema(BPASchema.builder()
                .schemaAttributeNames(Set.of("name", "id", "some"))
                .build(), Map.of("name", "me", "id", "123"));
    }

    @Test
    void testSchemaValidationFailure() {
        Assertions.assertThrows(WrongApiUsageException.class, () -> h.validateDocumentAgainstSchema(BPASchema.builder()
                .schemaAttributeNames(Set.of("name", "id", "some"))
                .build(), Map.of("other", "123")));
    }

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
}
