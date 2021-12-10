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
import lombok.NonNull;
import org.hyperledger.aries.api.jsonld.VerifiableCredential;

import java.util.List;

/**
 * Document and credential types that the company agent can process.
 */
@Getter
@AllArgsConstructor
public enum CredentialType {

    /**
     * A document that can never be a credential, and is not linked to a schema.
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
     * A document or indy credential that is linked to a ledger schema and uses an
     * embedded context
     */
    INDY(
            List.of(ApiConstants.CREDENTIALS_V1),
            List.of("VerifiableCredential")),

    /**
     * A document or json-ld credential that is not linked to any ledger and uses an
     * external or embedded context
     */
    JSON_LD(
            List.of(
                    ApiConstants.CREDENTIALS_V1,
                    ApiConstants.BBS_V1),
            List.of("VerifiableCredential"));

    private final List<Object> context;
    private final List<String> type;

    /**
     * Tries to determine the credential type from the VC
     *
     * @param c {@link VerifiableCredential.VerifiableIndyCredential}
     * @return {@link CredentialType} or null when no match was found
     */
    public static CredentialType fromCredential(@NonNull VerifiableCredential.VerifiableIndyCredential c) {
        if (c.getType().stream().anyMatch(t -> "OrganizationalProfileCredential".equals(t))) {
            return CredentialType.ORGANIZATIONAL_PROFILE_CREDENTIAL;
        }
        if (c.getContext().stream().anyMatch(ctx -> ApiConstants.BBS_V1.equals(ctx))) {
            return CredentialType.JSON_LD;
        }
        return CredentialType.INDY;
    }

}
