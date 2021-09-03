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
package org.hyperledger.bpa.controller.api.issuer;

import jakarta.inject.Inject;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.api.PartnerAPI;
import org.hyperledger.bpa.api.aries.ExchangeVersion;
import org.hyperledger.bpa.api.aries.SchemaAPI;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.model.BPACredentialExchange;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CredEx {
    @Inject
    Converter converter;

    private UUID id;
    private Long createdAt;
    private Long updatedAt;
    private SchemaAPI schema; // why?
    private CredDef credDef; // why?
    private PartnerAPI partner; // why?
    private Map<String, Object> credential;
    private V1CredentialExchange.CredentialProposalDict proposal;
    private CredentialExchangeRole role;
    private CredentialExchangeState state;
    private CredentialType type;
    private String label;
    private String threadId;
    private String credentialExchangeId;
    private String displayText;
    private Boolean revoked;
    private Boolean revocable;
    private ExchangeVersion exchangeVersion;

    public static CredEx from(@NonNull BPACredentialExchange db, @NonNull Optional<Converter> conv) {
        CredExBuilder builder = CredEx.builder();
        conv.ifPresentOrElse(
                c -> builder.partner(c.toAPIObject(db.getPartner())),
                () -> builder.partner(PartnerAPI.from(db.getPartner())));
        SchemaAPI schemaAPI = db.getSchema() != null ? SchemaAPI.from(db.getSchema()) : null;
        CredDef credDef = db.getCredDef() != null ? CredDef.from(db.getCredDef()) : null;
        String displayText = null;
        if (schemaAPI != null && credDef != null) {
            displayText = String.format("%s (%s) - %s", schemaAPI.getLabel(), schemaAPI.getVersion(),
                    credDef.getTag());
        }
        return builder
                .id(db.getId())
                .createdAt(db.getCreatedAt().toEpochMilli())
                .updatedAt(db.getUpdatedAt().toEpochMilli())
                .role(db.getRole())
                .state(db.getState())
                .schema(schemaAPI)
                .credDef(credDef)
                .credential(db.getCredential())
                .proposal(db.getCredentialProposal())
                .type(db.getType())
                .label(db.getLabel())
                .threadId(db.getThreadId())
                .credentialExchangeId(db.getCredentialExchangeId())
                .displayText(displayText)
                .revoked(db.getRevoked())
                .revocable(StringUtils.isNotEmpty(db.getRevRegId()))
                .exchangeVersion(db.getExchangeVersion())
                .build();
    }
}
