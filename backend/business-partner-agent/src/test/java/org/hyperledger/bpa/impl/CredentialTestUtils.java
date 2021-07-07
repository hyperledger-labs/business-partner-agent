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
package org.hyperledger.bpa.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.api.MyDocumentAPI;

public class CredentialTestUtils {

    private final ObjectMapper mapper;

    public CredentialTestUtils(ObjectMapper m) {
        mapper = m;
    }

    public MyDocumentAPI createDummyCred(CredentialType credType, Boolean isPublic) throws JsonProcessingException {
        String json = "{ \"iban\":\"Hello\" }";
        JsonNode jsonNode = mapper.readTree(json);
        return MyDocumentAPI.builder()
                .type(credType)
                .documentData(jsonNode)
                .isPublic(isPublic)
                .schemaId(CredentialType.SCHEMA_BASED.equals(credType) ? "M6Mbe3qx7vB4wpZF4sBRjt:2:bank_account:1.0"
                        : null)
                .build();
    }
}