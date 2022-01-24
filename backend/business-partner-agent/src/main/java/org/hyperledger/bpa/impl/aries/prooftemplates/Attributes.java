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
package org.hyperledger.bpa.impl.aries.prooftemplates;

import lombok.Builder;
import lombok.Singular;
import org.hyperledger.aries.api.present_proof.PresentProofRequest;
import org.hyperledger.bpa.model.prooftemplate.BPASchemaRestrictions;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Builder
class Attributes {
    String schemaId;
    NonRevocationApplicator revocationApplicator;
    List<BPASchemaRestrictions> schemaRestrictions;
    @Singular
    List<String> names;
    @Singular
    Map<String, String> equals;

    public void addToBuilder(
            BiConsumer<String, PresentProofRequest.ProofRequest.ProofRequestedAttributes> builderSink) {
        List<PresentProofRequest.ProofRequest.ProofRestrictions.ProofRestrictionsBuilder> restrictionsBuilder = ProofTemplateElementVisitor
                .asProofRestrictionsBuilder(
                        schemaRestrictions);
        restrictionsBuilder.forEach(r -> equals.forEach(r::addAttributeValueRestriction));

        PresentProofRequest.ProofRequest.ProofRequestedAttributes.ProofRequestedAttributesBuilder builder = PresentProofRequest.ProofRequest.ProofRequestedAttributes
                .builder()
                .names(names)
                // TODO only set when set in restriction
                .restrictions(restrictionsBuilder.stream().map(res -> res.schemaId(schemaId).build().toJsonObject())
                        .collect(Collectors.toList()));

        builderSink.accept(schemaId, revocationApplicator.applyOn(builder).build());

    }
}
