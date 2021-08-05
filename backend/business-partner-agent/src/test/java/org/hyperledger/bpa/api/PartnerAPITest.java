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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class PartnerAPITest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testGetAlias() {
        PartnerAPI p = PartnerAPI.builder().alias("my-alias").build();
        Assertions.assertEquals("my-alias", p.getName());
    }

    @Test
    void testGetLegalNameNoMatch() throws Exception {
        PartnerAPI p = PartnerAPI.builder()
                .credential(List.of(
                        PartnerAPI.PartnerCredential
                                .builder()
                                .type(CredentialType.ORGANIZATIONAL_PROFILE_CREDENTIAL)
                                .credentialData(mapper.readTree("{}"))
                                .build()))
                .build();
        Assertions.assertNull(p.getName());
    }

    @Test
    void testGetLegalNameMatch() throws Exception {
        PartnerAPI p = PartnerAPI.builder()
                .credential(List.of(
                        PartnerAPI.PartnerCredential
                                .builder()
                                .type(CredentialType.ORGANIZATIONAL_PROFILE_CREDENTIAL)
                                .credentialData(mapper.readTree(credentialData))
                                .build()))
                .build();
        Assertions.assertEquals("Customer AG", p.getName());
    }

    @Test
    void testGetLabel() {
        PartnerAPI p = PartnerAPI.builder().label("their-label").build();
        Assertions.assertEquals("their-label", p.getName());
    }

    @Test
    void testGetDid() {
        PartnerAPI p = PartnerAPI.builder().did("did:sov:123").build();
        Assertions.assertEquals("did:sov:123", p.getName());
    }

    private final String credentialData = "{\n" +
            "    \"id\": \"did:sov:iil:7D2jFxSPJP4zTAP9WAcJWo\",\n" +
            "    \"type\": \"Legal Entity\",\n" +
            "    \"altName\": \"\",\n" +
            "    \"legalName\": \"Customer AG\",\n" +
            "    \"identifier\": [\n" +
            "        {\n" +
            "            \"id\": \"\",\n" +
            "            \"type\": \"\"\n" +
            "        }\n" +
            "    ],\n" +
            "    \"registeredSite\": {\n" +
            "        \"address\": {\n" +
            "            \"city\": \"My City\",\n" +
            "            \"region\": \"\",\n" +
            "            \"country\": \"My Country\",\n" +
            "            \"zipCode\": \"12345\",\n" +
            "            \"streetAddress\": \"My New Street 456\"\n" +
            "        }\n" +
            "    }\n" +
            "}";
}
