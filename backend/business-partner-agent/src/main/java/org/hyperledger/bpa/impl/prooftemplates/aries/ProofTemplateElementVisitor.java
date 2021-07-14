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
import org.hyperledger.bpa.impl.prooftemplates.ProofTemplateConditionOperator;
import org.hyperledger.bpa.impl.prooftemplates.RevocationTimeStampProvider;
import org.hyperledger.bpa.model.*;
import org.hyperledger.bpa.model.prooftemplate.BPAAttribute;
import org.hyperledger.bpa.model.prooftemplate.BPAAttributeGroup;
import org.hyperledger.bpa.model.prooftemplate.BPACondition;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.hyperledger.bpa.impl.prooftemplates.ProofTemplateConditionOperators.NON_REVOKED_OPERATOR_STRING;
import static org.hyperledger.bpa.impl.prooftemplates.ProofTemplateConditionOperators.SCHEMA_ID_OPERATOR_STRING;

public class ProofTemplateElementVisitor {

    private final RevocationTimeStampProvider revocationTimeStampProvider;
    private final Function<String, Optional<ProofTemplateConditionOperator<AriesProofTemplateRequestBuilder>>> operatorResolver;
    private final Map<String, AriesProofTemplateRequestBuilder> builderWrapperBySchemaId = new HashMap<>();
    private PresentProofRequest.ProofRequest.ProofRequestBuilder proofRequestBuilder = PresentProofRequest.ProofRequest
            .builder();

    public ProofTemplateElementVisitor(
            Function<String, Optional<ProofTemplateConditionOperator<AriesProofTemplateRequestBuilder>>> operatorResolver,
            RevocationTimeStampProvider revocationTimeStampProvider) {
        this.operatorResolver = operatorResolver;
        this.revocationTimeStampProvider = revocationTimeStampProvider;
    }

    public void visit(BPAProofTemplate bpaProofTemplate) {
        proofRequestBuilder = applyProofTemplateToBuilder(bpaProofTemplate);
    }

    private PresentProofRequest.ProofRequest.ProofRequestBuilder applyProofTemplateToBuilder(
            BPAProofTemplate bpaProofTemplate) {
        return proofRequestBuilder.name(bpaProofTemplate.getName());
    }

    public void visit(BPAAttributeGroup bpaAttributeGroup) {
        builderWrapperBySchemaId.computeIfAbsent(bpaAttributeGroup.getSchemaId(),
                name -> new AriesProofTemplateRequestBuilder());
        Stream<BPACondition> schemaLevelConditions = bpaAttributeGroup.getSchemaLevelConditions().stream();
        String schemaId = bpaAttributeGroup.getSchemaId();
        prependSchemaIdCondition(bpaAttributeGroup.getSchemaId(), schemaLevelConditions)
                .map(bpaCondition -> new Pair<>(bpaCondition.getValue(), bpaCondition.getOperator()))
                .map(this::presetNonRevocationDateIfAbsent)
                .map(Pair.flatMapRight(this.operatorResolver))
                .flatMap(Optional::stream)
                .filter(Pair.filterRight(Predicate.not(ProofTemplateConditionOperator::attributeOnlyLevel)))
                .forEach(valueAndConditionOperator -> builderWrapperBySchemaId.compute(schemaId,
                        applyOperatorOnBuilder(valueAndConditionOperator, null)));
    }

    @NonNull
    private BiFunction<String, AriesProofTemplateRequestBuilder, AriesProofTemplateRequestBuilder> applyOperatorOnBuilder(
            @NonNull Pair<String, ProofTemplateConditionOperator<AriesProofTemplateRequestBuilder>> valueAndConditionOperator,
            String attributeName) {
        return (key, builder) -> valueAndConditionOperator.getRight().applicatorFor(
                attributeName, valueAndConditionOperator.getLeft()).apply(builder);
    }

    public void visit(Pair<String, BPAAttribute> schemaIdAndBpaAttribute) {
        String schemaId = schemaIdAndBpaAttribute.getLeft();
        builderWrapperBySchemaId.computeIfAbsent(schemaId,
                name -> new AriesProofTemplateRequestBuilder());
        BPAAttribute attribute = schemaIdAndBpaAttribute.getRight();
        if (doesNotContainAnyCondition(attribute) && doesNotContainAnyPredicateCondition(attribute)) {
            builderWrapperBySchemaId.get(schemaId).addAttribute(attribute.getName());
        }
        attribute.getConditions().stream()
                .map(bpaCondition -> new Pair<>(bpaCondition.getValue(), bpaCondition.getOperator()))
                .map(this::presetNonRevocationDateIfAbsent)
                .map(Pair.flatMapRight(this.operatorResolver))
                .flatMap(Optional::stream)
                .forEach(valueAndConditionOperator -> builderWrapperBySchemaId.compute(schemaId,
                        applyOperatorOnBuilder(valueAndConditionOperator, attribute.getName())));
    }

    private boolean doesNotContainAnyCondition(BPAAttribute attribute) {
        return attribute.getConditions().isEmpty();
    }

    private boolean doesNotContainAnyPredicateCondition(BPAAttribute attribute) {
        return attribute.getConditions()
                .stream()
                .map(BPACondition::getOperator)
                .map(this.operatorResolver)
                .flatMap(Optional::stream)
                .noneMatch(ProofTemplateConditionOperator::isPredicate);
    }

    public PresentProofRequest.ProofRequest getResult() {
        builderWrapperBySchemaId.forEach(this::writeAttributesToBuilder);
        builderWrapperBySchemaId.forEach(this::wrtiePredicatesToBuilder);
        return proofRequestBuilder.build();
    }

    private void wrtiePredicatesToBuilder(String schemaId, AriesProofTemplateRequestBuilder builder) {
        builder.predicateStream()
                .forEach(pred -> proofRequestBuilder.requestedPredicate(schemaId, pred.build()));
    }

    private void writeAttributesToBuilder(String schemaId, AriesProofTemplateRequestBuilder builder) {
        builder.attributeStream()
                .forEach(attr -> proofRequestBuilder.requestedAttribute(schemaId, attr.build()));
    }

    private Pair<String, String> presetNonRevocationDateIfAbsent(Pair<String, String> valueAndConditionString) {
        if (NON_REVOKED_OPERATOR_STRING.equals(valueAndConditionString.getRight())
                && valueAndConditionString.getLeft() == null) {
            return valueAndConditionString.withLeft(revocationTimeStampProvider.get().toString());
        }
        return valueAndConditionString;
    }

    Stream<BPACondition> prependSchemaIdCondition(String schemaId, Stream<BPACondition> conditions) {
        BPACondition schemaIdCondition = BPACondition.builder()
                .operator(SCHEMA_ID_OPERATOR_STRING)
                .value(schemaId)
                .build();
        return Stream.concat(Stream.of(schemaIdCondition), conditions);
    }
}
