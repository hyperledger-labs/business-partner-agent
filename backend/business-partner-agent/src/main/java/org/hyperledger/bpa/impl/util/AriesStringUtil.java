/*
 * Copyright (c) 2020 - for information on the respective copyright owner
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
package org.hyperledger.bpa.impl.util;

import org.apache.commons.lang3.StringUtils;

import lombok.NonNull;

public class AriesStringUtil {

    /**
     * aca-py can not not work with DID's like did:sov:iil:foo, it only works with
     * the foo part
     *
     * @param did String e.g. did or credential definition id
     * @return the last part of the input when separated by : unchanged otherwise
     */
    public static String getLastSegment(@NonNull String did) {
        final String[] parts = did.split(":");
        return parts[parts.length - 1];
    }

    public static String schemaGetName(@NonNull String schemaId) {
        String sId = StringUtils.strip(schemaId);
        final String[] parts = sId.split(":");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Not a valid schema id");
        }
        return parts[2];
    }

    public static String credDefIdGetSquenceNo(@NonNull String credDefId) {
        final String[] parts = credDefIdSplit(credDefId);
        return parts[3];
    }

    public static String credDefIdGetDid(@NonNull String credDefId) {
        final String[] parts = credDefIdSplit(credDefId);
        return parts[0];
    }

    public static String credDefIdGetTag(@NonNull String credDefId) {
        final String[] parts = credDefIdSplit(credDefId);
        return parts[4];
    }

    /**
     * Transform value to an acceptable value for schema name or attribute name.
     * Cannot contain whitespace. Leading and trailing removed, other replaced with
     * replaceChar.
     *
     * @param value String value to use as a schema name or attribute name
     * @return String with no whitespace
     */
    public static String schemaAttributeFormat(@NonNull String value, @NonNull Character replaceChar) {
        return value.trim().replaceAll("\\s+", String.valueOf(replaceChar));
    }

    /**
     * Transform value to an acceptable value for schema name or attribute name.
     * Cannot contain whitespace. Leading and trailing removed, other replaced with
     * underscore.
     *
     * @param value String value to use as a schema name or attribute name
     * @return String with no whitespace
     */
    public static String schemaAttributeFormat(@NonNull String value) {
        return schemaAttributeFormat(value, '_');
    }

    private static String[] credDefIdSplit(String credDefId) {
        final String[] parts = credDefId.split(":");
        if (parts.length != 5) {
            throw new IllegalArgumentException("Not a credential definition id");
        }
        return parts;
    }
}
