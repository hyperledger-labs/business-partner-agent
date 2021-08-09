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
package org.hyperledger.bpa.impl.prooftemplates;

import lombok.Builder;
import org.hyperledger.acy_py.generated.model.IndyProofReqPredSpec;
import org.hyperledger.aries.api.present_proof.PresentProofRequest;
import org.hyperledger.bpa.model.prooftemplate.BPASchemaRestrictions;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

@Builder
class Predicate {
    String schemaId;
    AtomicInteger sameSchemaCounter;
    NonRevocationApplicator revocationApplicator;
    BPASchemaRestrictions schemaRestrictions;
    String name;
    IndyProofReqPredSpec.PTypeEnum operator;
    Integer value;

    public void addToBuilder(
            BiConsumer<String, PresentProofRequest.ProofRequest.ProofRequestedPredicates> builderSink) {
        PresentProofRequest.ProofRequest.ProofRestrictions.ProofRestrictionsBuilder restrictionsBuilder = ProofTemplateElementVisitor
                .asProofRestrictionsBuilder(
                        schemaRestrictions);
        PresentProofRequest.ProofRequest.ProofRequestedPredicates.ProofRequestedPredicatesBuilder builder = PresentProofRequest.ProofRequest.ProofRequestedPredicates
                .builder()
                .name(name)
                .pType(operator)
                .pValue(value)
                .restriction(restrictionsBuilder.schemaId(schemaId).build().toJsonObject());
        String predicateName = schemaId + sameSchemaCounter.incrementAndGet();
        builderSink.accept(predicateName, revocationApplicator.applyOn(builder).build());

    }
}
