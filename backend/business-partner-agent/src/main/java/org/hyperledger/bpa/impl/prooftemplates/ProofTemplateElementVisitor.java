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

import org.hyperledger.aries.api.present_proof.PresentProofRequest;
import org.hyperledger.bpa.model.*;
import org.hyperledger.bpa.model.prooftemplate.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.stream.Stream;

public class ProofTemplateElementVisitor {

    private Function<String, Optional<String>> resolveLedgerSchemaId;
    private final RevocationTimeStampProvider revocationTimeStampProvider;
    private static final NonRevocationApplicator DEFAULT_NON_REVOCATION = new NonRevocationApplicator(false, null);

    private final Map<String, Attributes.AttributesBuilder> attributesBySchemaId = new HashMap<>();
    private final Map<String, List<Predicate.PredicateBuilder>> predicatesBySchemaId = new HashMap<>();
    private final Map<String, BPASchemaRestrictions> schemaRestrictions = new HashMap<>();
    private final Map<String, NonRevocationApplicator> nonRevocationApplicatorMap = new HashMap<>();
    private final Map<String, AtomicInteger> sameSchemaCounters = new HashMap<>();
    private String templateName;

    static PresentProofRequest.ProofRequest.ProofRestrictions.ProofRestrictionsBuilder asProofRestrictionsBuilder(
            BPASchemaRestrictions schemaRestrictions) {
        return PresentProofRequest.ProofRequest.ProofRestrictions.builder()
                .schemaId(schemaRestrictions.getSchemaId())
                .schemaName(schemaRestrictions.getSchemaName())
                .schemaVersion(schemaRestrictions.getSchemaVersion())
                .schemaIssuerDid(schemaRestrictions.getSchemaIssuerDid())
                .credentialDefinitionId(schemaRestrictions.getCredentialDefinitionId())
                .issuerDid(schemaRestrictions.getIssuerDid());
    }

    private NonRevocationApplicator getRevocationApplicator(Pair<String, ?> schemaAndAttributesBuilder) {
        return nonRevocationApplicatorMap.computeIfAbsent(schemaAndAttributesBuilder.getLeft(),
                schemaId -> DEFAULT_NON_REVOCATION);
    }

    private BPASchemaRestrictions getSchemaRestrictions(Pair<String, ?> schemaAndAttributesBuilder) {
        return schemaRestrictions.computeIfAbsent(
                schemaAndAttributesBuilder.getLeft(),
                schemaId -> BPASchemaRestrictions.builder().build());
    }

    private AtomicInteger getSameSchemaCounter(Pair<String, ?> schemaAndAttributesBuilder) {
        return sameSchemaCounters.computeIfAbsent(schemaAndAttributesBuilder.getLeft(), s -> new AtomicInteger(0));
    }

    public ProofTemplateElementVisitor(
            Function<String, Optional<String>> resolveLedgerSchemaId,
            RevocationTimeStampProvider revocationTimeStampProvider) {
        this.resolveLedgerSchemaId = resolveLedgerSchemaId;
        this.revocationTimeStampProvider = revocationTimeStampProvider;
    }

    public void visit(BPAProofTemplate bpaProofTemplate) {
        templateName = bpaProofTemplate.getName();
    }

    public void visit(BPAAttributeGroup bpaAttributeGroup) {
        resolveLedgerSchemaId.apply(bpaAttributeGroup.getSchemaId()).ifPresent(ledgerSchemaId -> {
            nonRevocationApplicatorMap.put(ledgerSchemaId, NonRevocationApplicator.builder()
                    .applyNonRevocation(bpaAttributeGroup.getNonRevoked())
                    .revocationTimeStampProvider(revocationTimeStampProvider)
                    .build());
            schemaRestrictions.put(ledgerSchemaId, bpaAttributeGroup.getSchemaLevelRestrictions());
        });
    }

    public void visit(Pair<String, BPAAttribute> schemaIdAndBpaAttribute) {
        String schemaId = schemaIdAndBpaAttribute.getLeft();
        BPAAttribute attribute = schemaIdAndBpaAttribute.getRight();
        if (shouldAddAsAttribute(attribute)) {
            attributesBySchemaId.compute(schemaId, addAttribute(attribute));
        }
        if (shouldAddAsPredicate(attribute)) {
            predicatesBySchemaId.compute(schemaId, addPredicates(attribute));
        }
    }

