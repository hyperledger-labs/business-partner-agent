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

import lombok.experimental.Accessors;
import org.hyperledger.aries.api.present_proof.PresentProofRequest;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRole;
import org.hyperledger.aries.api.present_proof.PresentationExchangeState;
import org.hyperledger.bpa.controller.api.prooftemplates.ProofTemplate;
import org.hyperledger.bpa.model.PartnerProof;

import io.micronaut.core.annotation.Nullable;

import java.util.Map;
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

    public Map<PresentationExchangeState, Long> stateToTimestamp;

    private String typeLabel;
    private String problemReport;

    /** if verifier, revealed attributes from the other agent */
    // TODO this should always be set, but it isn't
    // TODO now use attribute groups as well, at the moment we always use the first
    private JsonNode proofData;
    private ProofTemplateInfo proofTemplateInfo;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Accessors(chain = true)
    public static class ProofTemplateInfo {
        private ProofTemplate proofTemplate;
        private PresentProofRequest.ProofRequest proofRequest;
    }

    public static AriesProofExchange from(@NonNull PartnerProof p, @Nullable JsonNode proofData) {
        final AriesProofExchangeBuilder b = AriesProofExchange.builder();
        final Long created = p.getCreatedAt().toEpochMilli();

        // deprecated use state to timestamp
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
                .problemReport(p.getProblemReport())
                .exchangeVersion(p.getExchangeVersion())
                .stateToTimestamp(p.getStateToTimestamp() != null ? p.getStateToTimestamp().toApi() : null)
                .build();
    }

    // TODO delete all deprecated
    // depends on the role
    @Deprecated
    private Long receivedAt;
    @Deprecated
    private Long sentAt;
    // probably not available
    @Deprecated
    private Long issuedAt;

    @Deprecated
    private String issuer;
    @Deprecated
    private String schemaId;
    @Deprecated
    private String credentialDefinitionId;

    /** aca-py proof request object */
    @Deprecated
    private PresentProofRequest.ProofRequest proofRequest;
}
