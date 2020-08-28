/**
 * Copyright (c) 2020 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/organizational-agent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hyperledger.oa.api.aries;

import java.util.UUID;

import org.hyperledger.oa.api.CredentialType;
import org.hyperledger.oa.model.PartnerProof;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AriesProof {

    private UUID id;
    private UUID partnerId;
    private Long receivedAt;
    private Long issuedAt;
    private CredentialType type;
    private String state;

    private String issuer;
    private String schemaId;
    private JsonNode proofData;

    public static AriesProof from(PartnerProof p, JsonNode poofData) {
        return AriesProof
                .builder()
                .id(p.getId())
                .partnerId(p.getPartnerId())
                .receivedAt(Long.valueOf(p.getCreatedAt().toEpochMilli()))
                .issuedAt(Long.valueOf(p.getIssuedAt().toEpochMilli()))
                .type(p.getType())
                .state(p.getState())
                .issuer(p.getIssuer())
                .schemaId(p.getSchemaId())
                .proofData(poofData)
                .build();
    }
}
