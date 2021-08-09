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
import lombok.Singular;
import org.hyperledger.aries.api.present_proof.PresentProofRequest;
import org.hyperledger.bpa.model.prooftemplate.BPASchemaRestrictions;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

@Builder
class Attributes {
    String schemaId;
    NonRevocationApplicator revocationApplicator;
    BPASchemaRestrictions schemaRestrictions;
    @Singular
    List<String> names;
    @Singular
    Map<String, String> equals;

    public void addToBuilder(
            BiConsumer<String, PresentProofRequest.ProofRequest.ProofRequestedAttributes> builderSink) {
        PresentProofRequest.ProofRequest.ProofRestrictions.ProofRestrictionsBuilder restrictionsBuilder = ProofTemplateElementVisitor
                .asProofRestrictionsBuilder(
                        schemaRestrictions);
        equals.forEach(restrictionsBuilder::addAttributeValueRestriction);

        PresentProofRequest.ProofRequest.ProofRequestedAttributes.ProofRequestedAttributesBuilder builder = PresentProofRequest.ProofRequest.ProofRequestedAttributes
                .builder()
                .names(names)
                .restriction(restrictionsBuilder.schemaId(schemaId).build().toJsonObject());

        builderSink.accept(schemaId, revocationApplicator.applyOn(builder).build());

    }
}
