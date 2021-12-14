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
package org.hyperledger.bpa.impl.util;

import io.micronaut.core.annotation.Nullable;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

public class AriesStringUtil {

    /**
     * Gets the last segment of a did
     * 
     * @param did String e.g. did or credential definition id
     * @return the last part of the input when separated by : unchanged otherwise
     */
    public static String getLastSegment(@NonNull String did) {
        final String[] parts = did.split(":");
        return parts[parts.length - 1];
    }

    /**
     * Gets the last segment of a did
     *
     * @param did String e.g. did or credential definition id
     * @return the last part of the input when separated by : null otherwise
     */
    public static String getLastSegmentOrNull(@Nullable String did) {
        if (StringUtils.trimToNull(did) != null) {
            return getLastSegment(did);
        }
        return null;
    }

    public static String schemaGetName(@NonNull String schemaId) {
        return splitSchemaId(schemaId)[2];
    }

    public static String schemaGetCreator(@NonNull String schemaId) {
        return splitSchemaId(schemaId)[0];
    }

    public static String schemaGetVersion(@NonNull String schemaId) {
        return splitSchemaId(schemaId)[3];
    }

    private static String[] splitSchemaId(@NonNull String schemaId) {
        String sId = StringUtils.strip(schemaId);
        final String[] parts = sId.split(":");
        if (parts.length != 4) {
            throw new IllegalArgumentException(schemaId + " is not a valid schema id");
        }
        return parts;
    }

    public static boolean isIndySchemaId(String schemaId) {
        String sId = StringUtils.trimToNull(schemaId);
        if (sId == null) {
            return false;
        }
        final String[] parts = sId.split(":");
        return parts.length == 4;
    }

    public static String credDefIdGetSequenceNo(@NonNull String credDefId) {
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

    public static boolean isCredDef(@Nullable String expression) {
        if (StringUtils.isBlank(expression)) {
            return false;
        }
        return expression.split(":").length == 5;
    }

    private static String[] credDefIdSplit(@NonNull String credDefId) {
        final String[] parts = credDefId.split(":");
        if (parts.length != 5) {
            throw new IllegalArgumentException("Not a credential definition id");
        }
        return parts;
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

    public static boolean isUUID(String input) {
        if (StringUtils.isAllBlank(input)) {
            return false;
        }
        try {
            UUID.fromString(input);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
