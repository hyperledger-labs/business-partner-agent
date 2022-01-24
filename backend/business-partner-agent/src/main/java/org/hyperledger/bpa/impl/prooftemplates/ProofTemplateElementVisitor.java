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
package org.hyperledger.bpa.impl.prooftemplates;

import org.hyperledger.aries.api.present_proof.PresentProofRequest;
import org.hyperledger.bpa.model.BPAProofTemplate;
import org.hyperledger.bpa.model.prooftemplate.*;
import org.hyperledger.bpa.impl.util.Pair;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class constructs a {@link PresentProofRequest.ProofRequest} from a
 * {@link BPAProofTemplate} by visiting the template's parts
 * {@link BPAAttributeGroup}s and {@link BPAAttribute}s. The visit methods
 * accept the different parts and store them in a context conserving way, to
 * build the {@link PresentProofRequest.ProofRequest} with {@link #getResult()}.
 * <p>
 * The call order of the visit methods does not matter.
 * <p>
 * These methods are not thread safe.
 * <p>
 * Use one instance per {@link BPAProofTemplate}
 *
 * @see ProofTemplateConversion#proofRequestViaVisitorFrom for an example on how
 *      to use this class
 */
class ProofTemplateElementVisitor {

    private final Function<String, Optional<String>> resolveLedgerSchemaId;
    private final RevocationTimeStampProvider revocationTimeStampProvider;
    private static final NonRevocationApplicator DEFAULT_NON_REVOCATION = new NonRevocationApplicator(false, null);

    private final Map<String, Attributes.AttributesBuilder> attributesBySchemaId = new HashMap<>();
    private final Map<String, List<Predicate.PredicateBuilder>> predicatesBySchemaId = new HashMap<>();
    private final Map<String, List<BPASchemaRestrictions>> schemaRestrictions = new HashMap<>();
    private final Map<String, NonRevocationApplicator> nonRevocationApplicatorMap = new HashMap<>();
    private final Map<String, AtomicInteger> sameSchemaCounters = new HashMap<>();
    private String templateName;

    static List<PresentProofRequest.ProofRequest.ProofRestrictions.ProofRestrictionsBuilder> asProofRestrictionsBuilder(
            List<BPASchemaRestrictions> schemaRestrictions) {
        return schemaRestrictions.stream().map(res -> PresentProofRequest.ProofRequest.ProofRestrictions.builder()
                .schemaId(res.getSchemaId())
                .schemaName(res.getSchemaName())
                .schemaVersion(res.getSchemaVersion())
                .schemaIssuerDid(res.getSchemaIssuerDid())
                .credentialDefinitionId(res.getCredentialDefinitionId())
                .issuerDid(res.getIssuerDid())).collect(Collectors.toList());
    }

    private NonRevocationApplicator getRevocationApplicator(Pair<String, ?> schemaAndAttributesBuilder) {
        return nonRevocationApplicatorMap.computeIfAbsent(schemaAndAttributesBuilder.getLeft(),
                schemaId -> DEFAULT_NON_REVOCATION);
    }

    private List<BPASchemaRestrictions> getSchemaRestrictions(Pair<String, ?> schemaAndAttributesBuilder) {
        return schemaRestrictions.computeIfAbsent(
                schemaAndAttributesBuilder.getLeft(),
                schemaId -> List.of(BPASchemaRestrictions.builder().build()));
    }

    private AtomicInteger getSameSchemaCounter(Pair<String, ?> schemaAndAttributesBuilder) {
        return sameSchemaCounters.computeIfAbsent(schemaAndAttributesBuilder.getLeft(), s -> new AtomicInteger(0));
    }

    ProofTemplateElementVisitor(
            Function<String, Optional<String>> resolveLedgerSchemaId,
            RevocationTimeStampProvider revocationTimeStampProvider) {
        this.resolveLedgerSchemaId = resolveLedgerSchemaId;
        this.revocationTimeStampProvider = revocationTimeStampProvider;
    }

    /**
     * Collects general information of the given {@link BPAProofTemplate}, e.g. the
     * template's name.
     * 
     * @param bpaProofTemplate contains general information to create a
     *                         {@link PresentProofRequest.ProofRequest}.
     */
    void visit(BPAProofTemplate bpaProofTemplate) {
        templateName = bpaProofTemplate.getName();
    }

    /**
     * Collects the information of the given {@link BPAAttributeGroup}
     * 
     * @param bpaAttributeGroup contains all information for a
     *                          {@link PresentProofRequest.ProofRequest} related to
     *                          a certain schema.
     */
    void visit(BPAAttributeGroup bpaAttributeGroup) {
        resolveLedgerSchemaId.apply(bpaAttributeGroup.getSchemaId()).ifPresent(ledgerSchemaId -> {
            nonRevocationApplicatorMap.put(ledgerSchemaId, NonRevocationApplicator.builder()
                    .applyNonRevocation(bpaAttributeGroup.getNonRevoked())
                    .revocationTimeStampProvider(revocationTimeStampProvider)
                    .build());
            schemaRestrictions.put(ledgerSchemaId, bpaAttributeGroup.getSchemaLevelRestrictions());
        });
    }

    /**
     * Collects the information of the given {@link BPAAttribute} in context of its
     * {@link BPAAttributeGroup}'s <code>schemaId</code>
     * 
     * @param schemaIdAndBpaAttribute is the <code>schemaId</code> of the
     *                                {@link BPAAttributeGroup} containing
     *                                {@link BPAAttribute}
     */
    void visit(Pair<String, BPAAttribute> schemaIdAndBpaAttribute) {
        String schemaId = schemaIdAndBpaAttribute.getLeft();
        BPAAttribute attribute = schemaIdAndBpaAttribute.getRight();
        if (shouldAddAsAttribute(attribute)) {
            attributesBySchemaId.compute(schemaId, addAttribute(attribute));
        }
        if (shouldAddAsPredicate(attribute)) {
            predicatesBySchemaId.compute(schemaId, addPredicates(attribute));
        }
    }

    /**
     * Adds one attribute for the passed {@link BPAAttribute} to the given
     * <code>attributesBuilder</code>.
     *
     * @param attribute part of a {@link BPAProofTemplate}'s
     *                  {@link BPAAttributeGroup}, representing a field in a
     *                  verifiable credential.
     * @return The lambda for {@link Map#compute(Object, BiFunction)}.
     * @see ProofTemplateElementVisitor#addAttribute(String,
     *      Attributes.AttributesBuilder, BPAAttribute)
     */
    private BiFunction<String, Attributes.AttributesBuilder, Attributes.AttributesBuilder> addAttribute(
            BPAAttribute attribute) {
        return (schemaId, attributesBuilder) -> addAttribute(schemaId, attributesBuilder, attribute);
    }

    /**
     * Adds one attribute for the passed {@link BPAAttribute} to the given
     * <code>attributesBuilder</code>.
     *
     * @param schemaId          of the {@link BPAAttributeGroup} the given
     *                          {@link BPAAttribute} is contained in.
     * @param attributesBuilder is the builder containing already processed
     *                          {@link BPAAttribute}s for a
     *                          {@link BPAAttributeGroup}.
     * @param attribute         is part of a {@link BPAProofTemplate}'s
     *                          {@link BPAAttributeGroup}, representing a field in a
     *                          verifiable credential.
     * @return the <code>attributesBuilder</code> extended with the
     *         {@link BPAAttribute} processed during this invocation.
     */
    private Attributes.AttributesBuilder addAttribute(String schemaId,
            Attributes.AttributesBuilder attributesBuilder,
            BPAAttribute attribute) {
        Optional<String> equalsValue = attribute.getConditions().stream()
                .map(Pair.with(BPACondition::getValue, BPACondition::getOperator))
                .filter(Pair.filterRight(ValueOperators.EQUALS::equals))
                .findAny()
                .map(Pair::getLeft);

        Attributes.AttributesBuilder result = Optional.ofNullable(attributesBuilder).orElseGet(Attributes::builder)
                .schemaId(schemaId)
                .name(attribute.getName());
        equalsValue.ifPresent(value -> result.equal(attribute.getName(), value));
        return result;
    }

    /**
     * Adds one predicate for each predicate constraint at the passed
     * {@link BPAAttribute} to the given <code>predicateBuilderList</code>.
     *
     * @param attribute part of a {@link BPAProofTemplate}'s
     *                  {@link BPAAttributeGroup}, representing a field in a
     *                  verifiable credential.
     * @return The lambda for {@link Map#compute(Object, BiFunction)}.
     * @see ProofTemplateElementVisitor#addPredicates(String, List, BPAAttribute)
     */
    private BiFunction<String, List<Predicate.PredicateBuilder>, List<Predicate.PredicateBuilder>> addPredicates(
            BPAAttribute attribute) {
        return (schemaId, predicateBuilderList) -> addPredicates(schemaId, predicateBuilderList, attribute);
    }

    /**
     * Adds one predicate for each predicate constraint at the passed
     * {@link BPAAttribute} to the given <code>predicateBuilderList</code>.
     *
     * @param schemaId             of the {@link BPAAttributeGroup} the given
     *                             {@link BPAAttribute} is contained in.
     * @param predicateBuilderList is the list of already created
     *                             {@link Predicate.PredicateBuilder}s for a
     *                             {@link BPAAttributeGroup}.
     * @param attribute            is part of a {@link BPAProofTemplate}'s
     *                             {@link BPAAttributeGroup}, representing a field
     *                             in a verifiable credential.
     * @return the <code>predicateBuilderList</code> extended with the
     *         {@link Predicate.PredicateBuilder}s created during this invocation.
     */
    private List<Predicate.PredicateBuilder> addPredicates(String schemaId,
            List<Predicate.PredicateBuilder> predicateBuilderList,
            BPAAttribute attribute) {
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
    }

    /**
     * If an attribute is handled as a predicate the value has to be an integer for
     * comparison.
     *
     * @param value to compare the attribute's actual value to.
     * @return a stream containing the value as an integer or an empty stream, if a
     *         conversion was not successful.
     */
    private Stream<Integer> mapValueToInteger(String value) {
        try {
            return Optional.ofNullable(value).map(Integer::parseInt).stream();
        } catch (NumberFormatException e) {
            // TODO log error?
            return Stream.empty();
        }
    }

    /**
     * Attribute declaration with no constraint or the {@link ValueOperators#EQUALS}
     * should be added as attribute.
     *
     * @param attribute is part of a {@link BPAProofTemplate}'s
     *                  {@link BPAAttributeGroup}, representing a field in a
     *                  verifiable credential.
     * @return <code>true</code> if there is no constraint or the operator is
     *         {@link ValueOperators#EQUALS}
     */
    private boolean shouldAddAsAttribute(BPAAttribute attribute) {
        return attribute.getConditions().isEmpty() || attribute.getConditions().stream()
                .map(BPACondition::getOperator).anyMatch(ValueOperators.EQUALS::equals);
    }

    /**
     * Attribute declaration with a relation constraint should be added as
     * predicate.
     *
     * @param attribute is part of a {@link BPAProofTemplate}'s
     *                  {@link BPAAttributeGroup}, representing a field in a
     *                  verifiable credential.
     * @return <code>true</code> if the {@link ValueOperators#handleAsPredicate()}
     *         returns <code>true</code>
     */
    private boolean shouldAddAsPredicate(BPAAttribute attribute) {
        return attribute.getConditions()
                .stream()
                .map(BPACondition::getOperator)
                .anyMatch(ValueOperators::handleAsPredicate);
    }

    /**
     * Creates a {@link PresentProofRequest.ProofRequest} with the information
     * collected by the <code>visit</code> methods.
     * 
     * @return an applicable {@link PresentProofRequest.ProofRequest}
     */
    PresentProofRequest.ProofRequest getResult() {
        PresentProofRequest.ProofRequest.ProofRequestBuilder proofRequestBuilder = PresentProofRequest.ProofRequest
                .builder()
                .name(templateName);

        addAttributesTo(proofRequestBuilder);
        addPredicates(proofRequestBuilder);
        return proofRequestBuilder.build();
    }

    private void addAttributesTo(PresentProofRequest.ProofRequest.ProofRequestBuilder proofRequestBuilder) {
        attributesBySchemaId.entrySet().stream()
                .map(Pair::new)
                .map(Pair.lookUpAndSetOnRight(this::getRevocationApplicator, builder -> builder::revocationApplicator))
                .map(Pair.lookUpAndSetOnRight(this::getSchemaRestrictions, builder -> builder::schemaRestrictions))
                .map(Pair::getRight)
                .map(Attributes.AttributesBuilder::build)
                .forEach(attributes -> attributes.addToBuilder(proofRequestBuilder::requestedAttribute));
    }

    private void addPredicates(PresentProofRequest.ProofRequest.ProofRequestBuilder proofRequestBuilder) {
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
