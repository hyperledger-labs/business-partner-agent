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

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.api.present_proof.PresentProofRequest;
import org.hyperledger.bpa.api.exception.PartnerException;
import org.hyperledger.bpa.impl.prooftemplates.*;
import org.hyperledger.bpa.model.BPAProofTemplate;
import org.hyperledger.bpa.model.Partner;
import org.hyperledger.bpa.repository.PartnerRepository;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.Valid;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.UnaryOperator;

@Slf4j
@Singleton
public class ProofTemplateConverion {

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    ProofTemplateConditionOperators<PresentProofRequest.ProofRequest.ProofRequestedPredicates.ProofRequestedPredicatesBuilder, PresentProofRequest.ProofRequest.ProofRequestedAttributes.ProofRequestedAttributesBuilder, PresentProofRequest.ProofRequest.ProofRestrictions.ProofRestrictionsBuilder> ariesConditionOperators;

    public PresentProofRequest proofRequestFrom(@NonNull UUID partnerId,
            @NonNull @Valid BPAProofTemplate proofTemplate) {
        final Partner partner = partnerRepo.findById(partnerId)
                .orElseThrow(() -> new PartnerException("Partner not found"));
        if (!partner.hasConnectionId()) {
            throw new PartnerException("Partner has no aca-py connection");
        }

        RequestBuilderMap<ProofTemplateRequestBuilder<PresentProofRequest.ProofRequest.ProofRequestedPredicates.ProofRequestedPredicatesBuilder, PresentProofRequest.ProofRequest.ProofRequestedAttributes.ProofRequestedAttributesBuilder, PresentProofRequest.ProofRequest.ProofRestrictions.ProofRestrictionsBuilder>> builderSet = new RequestBuilderMap<>(
                this::proofTemplateRequestBuilderSupplier);
        ProofTemplateConditionContext.forTemplate(proofTemplate, builderSet::getNewBuilder)
                .map(resolveOperator(ariesConditionOperators::getConditionOperatorFor))
                .sorted()
                .forEach(ProofTemplateConditionContext::applyOnBuilder);
        PresentProofRequest.ProofRequest.ProofRequestBuilder proofRequestBuilder = PresentProofRequest.ProofRequest
                .builder();
        builderSet.getBuilders().forEach(builder -> {
            builder.getValue()
                    .attributeStream((attr, restr) -> attr.restriction(restr.build().toJsonObject()))
                    .forEach(attr -> proofRequestBuilder.requestedAttribute(builder.getKey(), attr.build()));
            builder.getValue()
                    .predicateStream((pred, restr) -> pred.restriction(restr.build().toJsonObject()))
                    .forEach(pred -> proofRequestBuilder.requestedPredicate(builder.getKey(), pred.build()));
        });
        return PresentProofRequest.builder()
                .proofRequest(proofRequestBuilder.build())
                .connectionId(partner.getConnectionId())
                .build();
    }

    ProofTemplateRequestBuilder<PresentProofRequest.ProofRequest.ProofRequestedPredicates.ProofRequestedPredicatesBuilder, PresentProofRequest.ProofRequest.ProofRequestedAttributes.ProofRequestedAttributesBuilder, PresentProofRequest.ProofRequest.ProofRestrictions.ProofRestrictionsBuilder> proofTemplateRequestBuilderSupplier() {
        return new ProofTemplateRequestBuilder<>(
                name -> PresentProofRequest.ProofRequest.ProofRequestedPredicates.builder().name(name),
                name -> PresentProofRequest.ProofRequest.ProofRequestedAttributes.builder().name(name),
                PresentProofRequest.ProofRequest.ProofRestrictions::builder);
    }

    static <T> UnaryOperator<ProofTemplateConditionContext<T>> resolveOperator(
            Function<String, Optional<ProofTemplateConditionOperator<T>>> resolver) {
        return context -> resolver.apply(context.getConditionOperatorString()).map(context::withConditionOperator)
                .orElse(context);
    }
}
