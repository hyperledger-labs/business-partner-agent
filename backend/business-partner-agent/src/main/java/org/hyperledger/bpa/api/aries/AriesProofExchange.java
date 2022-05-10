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
package org.hyperledger.bpa.api.aries;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;
import org.hyperledger.aries.api.ExchangeVersion;
import org.hyperledger.aries.api.present_proof.PresentProofRequest;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRole;
import org.hyperledger.aries.api.present_proof.PresentationExchangeState;
import org.hyperledger.aries.api.present_proof_v2.V2DIFProofRequest;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.persistence.model.PartnerProof;
import org.hyperledger.bpa.persistence.model.converter.ExchangePayload;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Requested or received Proof Requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AriesProofExchange {

    private UUID id;
    private UUID partnerId;

    private ExchangeVersion exchangeVersion;
    private PresentationExchangeState state;
    private PresentationExchangeRole role;
    private CredentialType type;

    private Long updatedAt;
    public Map<PresentationExchangeState, Long> stateToTimestamp;

    private String typeLabel;

    /** revealed attributes by group */
    private JsonNode proofData;
    private Boolean valid;
    private PresentProofRequest.ProofRequest proofRequest;
    private String problemReport;

    public static AriesProofExchange from(@NonNull PartnerProof p) {
        final AriesProofExchangeBuilder b = AriesProofExchange.builder();
        return b
                .id(p.getId())
                .partnerId(p.getPartnerId())
                .state(p.getState())
                .proofRequest(p.getProofRequest() != null ? p.getProofRequest().getIndy() : null) // TODO json_ld
                .role(p.getRole())
                .type(p.getType())
                .problemReport(p.getProblemReport())
                .exchangeVersion(p.getExchangeVersion())
                .stateToTimestamp(p.getStateToTimestamp() != null ? p.getStateToTimestamp().toApi() : null)
                .valid(p.getValid())
                .updatedAt(p.getUpdatedAt().toEpochMilli())
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Identifier {
        private String schemaId;
        private String schemaLabel;
        private String credentialDefinitionId;
        private String issuerLabel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevealedAttributeGroup {
        @Singular
        private Map<String, String> revealedAttributes;
        private Identifier identifier;
    }
}
