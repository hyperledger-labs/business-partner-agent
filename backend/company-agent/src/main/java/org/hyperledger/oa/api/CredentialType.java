/*
  Copyright (c) 2020 - for information on the respective copyright owner
  see the NOTICE file and/or the repository at
  https://github.com/hyperledger-labs/business-partner-agent

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package org.hyperledger.oa.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.hyperledger.oa.impl.util.AriesStringUtil;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

/**
 * Document and credential types that the company agent can process.
 */
@Getter
@AllArgsConstructor
public enum CredentialType {
    ORGANIZATIONAL_PROFILE_CREDENTIAL(
            List.of(
                    ApiConstants.CREDENTIALS_V1,
                    "https://raw.githubusercontent.com/iil-network/contexts/master/masterdata.jsonld",
                    "https://raw.githubusercontent.com/iil-network/contexts/master/labeled-credential.jsonld"),
            List.of(
                    "VerifiableCredential",
                    "LabeledCredential",
                    "OrganizationalProfileCredential"),
            "masterdata"),
    BANK_ACCOUNT_CREDENTIAL(
            List.of(
                    ApiConstants.CREDENTIALS_V1,
                    "https://raw.githubusercontent.com/iil-network/contexts/master/bankaccount.json",
                    "https://raw.githubusercontent.com/iil-network/contexts/master/labeled-credential.jsonld"),
            List.of(
                    "VerifiableCredential",
                    "LabeledCredential",
                    "BankAccountCredential"),
            "bank_account"),
    COMMERCIAL_REGISTER_CREDENTIAL(
            List.of(ApiConstants.CREDENTIALS_V1),
            List.of("VerifiableCredential"),
            "commercialregister"),
    OTHER(
            List.of(ApiConstants.CREDENTIALS_V1),
            List.of("VerifiableCredential"),
            "other");

    // json-ld

    private final List<Object> context;
    private final List<String> type;

    // aries credential tag

    private final String credentialTag;

    /**
     * Tries to get the type from the type list
     *
     * @param type the list of credential types
     * @return {@link CredentialType} or null when no match was found
     */
    public static @Nullable CredentialType fromType(List<String> type) {
        for (String t : type) {
            if ("OrganizationalProfileCredential".equals(t)) {
                return CredentialType.ORGANIZATIONAL_PROFILE_CREDENTIAL;
            } else if ("BankAccountCredential".equals(t)) {
                return CredentialType.BANK_ACCOUNT_CREDENTIAL;
            }
        }
        return null;
    }

    /**
     * Maps the aries schema name to a {@link CredentialType}
     *
     * @param schemaId id of the schema, not the schema name
     * @return {@link CredentialType}
     */
    public static CredentialType fromSchemaId(@NonNull String schemaId) {
        String schemaName = AriesStringUtil.schemaGetName(schemaId);
        String normalizedName = schemaName.toLowerCase(Locale.US);

        if (ORGANIZATIONAL_PROFILE_CREDENTIAL.getCredentialTag().equals(normalizedName)) {
            return ORGANIZATIONAL_PROFILE_CREDENTIAL;
        } else if (normalizedName.contains("bankaccount")
                || normalizedName.contains(BANK_ACCOUNT_CREDENTIAL.getCredentialTag())) {
            return BANK_ACCOUNT_CREDENTIAL;
        }
        return OTHER;
    }

}
