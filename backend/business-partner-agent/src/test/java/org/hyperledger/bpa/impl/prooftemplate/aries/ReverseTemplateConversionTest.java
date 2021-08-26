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
package org.hyperledger.bpa.impl.prooftemplate.aries;

import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord;
import org.hyperledger.aries.webhook.EventParser;
import org.hyperledger.bpa.BaseTest;
import org.hyperledger.bpa.controller.api.prooftemplates.ProofTemplate;
import org.hyperledger.bpa.impl.prooftemplates.ProofTemplateConversion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ReverseTemplateConversionTest extends BaseTest {

    private final EventParser ep = new EventParser();

    @Test
    void testVerifierVerified() throws Exception {
        String json = loader.load("files/self-request-proof/05-verifier-verified-attr-groups.json");
        PresentationExchangeRecord ex = ep.parsePresentProof(json).orElseThrow();

        ProofTemplate converted = ProofTemplateConversion.requestToTemplate(ex.getPresentationRequest());
        Assertions.assertNotNull(converted);
        Assertions.assertEquals("Demo Bank", converted.getName());
        Assertions.assertEquals(1, converted.getAttributeGroups().size());
        Assertions.assertTrue(converted.getAttributeGroups().get(0).getAttributeGroupName().startsWith("M6M"));
        System.out.println(mapper.writeValueAsString(converted));
    }
}
