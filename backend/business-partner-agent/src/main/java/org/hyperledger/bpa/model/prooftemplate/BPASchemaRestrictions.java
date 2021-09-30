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
package org.hyperledger.bpa.model.prooftemplate;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.bpa.controller.api.prooftemplates.SchemaRestrictions;
import org.hyperledger.bpa.impl.util.AriesStringUtil;
import org.hyperledger.bpa.impl.verification.ValidUUID;

import java.util.Optional;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Introspected
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public class BPASchemaRestrictions {
    @Nullable
    @ValidUUID
    private String schemaId;
    @Nullable
    private String schemaName;
    @Nullable
    private String schemaVersion;
    @Nullable
    private String schemaIssuerDid;
    @Nullable
    private String credentialDefinitionId;
    @Nullable
    private String issuerDid;

    public SchemaRestrictions toRepresentation() {
        return SchemaRestrictions.builder()
                .schemaId(getSchemaId())
                .schemaName(getSchemaName())
                .schemaVersion(getSchemaVersion())
                .schemaIssuerDid(getSchemaIssuerDid())
                .credentialDefinitionId(getCredentialDefinitionId())
                .issuerDid(getIssuerDid())
                .build();
    }

    public static BPASchemaRestrictions fromRepresentation(SchemaRestrictions schemaRestrictions) {
        return Optional.ofNullable(schemaRestrictions)
                .map(other -> BPASchemaRestrictions.builder()
                        .schemaId(StringUtils.trimToNull(other.getSchemaId()))
                        .schemaName(StringUtils.trimToNull(other.getSchemaName()))
                        .schemaVersion(StringUtils.trimToNull(other.getSchemaVersion()))
                        .schemaIssuerDid(AriesStringUtil.getLastSegmentOrNull(other.getSchemaIssuerDid()))
                        .credentialDefinitionId(StringUtils.trimToNull(other.getCredentialDefinitionId()))
                        .issuerDid(AriesStringUtil.getLastSegmentOrNull(other.getIssuerDid()))
                        .build())
                .orElse(null);
    }
}
