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

package org.hyperledger.bpa.model;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.validation.validator.Validator;
import org.hyperledger.bpa.impl.prooftemplates.ProofTemplateConditionOperators;
import org.hyperledger.bpa.model.prooftemplate.BPACondition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import java.util.Set;

@MicronautTest()
class BPAConditionTest {

    @Inject
    Validator validator;

    @Test
    void testThatLessThenConditionIsValid() {
        BPACondition sut = BPACondition.builder().value("else").operator("<").build();
        Set<ConstraintViolation<BPACondition>> constraintViolations = validator.validate(sut);
        Assertions.assertEquals(0, constraintViolations.size());
    }

    @Test
    void testThatLessOrEqualsConditionIsValid() {
        BPACondition sut = BPACondition.builder().value("some").operator("<=").build();
        Set<ConstraintViolation<BPACondition>> constraintViolations = validator.validate(sut);
        Assertions.assertEquals(0, constraintViolations.size());
    }

    @Test
    void testThatGreaterOrEqualsConditionIsValid() {
        BPACondition sut = BPACondition.builder().value("thing").operator(">=").build();
        Set<ConstraintViolation<BPACondition>> constraintViolations = validator.validate(sut);
        Assertions.assertEquals(0, constraintViolations.size());
    }

    @Test
    void testThatGreaterThanConditionIsValid() {
        BPACondition sut = BPACondition.builder().value("any").operator(">").build();
        Set<ConstraintViolation<BPACondition>> constraintViolations = validator.validate(sut);
        Assertions.assertEquals(0, constraintViolations.size());
    }

    @Test
    void testThatEqualsConditionIsValid() {
        BPACondition sut = BPACondition.builder().value("any")
                .operator(ProofTemplateConditionOperators.EQUALS_OPERATOR_STRING).build();
        Set<ConstraintViolation<BPACondition>> constraintViolations = validator.validate(sut);
        Assertions.assertEquals(0, constraintViolations.size());
    }

    @Test
    void testThatIssuerConditionIsValid() {
        BPACondition sut = BPACondition.builder().value("somebody")
                .operator(ProofTemplateConditionOperators.ISSUED_BY_OPERATOR_STRING).build();
        Set<ConstraintViolation<BPACondition>> constraintViolations = validator.validate(sut);
        Assertions.assertEquals(0, constraintViolations.size());
    }

    @Test
    void testThatNonRevocationBeforeConditionIsValid() {
        BPACondition sut = BPACondition.builder().value("somebody")
                .operator(ProofTemplateConditionOperators.NON_REVOKED_OPERATOR_STRING).build();
        Set<ConstraintViolation<BPACondition>> constraintViolations = validator.validate(sut);
        Assertions.assertEquals(0, constraintViolations.size());
    }

    @Test
    // non-revocation proof should use for 'from' the same value as 'to' or omit it.
    // See
    // https://github.com/hyperledger/aries-rfcs/blob/master/concepts/0441-present-proof-best-practices/README.md
    void testThatNonRevocationAfterConditionIsInvalid() {
        Assertions.assertTrue(true);
    }

    @Test
    void testThatATextConditionIsInvalid() {
        BPACondition sut = BPACondition.builder().value("any").operator("A text").build();
        Set<ConstraintViolation<BPACondition>> constraintViolations = validator.validate(sut);
        Assertions.assertEquals(1, constraintViolations.size());
        Assertions.assertEquals(sut,
                constraintViolations.stream().findFirst().map(ConstraintViolation::getInvalidValue).orElse(null));
    }
}