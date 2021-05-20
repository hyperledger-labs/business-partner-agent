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

package org.hyperledger.bpa.impl.aries;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.checkerframework.checker.optional.qual.Present;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord;
import org.hyperledger.aries.api.present_proof.PresentationRequest;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;

import org.mockito.Mock;
import org.mockito.Mockito;

import io.micronaut.core.type.Argument;

public class ProofManagerTest {

    @Mock
    private AriesClient aries;

    @InjectMocks
    private ProofManager proofManager;

    private PresentationExchangeRecord presentationExchangeRecord = new PresentationExchangeRecord();

    // private List<PresentationRequestCredentials> presentationRequestCreds = new
    // ArrayList<>();

    @Test
    void testProofConstructionOneReqAttr() {
    }

    @Test
    void testProofConstructionTwoReqAttrfromDiffCreds() {
    }

    @Test
    void testProofConstructionOneReqAttrWithSchemaRestriction() {
    }

    @Test
    void testProofConstructionOnePredicate() {
    }

    @Test
    void testProofConstructionOnePredicatewithSchemaRestriction() {
    }

    @Test
    void testProofConstructionOneReqAttrOnePredicateWithSchemaRestrictions() {
    }

}
