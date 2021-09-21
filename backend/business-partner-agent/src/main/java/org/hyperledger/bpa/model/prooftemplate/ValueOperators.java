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

package org.hyperledger.bpa.model.prooftemplate;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.NonNull;
import org.hyperledger.acy_py.generated.model.IndyProofReqPredSpec;

import java.util.Optional;
import java.util.function.Predicate;

class ParseableAsInteger implements Predicate<String> {
    @Override
    public boolean test(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

public enum ValueOperators {
    @JsonProperty("==")
    EQUALS("==",
            "Compares the attributes value with the given value for equality. This reveals the value.", s -> true),
    @JsonProperty("<")
    LESS_THAN(IndyProofReqPredSpec.PTypeEnum.LESS_THAN,
            "True, if the attributes value is less than given. Do not reveal the value.",
            new ParseableAsInteger()),
    @JsonProperty("<=")
    LESS_THAN_OR_EQUAL_TO(IndyProofReqPredSpec.PTypeEnum.LESS_THAN_OR_EQUAL_TO,
            "True, if the attributes value is less than or equal given. Do not reveal the value.",
            new ParseableAsInteger()),
    @JsonProperty(">")
    GREATER_THAN(IndyProofReqPredSpec.PTypeEnum.GREATER_THAN,
            "True, if the attributes value is greater than given. Do not reveal the value.",
            new ParseableAsInteger()),
    @JsonProperty(">=")
    GREATER_THAN_OR_EQUAL_TO(IndyProofReqPredSpec.PTypeEnum.GREATER_THAN_OR_EQUAL_TO,
            "True, if the attributes value is greater than or equal given. Do not reveal the value.",
            new ParseableAsInteger());

    private final IndyProofReqPredSpec.PTypeEnum predicateOperator;
    private final String value;
    private final String description;
    private final Predicate<String> conditionValueValidator;

    ValueOperators(String value, String description, Predicate<String> conditionValueValidator) {
        this.predicateOperator = null;
        this.value = value;
        this.description = description;
        this.conditionValueValidator = conditionValueValidator;

    }

    ValueOperators(IndyProofReqPredSpec.PTypeEnum value, String description,
            Predicate<String> conditionValueValidator) {
        this.predicateOperator = value;
        this.value = value.getValue();
        this.description = description;
        this.conditionValueValidator = conditionValueValidator;
    }

    public boolean handleAsPredicate() {
        return predicateOperator != null;
    }

    public Optional<IndyProofReqPredSpec.PTypeEnum> getPredicateOperator() {
        return Optional.ofNullable(predicateOperator);
    }

    public boolean conditionValueIsValid(String conditionValue) {
        return this.conditionValueValidator.test(conditionValue);
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return value + " - " + description;
    }

    public static ValueOperators fromPType(@NonNull IndyProofReqPredSpec.PTypeEnum pType) {
        switch (pType) {
        case GREATER_THAN:
            return GREATER_THAN;
        case GREATER_THAN_OR_EQUAL_TO:
            return GREATER_THAN_OR_EQUAL_TO;
        case LESS_THAN:
            return LESS_THAN;
        case LESS_THAN_OR_EQUAL_TO:
            return LESS_THAN_OR_EQUAL_TO;
        default:
            return null;
        }
    }

}
