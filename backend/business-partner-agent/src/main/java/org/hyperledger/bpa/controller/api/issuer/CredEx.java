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
package org.hyperledger.bpa.controller.api.issuer;

import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.aries.api.ExchangeVersion;
import org.hyperledger.aries.api.credentials.Credential;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.api.PartnerAPI;
import org.hyperledger.bpa.api.aries.SchemaAPI;
import org.hyperledger.bpa.persistence.model.BPACredentialExchange;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CredEx {

    private UUID id;
    private Long createdAt;
    private Long updatedAt;
    private PartnerAPI partner;
    private String schemaId; // TODO UI should use this id instead the one from the credential
    private String credentialDefinitionId; // TODO UI should use this id instead the one from the credential
    private Map<String, String> proposal;
    private Credential credential; // TODO should also be Map<String, String>
    private CredentialExchangeRole role;
    private CredentialExchangeState state;
    public Map<CredentialExchangeState, Long> stateToTimestamp;
    private CredentialType type;
    private String displayText;
    private Boolean revoked;
    private Boolean revocable;
    private ExchangeVersion exchangeVersion;
    private String errorMsg;

    // TODO UI should not need these two
    private SchemaAPI schema;
    private CredDef credDef;

    public static CredEx from(@NonNull BPACredentialExchange db) {
        return from(db, null);
    }

    public static CredEx from(@NonNull BPACredentialExchange db, PartnerAPI partner) {
        CredExBuilder builder = CredEx.builder();
        SchemaAPI schemaAPI = db.getSchema() != null ? SchemaAPI.from(db.getSchema()) : null;
        CredDef credDef = db.getCredDef() != null ? CredDef.from(db.getCredDef())
                : CredDef.builder().schema(schemaAPI).build();
        String displayText = null;
        if (schemaAPI != null) {
            displayText = String.format("%s", schemaAPI.getLabel());
            if (StringUtils.isNotEmpty(schemaAPI.getVersion())) {
                displayText = displayText + String.format(" (%s)", schemaAPI.getVersion());
            }
            if (StringUtils.isNotBlank(credDef.getTag())) {
                displayText = displayText + String.format(" - %s", credDef.getTag());
            }
        } else if (StringUtils.isNotEmpty(db.getErrorMsg())) {
            displayText = db.getErrorMsg();
        }
        Map<String, String> credentialAttrs;
        if (db.stateIsProposalReceived()
                || db.stateIsProposalSent()
                || db.roleIsHolder() && db.stateIsProblem()
                || db.roleIsIssuer() && db.stateIsDeclined()) {
            credentialAttrs = db.proposalAttributesToMap();
        } else if (db.stateIsOfferReceived()
                || db.roleIsHolder() && db.stateIsDeclined()) {
            credentialAttrs = db.offerAttributesToMap();
        } else {
            credentialAttrs = db.credentialAttributesToMap();
        }
        return builder
                .id(db.getId())
                .createdAt(db.getCreatedAt().toEpochMilli())
                .updatedAt(db.getUpdatedAt().toEpochMilli())
                .partner(partner)
                .schemaId(db.getSchema() != null ? db.getSchema().getSchemaId() : null)
                .credentialDefinitionId(db.getCredDef() != null ? db.getCredDef().getCredentialDefinitionId() : null)
                .proposal(db.proposalAttributesToMap())
                .credential(Credential
                        .builder()
                        .schemaId(db.getSchema() != null ? db.getSchema().getSchemaId() : null)
                        .credentialDefinitionId(
                                db.getCredDef() != null ? db.getCredDef().getCredentialDefinitionId() : null)
                        .attrs(credentialAttrs)
                        .build())
                .role(db.getRole())
                .state(db.getState())
                .stateToTimestamp(db.getStateToTimestamp() != null ? db.getStateToTimestamp().toApi() : null)
                .type(db.getType())
                .displayText(displayText)
                .revoked(db.getRevoked())
                .revocable(checkIfRevocable(db))
                .exchangeVersion(db.getExchangeVersion())
                .errorMsg(db.getErrorMsg())

                .schema(schemaAPI)
                .credDef(credDef)
                .build();
    }

    private static Boolean checkIfRevocable(@NonNull BPACredentialExchange db) {
        if (db.roleIsHolder() && db.getIndyCredential() != null) {
            return StringUtils.isNotEmpty(db.getIndyCredential().getRevRegId());
        }
        return StringUtils.isNotEmpty(db.getRevRegId());
    }
}
