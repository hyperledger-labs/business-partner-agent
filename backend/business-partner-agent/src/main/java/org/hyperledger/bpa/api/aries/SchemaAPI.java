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

import io.micronaut.core.util.CollectionUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hyperledger.bpa.controller.api.admin.TrustedIssuer;
import org.hyperledger.bpa.controller.api.issuer.CredDef;
import org.hyperledger.bpa.impl.activity.Identity;
import org.hyperledger.bpa.impl.util.AriesStringUtil;
import org.hyperledger.bpa.model.BPASchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemaAPI {

    private UUID id;

    private String label;

    private String schemaId;

    private String version;

    private Boolean isReadOnly;

    private Set<String> schemaAttributeNames;

    private List<TrustedIssuer> trustedIssuer;

    private List<CredDef> credentialDefinitions;

    private Boolean isMine;

    public static SchemaAPI from(BPASchema s) {
        return from(s, true, true, null);
    }

    public static SchemaAPI from(BPASchema s, Identity identity) {
        return from(s, true, true, identity);
    }

    public static SchemaAPI from(BPASchema s, boolean includeRestrictions, boolean includeCredDefs) {
        return from(s, includeRestrictions, includeCredDefs, null);
    }

    public static SchemaAPI from(BPASchema s, boolean includeRestrictions, boolean includeCredDefs, Identity identity) {
        SchemaAPIBuilder builder = SchemaAPI.builder();
        if (includeRestrictions && CollectionUtils.isNotEmpty(s.getRestrictions())) {
            List<TrustedIssuer> ti = new ArrayList<>();
            s.getRestrictions().forEach(r -> ti.add(TrustedIssuer.from(r)));
            builder.trustedIssuer(ti);
        }
        if (includeCredDefs && CollectionUtils.isNotEmpty(s.getCredentialDefinitions())) {
            List<CredDef> cd = new ArrayList<>();
            s.getCredentialDefinitions().forEach(r -> cd.add(CredDef.from(r)));
            builder.credentialDefinitions(cd);
        }
        if (identity != null) {
            builder.isMine(identity.isMySchema(s.getSchemaId()));
        }
        String version = s.getSchemaId() == null ? "" : AriesStringUtil.schemaGetVersion(s.getSchemaId());
        return builder
                .id(s.getId())
                .label(s.getLabel())
                .schemaId(s.getSchemaId())
                .version(version)
                .schemaAttributeNames(s.getSchemaAttributeNames())
                .isReadOnly(s.getIsReadOnly())
                .build();
    }

}
