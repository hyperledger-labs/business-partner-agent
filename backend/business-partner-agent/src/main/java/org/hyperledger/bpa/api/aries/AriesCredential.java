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

import io.micronaut.core.annotation.Nullable;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.bpa.model.BPACredentialExchange;

import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AriesCredential {
    private UUID id;
    private Long issuedAt;
    private CredentialExchangeState state;
    private Boolean isPublic;

    private String issuer;
    private String schemaId;
    private String credentialDefinitionId;
    private String connectionId;
    private Boolean revoked;
    private Boolean revocable;
    private ExchangeVersion exchangeVersion;

    private String label;
    private String typeLabel;
    private Map<String, String> credentialData;

    public static AriesCredential fromBPACredentialExchange(@NonNull BPACredentialExchange c,
            @Nullable String typeLabel) {
        AriesCredentialBuilder b = AriesCredential.builder();
        if (c.getCredential() != null) {
            b
                    .schemaId(c.getCredential().getSchemaId())
                    .credentialDefinitionId(c.getCredential().getCredentialDefinitionId())
                    .revocable(StringUtils.isNotEmpty(c.getCredential().getRevRegId()))
                    .credentialData(c.getCredential().getAttrs());
        }
        return b
                .id(c.getId())
                .issuedAt(c.calculateIssuedAt() != null ? c.calculateIssuedAt().toEpochMilli() : null)
                .state(c.getState())
                .isPublic(c.checkIfPublic())
                .issuer(c.getIssuer())
                .connectionId(c.getPartner() != null ? c.getPartner().getConnectionId() : null)
                .revoked(c.getRevoked())
                .label(c.getLabel())
                .typeLabel(typeLabel)
                .exchangeVersion(c.getExchangeVersion())
                .build();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static final class BPACredentialInfo {
        private UUID credentialId;
        private String schemaLabel;
        private String issuerLabel;
        private String credentialLabel;
    }
}
