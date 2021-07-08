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
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.hyperledger.bpa.impl.prooftemplates.ProofTemplateConditionOperators.SCHEMA_ID_OPERATOR_STRING;

@Slf4j
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProofTemplateConditionContext<T> implements Comparable<ProofTemplateConditionContext<T>> {

    final T builder;

    @With(AccessLevel.PACKAGE)
    @NotNull
    BPAAttributeGroup attributeGroup;
    @With(AccessLevel.PACKAGE)
    @Nullable
    BPAAttribute bpaAttribute;

    @With(AccessLevel.PACKAGE)
    @Nullable
    String conditionOperatorString;
    @With(AccessLevel.PACKAGE)
    @Nullable
    String conditionValue;
    @With(AccessLevel.PACKAGE)
    @Nullable
    String attributeName;
    @With(AccessLevel.PUBLIC)
    @Nullable
    ProofTemplateConditionOperator<T> conditionOperator;

    public static <T> Stream<ProofTemplateConditionContext<T>> forTemplate(BPAProofTemplate template, Supplier<T> builder) {
        ProofTemplateConditionContext<T> context = new ProofTemplateConditionContext<>(builder.get());
        return template.streamAttributeGroups().flatMap(context::flattenAttributeGroup);
    }


    Stream<ProofTemplateConditionContext<T>> flattenAttributeGroup(@NonNull BPAAttributeGroup attributeGroup) {
        Stream<ProofTemplateConditionContext<T>> attributes = attributeGroup.getAttributes().stream().flatMap(withAttributeGroup(attributeGroup)::flattenAttribute);
        Stream<ProofTemplateConditionContext<T>> groupConditions = attributeGroup.getSchemaLevelConditions().stream().flatMap(withAttributeGroup(attributeGroup)::flattenCondition);
        return Stream.concat(attributes, groupConditions);
    }

    Stream<ProofTemplateConditionContext<T>> flattenAttribute(@NonNull BPAAttribute attribute) {
        return prependSchemaIdCondition(attribute.getConditions().stream())
                .flatMap(withAttributeName(attribute.getName())::flattenCondition);
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
     * sorts {@link ProofTemplateConditionContext} descending by {@link ProofTemplateConditionOperator#getPrecedence()}, so that operators with highest precedence are applied first.
     *
     * @param other
     * @return
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
            conditionOperator.applyOnBuilder(builder, conditionValue, attributeName);
        } else {
            if (attributeName == null) {
                log.warn("Attribute group {} had an unresolvable condition operator: {}", attributeGroup.getSchemaId(), conditionOperatorString);
            }
        }
    }
}
