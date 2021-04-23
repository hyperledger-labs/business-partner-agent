/*
 *
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hyperledger.bpa.controller.api.issuer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
    private String role;
    private String state;
    private String type;
    private String label;
    private String threadId;
    private String credentialExchangeId;

    public static CredEx from(BPACredentialExchange db) {
        return CredEx
                .builder()
                .id(db.getId())
                .createdAt(db.getCreatedAt().toEpochMilli())
                .updatedAt(db.getUpdatedAt().toEpochMilli())
                .role(db.getRole())
                .state(db.getState())
                .schema(SchemaAPI.from(db.getSchema()))
                .partner(PartnerAPI.from(db.getPartner()))
                .credDef(CredDef.from(db.getCredDef()))
                .credential(db.getCredential())
                .type(db.getType().name())
                .label(db.getLabel())
                .threadId(db.getThreadId())
                .credentialExchangeId(db.getCredentialExchangeId())
                .build();
    }
}
