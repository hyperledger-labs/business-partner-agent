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
package org.hyperledger.bpa.api.aries;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

import org.hyperledger.aries.api.present_proof.PresentProofRequest;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRole;
import org.hyperledger.aries.api.present_proof.PresentationExchangeState;
import org.hyperledger.bpa.model.PartnerProof;

import io.micronaut.core.annotation.Nullable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AriesProofExchange {

    private UUID id;
    private UUID partnerId;

    // depends on the role
    private Long receivedAt;
    private Long sentAt;
    // probably not available
    private Long issuedAt;

    private String typeLabel;
    private PresentationExchangeState state;

    private String issuer;
    private String schemaId;
    private String credentialDefinitionId;
    private PresentationExchangeRole role;
    private JsonNode proofData;
    private PresentProofRequest.ProofRequest proofRequest;

    public static AriesProofExchange from(@NonNull PartnerProof p, @Nullable JsonNode proofData) {
        final AriesProofExchangeBuilder b = AriesProofExchange.builder();
        final Long created = p.getCreatedAt().toEpochMilli();
        // TODO: Handle sent AND received date for in Transit ProofExchanges
        if (PresentationExchangeRole.PROVER.equals(p.getRole())) {
            b.sentAt(created);
        } else {
            b.receivedAt(created);
        }
        return b
                .id(p.getId())
                .partnerId(p.getPartnerId())
                .state(p.getState())
                .issuer(p.getIssuer())
                .schemaId(p.getSchemaId())
                .credentialDefinitionId(p.getCredentialDefinitionId())
                .proofData(proofData)
                .proofRequest(p.getProofRequest())
                .role(p.getRole())
                .build();
    }
}
