/*
  Copyright (c) 2020 - for information on the respective copyright owner
  see the NOTICE file and/or the repository at
  https://github.com/hyperledger-labs/business-partner-agent

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package org.hyperledger.bpa.controller.api.partner;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.StringUtils;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hyperledger.aries.api.proof.PresentProofRequest;

@Data
@NoArgsConstructor
public class RequestProofRequest {
    private String schemaId;
    @Nullable
    private String credentialDefinitionId;
    @Nullable
    private String issuerDid;

    public PresentProofRequest.ProofRequest.ProofAttributes.ProofRestrictions buildRestrictions() {
        PresentProofRequest.ProofRequest.ProofAttributes.ProofRestrictions.ProofRestrictionsBuilder builder = PresentProofRequest.ProofRequest.ProofAttributes.ProofRestrictions
                .builder();
        if (StringUtils.isNotEmpty(schemaId)) {
            builder.schemaId(schemaId);
        }
        if (StringUtils.isNotEmpty(credentialDefinitionId)) {
            builder.credentialDefinitionId(credentialDefinitionId);
        }
        if (StringUtils.isNotEmpty(issuerDid)) {
            builder.issuerDid(issuerDid);
        }
        return builder.build();
    }
}
