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
package org.hyperledger.bpa.impl.aries.jsonld;

import io.micronaut.core.annotation.Nullable;
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecordByFormat;
import org.hyperledger.bpa.api.CredentialType;

import java.util.ArrayList;
import java.util.List;

public class LDContextHelper {

    public static String findSchemaId(@Nullable V20CredExRecordByFormat.LdProof ldProof) {
        if (ldProof == null) {
            return null;
        }
        // TODO this does not consider all use cases
        List<Object> context = ldProof.getCredential().getContext();
        List<Object> contextCopy = new ArrayList<>(context);
        contextCopy.removeAll(CredentialType.JSON_LD.getContext());
        return (String) contextCopy.get(0);
    }
}
