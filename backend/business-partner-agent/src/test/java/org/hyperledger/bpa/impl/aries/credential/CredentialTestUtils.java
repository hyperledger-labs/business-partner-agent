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
package org.hyperledger.bpa.impl.aries.credential;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import org.hyperledger.aries.api.credentials.CredentialAttributes;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.api.MyDocumentAPI;

import java.util.ArrayList;

public class CredentialTestUtils {

    private final ObjectMapper mapper;

    public CredentialTestUtils(ObjectMapper m) {
        mapper = m;
    }

    public MyDocumentAPI createDummyCred(@NonNull CredentialType credType, @NonNull Boolean isPublic)
            throws JsonProcessingException {
        String json = "{ \"iban\":\"Hello\" }";
        // JsonNode jsonNode = mapper.readTree(json);
        CredentialAttributes dummyAttribute = new CredentialAttributes("iban", "Hello", null);

        ArrayList<CredentialAttributes> attributesArrayList = new ArrayList<>();
        attributesArrayList.add(dummyAttribute);

        return MyDocumentAPI.builder()
                .type(credType)
                .documentData(attributesArrayList)
                .isPublic(isPublic)
                .schemaId(CredentialType.INDY.equals(credType) ? "M6Mbe3qx7vB4wpZF4sBRjt:2:bank_account:1.0"
                        : null)
                .build();
    }
}