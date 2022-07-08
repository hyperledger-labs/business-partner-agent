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

import org.hyperledger.aries.api.present_proof.PresentProofRequest;
import org.hyperledger.aries.api.present_proof_v2.V2DIFProofRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

public class LDConverterTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testOnlyOneRestrictionUrl() throws Exception {
        V2DIFProofRequest pr = mapper.readValue(request, V2DIFProofRequest.class);
        PresentProofRequest.ProofRequest indy = LDConverter.difToIndyProofRequest(pr);
        Assertions.assertEquals(1, indy.getRequestedAttributes().size());
        Assertions.assertEquals(1, indy.getRequestedAttributes().get("descr-id").getRestrictions().size());
    }

    private final String request = """
            {
                "options": {
                    "domain": "24dddd3b-28a1-401d-855c-b84617cd451f",
                    "challenge": "2dbe1a4a-09ed-494c-b613-563e61d8afa4"
                },
                "presentationDefinition": {
                    "id": "5cde420d-2806-4a34-88e7-d12030870671",
                    "name": "PR",
                    "inputDescriptors": [
                        {
                            "id": "descr-id",
                            "name": "PermanentResident",
                            "schema": [
                                {
                                    "uri": "https://w3id.org/citizenship#PermanentResident"
                                }
                            ],
                            "constraints": {
                                "fields": [
                                    {
                                        "id": "fa7101e4-dbb2-4f87-b5e2-30213d8395b6",
                                        "path": [
                                            "$.credentialSubject.name"
                                        ]
                                    },
                                    {
                                        "id": "f69d20ce-ad33-4451-81e1-a1f9c622dbbd",
                                        "path": [
                                            "$.credentialSubject.description"
                                        ]
                                    }
                                ],
                                "isHolder": [
                                    {
                                        "fieldId": [
                                            "fa7101e4-dbb2-4f87-b5e2-30213d8395b6"
                                        ],
                                        "directive": "PREFERRED"
                                    },
                                    {
                                        "fieldId": [
                                            "f69d20ce-ad33-4451-81e1-a1f9c622dbbd"
                                        ],
                                        "directive": "PREFERRED"
                                    }
                                ]
                            }
                        }
                    ]
                }
            }
            """;
}
