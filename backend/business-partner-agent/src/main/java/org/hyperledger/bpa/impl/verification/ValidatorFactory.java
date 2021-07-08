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

package org.hyperledger.bpa.impl.verification;

import io.micronaut.context.annotation.Factory;
import io.micronaut.validation.validator.constraints.ConstraintValidator;
import org.hyperledger.bpa.impl.aries.config.SchemaService;
import org.hyperledger.bpa.impl.prooftemplates.ProofTemplateConditionOperators;
import org.hyperledger.bpa.impl.verification.prooftemplates.*;
import org.hyperledger.bpa.model.BPAAttribute;
import org.hyperledger.bpa.model.BPAAttributeGroup;

import javax.inject.Singleton;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

@Factory
public class ValidatorFactory {

    @Singleton
    ConstraintValidator<ValidUUID, CharSequence> uuidValidator() {
        return (value, annotationMetadata, context) -> {
            // treat an empty id as valid.
            if (value == null) {
                return true;
            }
            try {
                UUID.fromString(String.valueOf(value));
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        };
    }

    @Singleton
    ConstraintValidator<ValidBPASchemaId, CharSequence> schemaIdValidator(SchemaService schemaService) {
        return (value, annotationMetadata, context) -> schemaService.getSchemaFor(String.valueOf(value)).isPresent();
    }

    @Singleton
    ConstraintValidator<ValidAttributeGroup, BPAAttributeGroup> attributeGroupValidator(SchemaService schemaService) {
        return (value, annotationMetadata, context) -> {
            boolean valid = false;
            if (value != null) {
                Predicate<String> attributeInSchema = schemaService
                        .getSchemaAttributeNames(value.getSchemaId())::contains;
                List<BPAAttribute> attributes = value.getAttributes();
                if (attributes != null) {
                    // an empty AttributeGroup is treated as valid
                    valid = attributes.isEmpty() || attributes.stream()
                            .map(BPAAttribute::getName)
                            .allMatch(attributeInSchema);
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


    @Singleton
    ConstraintValidator<ValidAttributeConditionOperator, CharSequence> attributeConditionOperatorValidator(ProofTemplateConditionOperators<?, ?, ?> conditionOperators) {
        return (value, annotationMetadata, context) ->
                conditionOperators.getConditionOperatorFor(String.valueOf(value)).isPresent();
    }
}