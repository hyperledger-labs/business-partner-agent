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

import io.micronaut.context.annotation.Factory;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.acy_py.generated.model.IndyProofReqPredSpec;
import org.hyperledger.aries.api.present_proof.PresentProofRequest;
import org.hyperledger.bpa.impl.prooftemplates.ProofTemplateConditionOperator;
import org.hyperledger.bpa.impl.prooftemplates.ProofTemplateConditionOperators;
import org.hyperledger.bpa.impl.prooftemplates.ProofTemplateRequestBuilder;
import org.jetbrains.annotations.NotNull;

import javax.inject.Singleton;
import java.util.Optional;

@Slf4j
@Factory
public class AriesProofTemplateConditionOperatorFactory {

    @Singleton
    ProofTemplateConditionOperators<
            PresentProofRequest.ProofRequest.ProofRequestedPredicates.ProofRequestedPredicatesBuilder,
            PresentProofRequest.ProofRequest.ProofRequestedAttributes.ProofRequestedAttributesBuilder,
            PresentProofRequest.ProofRequest.ProofRestrictions.ProofRestrictionsBuilder
            >
    proofTemplateOperators() {
        ProofTemplateConditionOperators<
                PresentProofRequest.ProofRequest.ProofRequestedPredicates.ProofRequestedPredicatesBuilder,
                PresentProofRequest.ProofRequest.ProofRequestedAttributes.ProofRequestedAttributesBuilder,
                PresentProofRequest.ProofRequest.ProofRestrictions.ProofRestrictionsBuilder
                > knownOperators;
        knownOperators = new ProofTemplateConditionOperators<>();
        knownOperators.put(null, noneOperator());
        knownOperators.put(IndyProofReqPredSpec.PTypeEnum.LESS_THAN.getValue(), lessThanOperator());
        knownOperators.put(IndyProofReqPredSpec.PTypeEnum.LESS_THAN_OR_EQUAL_TO.getValue(), lessThanEqualsOperator());
        knownOperators.put(IndyProofReqPredSpec.PTypeEnum.GREATER_THAN.getValue(), greaterThanOperator());
        knownOperators.put(IndyProofReqPredSpec.PTypeEnum.GREATER_THAN_OR_EQUAL_TO.getValue(), greaterThanEqualsOperator());
        knownOperators.put(ProofTemplateConditionOperators.EQUALS_OPERATOR_STRING, equalsOperator());
        knownOperators.put(ProofTemplateConditionOperators.SCHEMA_ID_OPERATOR_STRING, schemaIdOperator());
        knownOperators.put(ProofTemplateConditionOperators.NON_REVOKED_OPERATOR_STRING, nonRevokedOperator());
        // TODO support 'issued-by'
        return knownOperators;
    }

    public interface AttributeOperator extends ProofTemplateConditionOperator<
            ProofTemplateRequestBuilder<
                    PresentProofRequest.ProofRequest.ProofRequestedPredicates.ProofRequestedPredicatesBuilder,
                    PresentProofRequest.ProofRequest.ProofRequestedAttributes.ProofRequestedAttributesBuilder,
                    PresentProofRequest.ProofRequest.ProofRestrictions.ProofRestrictionsBuilder
                    >
            > {
        @Override
        default boolean attributeOnlyLevel() {
            return true;
        }
    }

    public interface AttributeAndAttributeGroupOperator extends ProofTemplateConditionOperator<
            ProofTemplateRequestBuilder<
                    PresentProofRequest.ProofRequest.ProofRequestedPredicates.ProofRequestedPredicatesBuilder,
                    PresentProofRequest.ProofRequest.ProofRequestedAttributes.ProofRequestedAttributesBuilder,
                    PresentProofRequest.ProofRequest.ProofRestrictions.ProofRestrictionsBuilder
                    >
            > {
        @Override
        default boolean attributeOnlyLevel() {
            return false;
        }
    }

    AttributeAndAttributeGroupOperator schemaIdOperator() {
        return (proofRequestBuilder, name, value) ->
                proofRequestBuilder.putRestriction(name,
                        restrictions -> restrictions.schemaId(value)
                );
    }

    AttributeOperator nonRevokedOperator() {
        return (proofRequestBuilder, name, value) -> {

            Optional<PresentProofRequest.ProofRequest.ProofNonRevoked> revoked = toNonRevoked(value);
            proofRequestBuilder.onAttribute(name, attr -> {

                revoked.ifPresentOrElse(attr::nonRevoked, () ->
                {
                    // FIXME add this to ProofRequest
                });

                return attr;
            });
        };
    }

    @NotNull
    private Optional<PresentProofRequest.ProofRequest.ProofNonRevoked> toNonRevoked(String value) {
        try {
            Long longValue = Long.decode(value);
            return Optional.of(
                    PresentProofRequest.ProofRequest.ProofNonRevoked.builder()
                            .from(longValue)
                            .to(longValue)
                            .build()
            );
        } catch (NumberFormatException e) {
            log.error("non revocation operator need a number value.", e);

        }
        return Optional.empty();
    }


    AttributeOperator noneOperator() {
        return (proofRequestBuilder, name, value) -> {
            log.warn("The attribute {} was added without any restrictions, use at least a schema restriction.", name);
            proofRequestBuilder.addAttribute(name);
        };
    }


    AttributeOperator equalsOperator() {
        return (proofRequestBuilder, name, value) -> {
            if (name == null || value == null) {
                throw new RuntimeException("equals conditions require an attribute name and a value.");
            }
            proofRequestBuilder.addAttribute(name);
            proofRequestBuilder.putRestriction(name, restriction ->
                    restriction.addAttributeValueRestriction(name, value)
            );
        };
    }

    AttributeOperator lessThanOperator() {
        return (proofRequestBuilder, name, value) ->
                predicateRelation(proofRequestBuilder, name, value, IndyProofReqPredSpec.PTypeEnum.LESS_THAN);
    }

    AttributeOperator lessThanEqualsOperator() {
        return (proofRequestBuilder, name, value) ->
                predicateRelation(proofRequestBuilder, name, value, IndyProofReqPredSpec.PTypeEnum.LESS_THAN_OR_EQUAL_TO);
    }

    AttributeOperator greaterThanOperator() {
        return (proofRequestBuilder, name, value) ->
                predicateRelation(proofRequestBuilder, name, value, IndyProofReqPredSpec.PTypeEnum.GREATER_THAN);
    }

    AttributeOperator greaterThanEqualsOperator() {
        return (proofRequestBuilder, name, value) ->
                predicateRelation(proofRequestBuilder, name, value, IndyProofReqPredSpec.PTypeEnum.GREATER_THAN_OR_EQUAL_TO);
    }

    private void predicateRelation(
            @NonNull ProofTemplateRequestBuilder<
                    PresentProofRequest.ProofRequest.ProofRequestedPredicates.ProofRequestedPredicatesBuilder,
                    PresentProofRequest.ProofRequest.ProofRequestedAttributes.ProofRequestedAttributesBuilder,
                    PresentProofRequest.ProofRequest.ProofRestrictions.ProofRestrictionsBuilder
                    > proofRequestBuilder,
            @Nullable String name,
            @Nullable String value,
            @NonNull IndyProofReqPredSpec.PTypeEnum greaterThanOrEqualTo) {
        if (name == null || value == null) {
            throw new RuntimeException("predicate conditions require an attribute name and a value.");
        }
        proofRequestBuilder.addPredicate(name);
        proofRequestBuilder.onPredicate(name, pred ->
                pred.pType(greaterThanOrEqualTo).pValue(value)
        );
    }
}
