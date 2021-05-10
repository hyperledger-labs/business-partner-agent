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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.api.PartnerAPI;
import org.hyperledger.bpa.api.aries.SchemaAPI;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.model.BPACredentialExchange;

import javax.inject.Inject;
import java.util.Map;
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
    private SchemaAPI schema;
    private CredDef credDef;
    private PartnerAPI partner;
    private Map<String, Object> credential;
    private CredentialExchangeRole role;
    private CredentialExchangeState state;
    private CredentialType type;
    private String label;
    private String threadId;
    private String credentialExchangeId;
    private String displayText;

    public static CredEx from(BPACredentialExchange db) {
        SchemaAPI schemaAPI = SchemaAPI.from(db.getSchema());
        PartnerAPI partnerAPI = PartnerAPI.from(db.getPartner());
        CredDef credDef = CredDef.from(db.getCredDef());
        String displayText = String.format("%s (%s) - %s", schemaAPI.getLabel(), schemaAPI.getVersion(),
                credDef.getTag());
        return CredEx
                .builder()
                .id(db.getId())
                .createdAt(db.getCreatedAt().toEpochMilli())
                .updatedAt(db.getUpdatedAt().toEpochMilli())
                .role(db.getRole())
                .state(db.getState())
                .schema(schemaAPI)
                .partner(partnerAPI)
                .credDef(credDef)
                .credential(db.getCredential())
                .type(db.getType())
                .label(db.getLabel())
                .threadId(db.getThreadId())
                .credentialExchangeId(db.getCredentialExchangeId())
                .displayText(displayText)
                .build();
    }
}
