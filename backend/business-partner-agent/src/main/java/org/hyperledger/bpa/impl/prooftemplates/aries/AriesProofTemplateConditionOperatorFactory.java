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

import io.micronaut.context.annotation.Factory;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.acy_py.generated.model.IndyProofReqPredSpec;
import org.hyperledger.aries.api.present_proof.PresentProofRequest;
import org.hyperledger.bpa.impl.prooftemplates.ProofTemplateConditionOperator;
import org.hyperledger.bpa.impl.prooftemplates.ProofTemplateConditionOperators;

import javax.inject.Singleton;
import java.time.Clock;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
@Factory
public class AriesProofTemplateConditionOperatorFactory {

    @Singleton
    Clock systemClock() {
        return Clock.systemUTC();
    }

    @Singleton
    ProofTemplateConditionOperators<AriesProofTemplateRequestBuilder> proofTemplateOperators() {
        ProofTemplateConditionOperators<AriesProofTemplateRequestBuilder> knownOperators;
        knownOperators = new ProofTemplateConditionOperators<>();
        knownOperators.put(IndyProofReqPredSpec.PTypeEnum.LESS_THAN.getValue(), lessThanOperator());
        knownOperators.put(IndyProofReqPredSpec.PTypeEnum.LESS_THAN_OR_EQUAL_TO.getValue(), lessThanEqualsOperator());
        knownOperators.put(IndyProofReqPredSpec.PTypeEnum.GREATER_THAN.getValue(), greaterThanOperator());
        knownOperators.put(IndyProofReqPredSpec.PTypeEnum.GREATER_THAN_OR_EQUAL_TO.getValue(),
                greaterThanEqualsOperator());
        knownOperators.put(ProofTemplateConditionOperators.ISSUED_BY_OPERATOR_STRING, issuedByRestrictionOperator());
        knownOperators.put(ProofTemplateConditionOperators.EQUALS_OPERATOR_STRING, equalsOperator());
        knownOperators.put(ProofTemplateConditionOperators.SCHEMA_ID_OPERATOR_STRING, schemaIdRestrictionOperator());
        knownOperators.put(ProofTemplateConditionOperators.NON_REVOKED_OPERATOR_STRING, nonRevokedOperator());
        return knownOperators;
    }

    public interface AttributeOperator extends
            ProofTemplateConditionOperator<AriesProofTemplateRequestBuilder> {
        @Override
        default boolean attributeOnlyLevel() {
            return true;
        }

        void applyOnBuilder(
                @NonNull AriesProofTemplateRequestBuilder builder,
                @NonNull String name,
                @NonNull String value);
    }

    public interface PredicateCreatingOperator
            extends ProofTemplateConditionOperator<AriesProofTemplateRequestBuilder> {
        @Override
        default boolean attributeOnlyLevel() {
            return true;
        }

        void applyOnBuilder(
                @NonNull AriesProofTemplateRequestBuilder builder,
                @NonNull String name,
                @NonNull String value);

        @Override
        default boolean isPredicate() {
            return true;
        }
    }

    public interface AttributeAndAttributeGroupOperator extends
            ProofTemplateConditionOperator<AriesProofTemplateRequestBuilder> {
        @Override
        default boolean attributeOnlyLevel() {
            return false;
        }
    }

    AttributeAndAttributeGroupOperator schemaIdRestrictionOperator() {
        return (proofRequestBuilder, name, value) -> proofRequestBuilder.putRestriction(name,
                restrictions -> restrictions.schemaId(value));
    }

    AttributeAndAttributeGroupOperator issuedByRestrictionOperator() {
        return (proofRequestBuilder, name, value) -> proofRequestBuilder.putRestriction(name,
                restrictions -> restrictions.issuerDid(value));
    }

    AttributeAndAttributeGroupOperator nonRevokedOperator() {
        return (proofRequestBuilder, name, value) -> {
            proofRequestBuilder.onAttribute(name, attr -> setNonRevoked(value, attr::nonRevoked)
                    .orElseGet(() -> {
                        log.error("Non-Revocation check was not added to " + name);
                        return attr;
                    }));
            proofRequestBuilder.onPredicate(name, pred -> setNonRevoked(value, pred::nonRevoked)
                    .orElseGet(() -> {
                        log.error("Non-Revocation check was not added to " + name);
                        return pred;
                    }));
        };
    }

    private <T> Optional<T> setNonRevoked(
            @Nullable String value,
            @NonNull Function<PresentProofRequest.ProofRequest.ProofNonRevoked, T> nonRevokedTarget) {
        Optional<PresentProofRequest.ProofRequest.ProofNonRevoked> nonRevoked = Optional.empty();
        if (value != null) {
            try {
                Long longValue = Long.decode(value);
                nonRevoked = Optional.of(
                        PresentProofRequest.ProofRequest.ProofNonRevoked.builder()
                                .from(longValue)
                                .to(longValue)
                                .build());
            } catch (NumberFormatException e) {
                log.error("non revocation operator need a number value.", e);
            }
        }
        return nonRevoked.map(nonRevokedTarget);
    }

    AttributeOperator equalsOperator() {
        return (proofRequestBuilder, name, value) -> {
            proofRequestBuilder.addAttribute(name);
            proofRequestBuilder.putRestriction(name,
                    restriction -> restriction.addAttributeValueRestriction(name, value));
        };
    }

    PredicateCreatingOperator lessThanOperator() {
        return (proofRequestBuilder, name, value) -> predicateRelation(proofRequestBuilder, name, value,
                IndyProofReqPredSpec.PTypeEnum.LESS_THAN);
    }

    PredicateCreatingOperator lessThanEqualsOperator() {
        return (proofRequestBuilder, name, value) -> predicateRelation(proofRequestBuilder, name, value,
                IndyProofReqPredSpec.PTypeEnum.LESS_THAN_OR_EQUAL_TO);
    }

    PredicateCreatingOperator greaterThanOperator() {
        return (proofRequestBuilder, name, value) -> predicateRelation(proofRequestBuilder, name, value,
                IndyProofReqPredSpec.PTypeEnum.GREATER_THAN);
    }

    PredicateCreatingOperator greaterThanEqualsOperator() {
        return (proofRequestBuilder, name, value) -> predicateRelation(proofRequestBuilder, name, value,
                IndyProofReqPredSpec.PTypeEnum.GREATER_THAN_OR_EQUAL_TO);
    }

    private void predicateRelation(
            @NonNull AriesProofTemplateRequestBuilder proofRequestBuilder,
            @NonNull String name,
            @NonNull String value,
            @NonNull IndyProofReqPredSpec.PTypeEnum greaterThanOrEqualTo) {
        proofRequestBuilder.addPredicate(name);
        proofRequestBuilder.onPredicate(name, pred -> pred.pType(greaterThanOrEqualTo).pValue(Integer.parseInt(value)));
    }
}
