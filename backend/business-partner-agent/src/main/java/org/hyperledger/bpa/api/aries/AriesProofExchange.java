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
import io.micronaut.core.annotation.Nullable;
import lombok.*;
import org.hyperledger.aries.api.present_proof.PresentProofRequest;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRole;
import org.hyperledger.aries.api.present_proof.PresentationExchangeState;
import org.hyperledger.bpa.controller.api.prooftemplates.ProofTemplate;
import org.hyperledger.bpa.model.PartnerProof;

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
    // TODO resolve issuer, cred def
    private JsonNode proofData;
    private ProofTemplate proofTemplate;
    private PresentProofRequest.ProofRequest proofRequest;

    public static AriesProofExchange from(@NonNull PartnerProof p, @Nullable JsonNode proofData) {
        final AriesProofExchangeBuilder b = AriesProofExchange.builder();
        return b
                .id(p.getId())
                .partnerId(p.getPartnerId())
                .state(p.getState())
                .proofData(proofData)
                .proofRequest(p.getProofRequest())
                .role(p.getRole())
                .problemReport(p.getProblemReport())
                .exchangeVersion(p.getExchangeVersion())
                .stateToTimestamp(p.getStateToTimestamp() != null ? p.getStateToTimestamp().toApi() : null)
                .build();
    }
}
