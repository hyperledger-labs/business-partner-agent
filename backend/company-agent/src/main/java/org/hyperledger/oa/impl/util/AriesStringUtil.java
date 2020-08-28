/**
 * Copyright (c) 2020 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/organizational-agent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hyperledger.oa.impl.util;

import lombok.NonNull;

public class AriesStringUtil {

    /**
     * aca-py can not not work with DID's like did:sov:iil:foo, it only works with
     * the foo part
     *
     * @param did String e.g. did or credential definition id
     * @return the last part of the input when separated by : unchanged otherwise
     */
    public static String didGetLastSegment(@NonNull String did) {
        final String[] parts = did.split(":");
        return parts[parts.length - 1];
    }

    public static String schemaGetName(@NonNull String schemaId) {
        final String[] parts = schemaId.split(":");
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

    private static String[] credDefIdSplit(String credDefId) {
        final String[] parts = credDefId.split(":");
        if (parts.length != 5) {
            throw new IllegalArgumentException("Not a credential definition id");
        }
        return parts;
    }
}
