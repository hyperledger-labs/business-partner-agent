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

import io.micronaut.core.util.CollectionUtils;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import org.hyperledger.aries.api.present_proof.PresentProofRequest.ProofRequest.ProofRequestedAttributes;
import org.hyperledger.aries.api.present_proof.PresentProofRequest.ProofRequest.ProofRestrictions.ProofRestrictionsBuilder;
import org.hyperledger.bpa.persistence.model.prooftemplate.BPASchemaRestrictions;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Data
@Builder
public class Attributes {
    private String schemaId;
    private NonRevocationApplicator revocationApplicator;
    private List<BPASchemaRestrictions> schemaRestrictions;
    @Singular
    private List<String> names;
    @Singular
    private Map<String, String> equals;

    public void addToBuilder(BiConsumer<String, ProofRequestedAttributes> builderSink) {
        List<ProofRestrictionsBuilder> restrictionsBuilder = ProofTemplateElementVisitor
                .asProofRestrictionsBuilder(schemaRestrictions);

        ProofRequestedAttributes.ProofRequestedAttributesBuilder builder = ProofRequestedAttributes.builder()
                .names(names);

        if (CollectionUtils.isNotEmpty(restrictionsBuilder)) {
            restrictionsBuilder.forEach(r -> equals.forEach(r::addAttributeValueRestriction));
            builder.restrictions(restrictionsBuilder.stream().map(res -> res.build().toJsonObject())
                    .collect(Collectors.toList()));
        }

        builderSink.accept(schemaId, revocationApplicator.applyOn(builder).build());
    }
}
