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
package org.hyperledger.bpa.impl.prooftemplates.aries;

import io.micronaut.core.annotation.NonNull;
import org.hyperledger.aries.api.present_proof.PresentProofRequest;
import org.hyperledger.bpa.impl.prooftemplates.ProofTemplateRequestBuilder;

import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class AriesProofTemplateRequestBuilder extends
        ProofTemplateRequestBuilder<PresentProofRequest.ProofRequest.ProofRequestedPredicates.ProofRequestedPredicatesBuilder, PresentProofRequest.ProofRequest.ProofRequestedAttributes.ProofRequestedAttributesBuilder, PresentProofRequest.ProofRequest.ProofRestrictions.ProofRestrictionsBuilder> {
    private final Set<String> attributeNames = new HashSet<>();
    private PresentProofRequest.ProofRequest.ProofRequestedAttributes.ProofRequestedAttributesBuilder attributes = PresentProofRequest.ProofRequest.ProofRequestedAttributes
            .builder();

    public AriesProofTemplateRequestBuilder() {
        super(name -> PresentProofRequest.ProofRequest.ProofRequestedPredicates.builder().name(name),
                name -> PresentProofRequest.ProofRequest.ProofRequestedAttributes.builder().name(name),
                PresentProofRequest.ProofRequest.ProofRestrictions::builder);
    }

    public PresentProofRequest.ProofRequest.ProofRequestedAttributes.ProofRequestedAttributesBuilder joinAttributeWithRestrictions(
            PresentProofRequest.ProofRequest.ProofRequestedAttributes.ProofRequestedAttributesBuilder attribute,
            PresentProofRequest.ProofRequest.ProofRestrictions.ProofRestrictionsBuilder restrictions) {
        return attribute.restriction(restrictions.build().toJsonObject());
    }

    public PresentProofRequest.ProofRequest.ProofRequestedPredicates.ProofRequestedPredicatesBuilder joinPredicateWithRestrictions(
            PresentProofRequest.ProofRequest.ProofRequestedPredicates.ProofRequestedPredicatesBuilder predicate,
            PresentProofRequest.ProofRequest.ProofRestrictions.ProofRestrictionsBuilder restrictions) {
        return predicate.restriction(restrictions.build().toJsonObject());
    }

    @Override
    public void addAttribute(String name) {
        attributeNames.add(name);
    }

    @Override
    public void onAttribute(String name,
            @NonNull UnaryOperator<PresentProofRequest.ProofRequest.ProofRequestedAttributes.ProofRequestedAttributesBuilder> modifier) {
        if (isOnSchemaOrKnownAttribute(name)) {
            attributes = modifier.apply(attributes);
        }
    }

    private boolean isOnSchemaOrKnownAttribute(String name) {
        return name == null || attributeNames.contains(name);
    }

    public Stream<PresentProofRequest.ProofRequest.ProofRequestedAttributes.ProofRequestedAttributesBuilder> attributeStream() {
        attributeNames
                .stream()
                .map(this::restrictionsFor)
                .flatMap(Optional::stream)
                .forEach(restriction -> attributes = this.joinAttributeWithRestrictions(attributes, restriction));
        return Stream.of(addSchemaLevelRestrictions().names(new ArrayList<>(attributeNames)));
    }

    private PresentProofRequest.ProofRequest.ProofRequestedAttributes.ProofRequestedAttributesBuilder addSchemaLevelRestrictions() {
        return this.restrictionsFor(null)
                .map(restriction -> this.joinAttributeWithRestrictions(attributes, restriction))
                .orElse(attributes);
    }
}