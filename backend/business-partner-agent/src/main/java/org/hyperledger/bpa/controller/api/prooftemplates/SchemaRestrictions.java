/*
 * Copyright (c) 2020-2021 - for information on the respective copyright owner
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

package org.hyperledger.bpa.controller.api.prooftemplates;

import io.micronaut.core.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hyperledger.aries.api.present_proof.PresentProofRequest;
import org.hyperledger.bpa.impl.verification.ValidUUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemaRestrictions {

    @Nullable
    @ValidUUID // this is confusing
    private String schemaId;
    @Nullable
    private String schemaName;
    @Nullable
    private String schemaVersion;
    @Nullable
    private String schemaIssuerDid;
    @Nullable
    private String credentialDefinitionId;
    @Nullable
    private String issuerDid;

    public static SchemaRestrictions fromProofRestrictions(PresentProofRequest.ProofRequest.ProofRestrictions r) {
        return SchemaRestrictions
                .builder()
                .schemaId(r.getSchemaId())
                .schemaName(r.getSchemaName())
                .schemaVersion(r.getSchemaVersion())
                .schemaIssuerDid(r.getSchemaIssuerDid())
                .credentialDefinitionId(r.getCredentialDefinitionId())
                .issuerDid(r.getIssuerDid())
                .build();
    }
}
