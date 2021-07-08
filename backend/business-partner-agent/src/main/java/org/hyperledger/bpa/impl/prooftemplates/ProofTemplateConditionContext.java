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

import io.micronaut.core.annotation.Nullable;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.bpa.model.BPAAttribute;
import org.hyperledger.bpa.model.BPAAttributeGroup;
import org.hyperledger.bpa.model.BPACondition;
import org.hyperledger.bpa.model.BPAProofTemplate;

import javax.validation.constraints.NotNull;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.hyperledger.bpa.impl.prooftemplates.ProofTemplateConditionOperators.FETCH_VALUE;
import static org.hyperledger.bpa.impl.prooftemplates.ProofTemplateConditionOperators.SCHEMA_ID_OPERATOR_STRING;

/**
 * @param <T>
 */
@Slf4j
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProofTemplateConditionContext<T> implements Comparable<ProofTemplateConditionContext<T>> {
    @With(AccessLevel.PRIVATE)
    private final T builder;

    @With(AccessLevel.PACKAGE)
    @NotNull
    private BPAAttributeGroup attributeGroup;

    @With(AccessLevel.PACKAGE)
    @Nullable
    private String conditionOperatorString;
    @With(AccessLevel.PACKAGE)
    @Nullable
    private String conditionValue;
    @With(AccessLevel.PACKAGE)
    @Nullable
    private String attributeName;
    @With(AccessLevel.PUBLIC)
    @Nullable
    private ProofTemplateConditionOperator<T> conditionOperator;

    private static final BPACondition DEFAULT_CONDITION_FETCH_VALUE = BPACondition.builder().operator(FETCH_VALUE)
            .build();

    public static <T> Stream<ProofTemplateConditionContext<T>> forTemplate(BPAProofTemplate template,
            Function<String, T> builder) {
        return template.streamAttributeGroups()
                .flatMap(group -> contextForGroup(builder, group)
                        .flattenAttributeGroup(group));
    }

    private static <T> ProofTemplateConditionContext<T> contextForGroup(Function<String, T> builder,
            BPAAttributeGroup group) {
        return new ProofTemplateConditionContext<>(builder.apply(group.getSchemaId()));
    }

    Stream<ProofTemplateConditionContext<T>> flattenAttributeGroup(@NonNull BPAAttributeGroup attributeGroup) {
        Stream<ProofTemplateConditionContext<T>> attributes = attributeGroup.getAttributes().stream()
                .flatMap(withAttributeGroup(attributeGroup)::flattenAttribute);
        Stream<ProofTemplateConditionContext<T>> groupConditions = attributeGroup.getSchemaLevelConditions().stream()
                .flatMap(withAttributeGroup(attributeGroup)::flattenCondition);
        return Stream.concat(attributes, groupConditions);
    }

    Stream<ProofTemplateConditionContext<T>> flattenAttribute(@NonNull BPAAttribute attribute) {

        Stream<BPACondition> conditionStream = haveAtLeastOneCondition(attribute);
        return prependSchemaIdCondition(conditionStream)
                .flatMap(withAttributeName(attribute.getName())::flattenCondition);
    }

    private Stream<BPACondition> haveAtLeastOneCondition(@org.jetbrains.annotations.NotNull BPAAttribute attribute) {
        Stream<BPACondition> conditionStream = attribute.getConditions().stream();
        if (attribute.getConditions().isEmpty()) {
            conditionStream = Stream.of(DEFAULT_CONDITION_FETCH_VALUE);
        }
        return conditionStream;
    }

    Stream<BPACondition> prependSchemaIdCondition(Stream<BPACondition> conditions) {
        BPACondition schemaIdCondition = BPACondition.builder()
                .operator(SCHEMA_ID_OPERATOR_STRING)
                .value(attributeGroup.getSchemaId())
                .build();
        return Stream.concat(Stream.of(schemaIdCondition), conditions);
    }

    Stream<ProofTemplateConditionContext<T>> flattenCondition(@NonNull BPACondition condition) {
        return Stream.of(withConditionOperatorString(condition.getOperator()).withConditionValue(condition.getValue()));
    }

    /**
     * sorts {@link ProofTemplateConditionContext} descending by
     * {@link ProofTemplateConditionOperator#getPrecedence()}, so that operators
     * with highest precedence are applied first.
     *
     * @param other templateConditionContext whose operator precedence is compared
     *              to this'
     * @return a value < 0 if this' operator's precedence is greater than the
     *         other's,<br>
     *         a value > 0 if this' operator's precedence is less than the
     *         other's,<br>
     *         0 if precendences are equal or both operators are
     *         <code>null</code>.<br>
     *         An absent operator (<code>null</code>) is treated as having a
     *         precedence which is less than a present operator's precedence.
     */
    @Override
    public int compareTo(@NonNull ProofTemplateConditionContext<T> other) {
        int result;
        if (conditionOperator != null) {
            if (other.conditionOperator != null) {
                result = conditionOperator.getPrecedence() - other.conditionOperator.getPrecedence();
            } else {
                result = 1;
            }
        } else {
            if (other.conditionOperator != null) {
                result = -1;
            } else {
                result = 0;
            }
        }
        // make it descending.
        return -1 * result;
    }

    public void applyOnBuilder() {
        if (conditionOperator != null) {
            conditionOperator.applyOnBuilder(builder, attributeName, conditionValue);
        } else {
            if (attributeName == null) {
                log.warn("Attribute group {} had an unresolvable condition operator: {}", attributeGroup.getSchemaId(),
                        conditionOperatorString);
            }
        }
    }
}
