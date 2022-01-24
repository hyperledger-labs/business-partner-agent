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
package org.hyperledger.bpa.controller.api.admin;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.micronaut.core.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hyperledger.bpa.api.CredentialType;

import java.util.List;
import java.util.Set;

@SuperBuilder
@Data
@AllArgsConstructor
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "credentialType",
        defaultImpl = AddSchemaRequest.AddIndySchema.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AddSchemaRequest.AddIndySchema.class, names = { "INDY", "indy" }),
        @JsonSubTypes.Type(value = AddSchemaRequest.AddJsonLDSchema.class, names = { "JSON_LD", "json_ld", "json-ld" }),
})
public abstract class AddSchemaRequest {

    private CredentialType credentialType;

    @Nullable
    private String label;

    private String schemaId;

    @Nullable
    private String defaultAttributeName;

    @SuppressWarnings("unused")
    public AddSchemaRequest() {
        this.credentialType = CredentialType.INDY;
    }

    public AddSchemaRequest(CredentialType credentialType) {
        this.credentialType = credentialType;
    }

    public boolean typeIsIndy() {
        return CredentialType.INDY.equals(credentialType);
    }

    public boolean typeIsJsonLD() {
        return CredentialType.JSON_LD.equals(credentialType);
    }

    @SuperBuilder
    @Data
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static final class AddIndySchema extends AddSchemaRequest {
        @Nullable
        private List<AddTrustedIssuerRequest> trustedIssuer;

        public AddIndySchema() {
            super(CredentialType.INDY);
        }
    }

    @SuperBuilder
    @Data
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    public static final class AddJsonLDSchema extends AddSchemaRequest {
        private Set<String> attributes;
        private String ldType;

        public AddJsonLDSchema() {
            super(CredentialType.JSON_LD);
        }
    }
}
