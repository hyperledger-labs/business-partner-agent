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
import org.hyperledger.bpa.controller.api.prooftemplates.Condition;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import java.util.Set;
@MicronautTest()
class ConditionTest {

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
    void testThatEqualsConditionIsInvalid() {
        BPACondition sut = BPACondition.builder().value("any").operator("==").build();
        Set<ConstraintViolation<BPACondition>> constraintViolations = validator.validate(sut);
        Assertions.assertEquals(1, constraintViolations.size());
        Assertions.assertEquals("==",constraintViolations.stream().findFirst().map(ConstraintViolation::getInvalidValue).get());
    }

    @Test
    void testThatATextConditionIsInvalid() {
        BPACondition sut = BPACondition.builder().value("any").operator("A text").build();
        Set<ConstraintViolation<BPACondition>> constraintViolations = validator.validate(sut);
        Assertions.assertEquals(1, constraintViolations.size());
        Assertions.assertEquals("A text",constraintViolations.stream().findFirst().map(ConstraintViolation::getInvalidValue).get());
    }
}