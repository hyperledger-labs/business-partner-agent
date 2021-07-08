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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ProofTemplateConditionOperators<Predicate, Attribute, Restrictions> {

    public static final String EQUALS_OPERATOR_STRING = "==";
    public static final String ISSUED_BY_OPERATOR_STRING = "issued-by";
    public static final String NON_REVOKED_OPERATOR_STRING = "<R";
    public static final String SCHEMA_ID_OPERATOR_STRING = "schema-id";
    public static final String FETCH_VALUE = "fetch-value";

    private Map<String, ProofTemplateConditionOperator<ProofTemplateRequestBuilder<Predicate, Attribute, Restrictions>>> knownOperators = new HashMap<>();

    public void put(@Nullable String operatorName,
            ProofTemplateConditionOperator<ProofTemplateRequestBuilder<Predicate, Attribute, Restrictions>> operator) {
        knownOperators.put(operatorName, operator);
    }

    public void putIfAbsent(@Nullable String operatorName,
            ProofTemplateConditionOperator<ProofTemplateRequestBuilder<Predicate, Attribute, Restrictions>> operator) {
        knownOperators.putIfAbsent(operatorName, operator);
    }

    public Optional<ProofTemplateConditionOperator<ProofTemplateRequestBuilder<Predicate, Attribute, Restrictions>>> getConditionOperatorFor(
            String operatorString) {
        return Optional.ofNullable(knownOperators.get(operatorString));
    }
}
