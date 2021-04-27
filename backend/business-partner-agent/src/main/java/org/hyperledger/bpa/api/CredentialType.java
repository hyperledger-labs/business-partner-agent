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
package org.hyperledger.bpa.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Document and credential types that the company agent can process.
 */
@Getter
@AllArgsConstructor
public enum CredentialType {

    /**
     * A document that can never be a credential and that is not linked to a schema.
     * Typed documents use a static context, like it is defined here.
     */
    ORGANIZATIONAL_PROFILE_CREDENTIAL(
            List.of(
                    ApiConstants.CREDENTIALS_V1,
                    "https://raw.githubusercontent.com/iil-network/contexts/master/masterdata.jsonld",
                    "https://raw.githubusercontent.com/iil-network/contexts/master/labeled-credential.jsonld"),
            List.of(
                    "VerifiableCredential",
                    "LabeledCredential",
                    "OrganizationalProfileCredential")),
    /**
     * A document or indy credential that is linked to a schema and uses an ad hoc
     * context
     */
    SCHEMA_BASED(
            List.of(ApiConstants.CREDENTIALS_V1),
            List.of("VerifiableCredential"));

    // json-ld

    private final List<Object> context;
    private final List<String> type;

    /**
     * Tries to get the type from the type list
     *
     * @param type the list of credential types
     * @return {@link CredentialType} or null when no match was found
     */
    public static CredentialType fromType(List<String> type) {
        for (String t : type) {
            if ("OrganizationalProfileCredential".equals(t)) {
                return CredentialType.ORGANIZATIONAL_PROFILE_CREDENTIAL;
            }
        }
        return CredentialType.SCHEMA_BASED;
    }

}
