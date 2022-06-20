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
package org.hyperledger.bpa.impl.verification;

import io.micronaut.context.annotation.Factory;
import io.micronaut.validation.validator.constraints.ConstraintValidator;
import jakarta.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.bpa.api.aries.SchemaAPI;
import org.hyperledger.bpa.impl.aries.schema.SchemaService;
import org.hyperledger.bpa.impl.util.Pair;
import org.hyperledger.bpa.impl.verification.prooftemplates.DistinctAttributeNames;
import org.hyperledger.bpa.impl.verification.prooftemplates.ValidAttributeCondition;
import org.hyperledger.bpa.impl.verification.prooftemplates.ValidAttributeGroup;
import org.hyperledger.bpa.impl.verification.prooftemplates.ValidBPASchemaId;
import org.hyperledger.bpa.persistence.model.prooftemplate.BPAAttribute;
import org.hyperledger.bpa.persistence.model.prooftemplate.BPAAttributeGroup;
import org.hyperledger.bpa.persistence.model.prooftemplate.BPACondition;

import java.util.*;
import java.util.function.Predicate;

@Factory
public class ValidatorFactory {

    @Singleton
    ConstraintValidator<ValidUUID, CharSequence> uuidValidator() {
        return (value, annotationMetadata, context) -> {
            // treat an empty id as valid.
            if (StringUtils.isBlank(value)) {
                return true;
            }
            context.messageTemplate("{validatedValue} is not a valid UUID");
            return isUUID(value);
        };
    }

    private boolean isUUID(CharSequence value) {
        try {
            UUID.fromString(String.valueOf(value));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Singleton
    ConstraintValidator<ValidBPASchemaId, CharSequence> schemaIdValidator(SchemaService schemaService) {
        return (value, annotationMetadata, context) -> Optional.ofNullable(value)
                .map(String::valueOf)
                .filter(this::isUUID)
                .map(UUID::fromString)
                .flatMap(schemaService::getSchema)
                .isPresent();
    }

    // TODO find a way to validate single list entries in isolation. This shows the
    // whole list as invalid, even if it's only one entry.
    @Singleton
    ConstraintValidator<ValidAttributeGroup, List<BPAAttributeGroup>> attributeGroupsValidator(
            SchemaService schemaService) {
        return (value, annotationMetadata, context) -> Objects.requireNonNull(value)
                .stream()
                .allMatch(attributeGroup -> attributeGroupValidator(schemaService).isValid(attributeGroup,
                        annotationMetadata, context));

    }

    @Singleton
    ConstraintValidator<ValidAttributeGroup, BPAAttributeGroup> attributeGroupValidator(SchemaService schemaService) {
        return (value, annotationMetadata, context) -> {
            boolean valid = false;
            if (value != null) {
                Optional<Predicate<String>> attributeInSchema = Optional.of(value)
                        .map(BPAAttributeGroup::getSchemaId)
                        .flatMap(schemaService::getSchema)
                        .map(SchemaAPI::getSchemaId)
                        .map(schemaId -> schemaService
                                .getSchemaAttributeNames(schemaId)::contains);
                List<BPAAttribute> attributes = value.getAttributes();
                if (attributes != null && attributeInSchema.isPresent()) {
                    // an empty AttributeGroup is treated as valid
                    valid = attributes.isEmpty() || attributes.stream()
                            .map(BPAAttribute::getName)
                            .allMatch(attributeInSchema.get());
                } else if (attributeInSchema.isEmpty()) {
                    // treat attributes in an attribute group for a not existing schema as valid,
                    // because these would be a follow error.
                    valid = true;
                }
            }
            return valid;
        };
    }

    @Singleton
    ConstraintValidator<DistinctAttributeNames, List<BPAAttribute>> distinctAttributeNamesValidator() {
        return (value, annotationMetadata, context) -> {
            Set<String> seenAttributes = new HashSet<>();
            Predicate<String> findDuplicates = Predicate.not(seenAttributes::add);
            if (value != null && !value.isEmpty()) {
                return value.stream()
                        .map(BPAAttribute::getName)
                        .noneMatch(findDuplicates);
            }
            return true;
        };
    }

    // TODO find a way to validate single list entries in isolation. This shows the
    // whole list as invalid, even if it's only one entry.
    @Singleton
    ConstraintValidator<ValidAttributeCondition, List<BPACondition>> attributeConditionsOperatorAndValueValidator() {
        return (value, annotationMetadata, context) -> Objects.requireNonNull(value)
                .stream()
                .allMatch(condition -> attributeConditionOperatorAndValueValidator()
                        .isValid(condition, annotationMetadata, context));
    }

    @Singleton
    ConstraintValidator<ValidAttributeCondition, BPACondition> attributeConditionOperatorAndValueValidator() {
        return (value, annotationMetadata, context) -> Optional.ofNullable(value)
                .map(condition -> new Pair<>(condition.getValue(), condition.getOperator()))
                .filter(pair -> pair.getRight().conditionValueIsValid(pair.getLeft()))
                .isPresent();

    }
}