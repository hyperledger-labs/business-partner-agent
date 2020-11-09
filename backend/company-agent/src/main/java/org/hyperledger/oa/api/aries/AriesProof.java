/*
  Copyright (c) 2020 - for information on the respective copyright owner
  see the NOTICE file and/or the repository at
  https://github.com/hyperledger-labs/organizational-agent

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
package org.hyperledger.oa.api.aries;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;
import org.hyperledger.oa.api.CredentialType;
import org.hyperledger.oa.model.PartnerProof;

import javax.annotation.Nullable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AriesProof {

    private UUID id;
    private UUID partnerId;

    // depends on the role
    private Long receivedAt;
    private Long sentAt;
    // probably not available
    private Long issuedAt;

    private CredentialType type;
    private String state;

    private String issuer;
    private String schemaId;
    private String credentialDefinitionId;
    private String role;
    private JsonNode proofData;

    public static AriesProof from(@NonNull PartnerProof p, @Nullable JsonNode poofData) {
        final AriesProofBuilder b = AriesProof.builder();
        final Long created = p.getCreatedAt().toEpochMilli();
        if (ProofRole.PROVER.getValue().equals(p.getRole())) {
            b.sentAt(created);
        } else {
            b.receivedAt(created);
        }
        return b
                .id(p.getId())
                .partnerId(p.getPartnerId())
                .type(p.getType())
                .state(p.getState())
                .issuer(p.getIssuer())
                .schemaId(p.getSchemaId())
                .credentialDefinitionId(p.getCredentialDefinitionId())
                .proofData(poofData)
                .role(p.getRole())
                .build();
    }

    @Getter
    @AllArgsConstructor
    public enum ProofRole {
        /** Proofs that I received */
        VERIFIER("verifier"),
        /** Proofs that I have sent */
        PROVER("prover");

        private final String value;
    }
}