    private BiFunction<String, Attributes.AttributesBuilder, Attributes.AttributesBuilder> addAttribute(
            BPAAttribute attribute) {
        return (schemaId, builder) -> {
            Optional<String> equalsValue = attribute.getConditions().stream()
                    .map(Pair.with(BPACondition::getValue, BPACondition::getOperator))
                    .filter(Pair.filterRight(ValueOperators.EQUALS::equals))
                    .findAny()
                    .map(Pair::getLeft);

            Attributes.AttributesBuilder result = Optional.ofNullable(builder).orElseGet(Attributes::builder)
                    .schemaId(schemaId)
                    .name(attribute.getName());
            equalsValue.ifPresent(value -> result.equal(attribute.getName(), value));
            return result;
        };
    }

    private BiFunction<String, List<Predicate.PredicateBuilder>, List<Predicate.PredicateBuilder>> addPredicates(
            BPAAttribute attribute) {
        return (schemaId, predicateBuilderList) -> {
            List<Predicate.PredicateBuilder> buildersList = Optional.ofNullable(predicateBuilderList)
                    .orElseGet(ArrayList::new);
            attribute.getConditions().stream()
                    .map(Pair.with(BPACondition::getValue, BPACondition::getOperator))
                    .flatMap(Pair.streamMapRight(o -> o.getPredicateOperator().stream()))
                    .flatMap(Pair.streamMapLeft(this::mapValueToInteger))
                    .map(valueAndOperator -> Predicate.builder()
                            .schemaId(schemaId)
                            .name(attribute.getName())
                            .operator(valueAndOperator.getRight())
                            .value(valueAndOperator.getLeft()))
                    .forEach(buildersList::add);
            return buildersList;
        };
    }

    private Stream<Integer> mapValueToInteger(String value) {
        try {
            return Optional.ofNullable(value).map(Integer::parseInt).stream();
        } catch (NumberFormatException e) {
            // TODO log error?
            return Stream.empty();
        }
    }

    private boolean shouldAddAsAttribute(BPAAttribute attribute) {
        return attribute.getConditions().isEmpty() || attribute.getConditions().stream()
                .map(BPACondition::getOperator).anyMatch(ValueOperators.EQUALS::equals);
    }

    private boolean shouldAddAsPredicate(BPAAttribute attribute) {
        return attribute.getConditions()
                .stream()
                .map(BPACondition::getOperator)
                .anyMatch(ValueOperators::handleAsPredicate);
    }

    public PresentProofRequest.ProofRequest getResult() {
        PresentProofRequest.ProofRequest.ProofRequestBuilder proofRequestBuilder = PresentProofRequest.ProofRequest
                .builder()
                .name(templateName);

        addAttributesTo(proofRequestBuilder);
        addPredicates(proofRequestBuilder);
        return proofRequestBuilder.build();
    }

    public void addAttributesTo(PresentProofRequest.ProofRequest.ProofRequestBuilder proofRequestBuilder) {
        attributesBySchemaId.entrySet().stream()
                .map(Pair::new)
                .map(Pair.lookUpAndSetOnRight(this::getRevocationApplicator, builder -> builder::revocationApplicator))
                .map(Pair.lookUpAndSetOnRight(this::getSchemaRestrictions, builder -> builder::schemaRestrictions))
                .map(Pair::getRight)
                .map(Attributes.AttributesBuilder::build)
                .forEach(attributes -> attributes.addToBuilder(proofRequestBuilder::requestedAttribute));
    }

    public void addPredicates(PresentProofRequest.ProofRequest.ProofRequestBuilder proofRequestBuilder) {
        predicatesBySchemaId.entrySet().stream()
                .map(Pair::new)
                .flatMap(Pair.streamMapRight(List::stream))
                .map(Pair.lookUpAndSetOnRight(this::getRevocationApplicator, builder -> builder::revocationApplicator))
                .map(Pair.lookUpAndSetOnRight(this::getSchemaRestrictions, builder -> builder::schemaRestrictions))
                .map(Pair.lookUpAndSetOnRight(this::getSameSchemaCounter, builder -> builder::sameSchemaCounter))
                .map(Pair::getRight)
                .map(Predicate.PredicateBuilder::build)
                .forEach(attributes -> attributes.addToBuilder(proofRequestBuilder::requestedPredicate));
    }

}
