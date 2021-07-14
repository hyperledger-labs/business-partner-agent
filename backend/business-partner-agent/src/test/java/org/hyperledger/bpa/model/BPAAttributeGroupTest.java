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

import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.validation.validator.Validator;
import org.hyperledger.bpa.impl.aries.config.SchemaService;
import org.hyperledger.bpa.model.prooftemplate.BPAAttribute;
import org.hyperledger.bpa.model.prooftemplate.BPAAttributeGroup;
import org.hyperledger.bpa.model.prooftemplate.BPACondition;
import org.hyperledger.bpa.model.prooftemplate.ValueOperators;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@MicronautTest
class BPAAttributeGroupTest {
    @Inject
    Validator validator;

    @Inject
    SchemaService schemaService;

    @MockBean(SchemaService.class)
    SchemaService schemaService() {
        return Mockito.mock(SchemaService.class);
    }

    @Test
    void testThatSchemaIdIsCheckedForExistenceInSchemaService() {
        Mockito.when(schemaService.getSchemaFor("mySchemaId"))
                .thenReturn(Optional.empty());
        BPAAttributeGroup sut = BPAAttributeGroup.builder().schemaId("mySchemaId").build();
        Set<ConstraintViolation<BPAAttributeGroup>> constraintViolations = validator.validate(sut);
        Assertions.assertEquals(1, constraintViolations.size());
        Assertions.assertEquals("mySchemaId",
                constraintViolations.stream().findFirst().map(ConstraintViolation::getInvalidValue).orElse(null));
    }

    @Test
    void testThatAttributeNamesAreCheckedAgainstSchemaFromSchemaService() {
        Mockito.when(schemaService.getSchemaFor("mySchemaId"))
                .thenReturn(Optional.of(new BPASchema()));
        Mockito.when(schemaService.getSchemaAttributeNames("mySchemaId"))
                .thenReturn(Set.of("surname", "lastname"));
        BPAAttributeGroup sut = BPAAttributeGroup.builder()
                .schemaId("mySchemaId")
                .attribute(BPAAttribute.builder()
                        .name("fullname")
                        .build())
                .attribute(BPAAttribute.builder()
                        .name("surname")
                        .build())
                .attribute(BPAAttribute.builder()
                        .name("lastname")
                        .build())
                .build();
        Set<ConstraintViolation<BPAAttributeGroup>> constraintViolations = validator.validate(sut);
        Assertions.assertEquals(1, constraintViolations.size());
        Assertions.assertEquals(sut,
                constraintViolations.stream().findFirst().map(ConstraintViolation::getInvalidValue).orElse(null));
    }

    @Test
    void testThatAttributesNamesAreDistinct() {
        Mockito.when(schemaService.getSchemaFor("mySchemaId"))
                .thenReturn(Optional.of(new BPASchema()));
        Mockito.when(schemaService.getSchemaAttributeNames("mySchemaId"))
                .thenReturn(Set.of("fullname"));
        BPAAttributeGroup sut = BPAAttributeGroup.builder()
                .schemaId("mySchemaId")
                .attribute(BPAAttribute.builder()
                        .name("fullname")
                        .build())
                .attribute(BPAAttribute.builder()
                        .name("fullname")
                        .build())
                .build();
        Set<ConstraintViolation<BPAAttributeGroup>> constraintViolations = validator.validate(sut);
        Assertions.assertEquals(1, constraintViolations.size());
        List<BPAAttribute> expected = List.of(
                BPAAttribute.builder()
                        .name("fullname")
                        .build(),
                BPAAttribute.builder()
                        .name("fullname")
                        .build());
        Assertions.assertEquals(expected,
                constraintViolations.stream().findFirst().map(ConstraintViolation::getInvalidValue).orElse(null));
    }

    @Test
    void testThatAttributeConditionsAreVerified() {
        Mockito.when(schemaService.getSchemaFor("mySchemaId"))
                .thenReturn(Optional.of(new BPASchema()));
        Mockito.when(schemaService.getSchemaAttributeNames("mySchemaId"))
                .thenReturn(Set.of("myAttributeName"));
        BPACondition invalidCondition = BPACondition.builder()
                .value("any")
                .operator(ValueOperators.GREATER_THAN)
                .build();
        BPAAttributeGroup sut = BPAAttributeGroup.builder()
                .schemaId("mySchemaId")
                .attribute(BPAAttribute.builder()
                        .name("myAttributeName")
                        .condition(invalidCondition)
                        .build())

                .build();

        Set<ConstraintViolation<BPAAttributeGroup>> constraintViolations = validator.validate(sut);
        Assertions.assertEquals(1, constraintViolations.size());
        Assertions.assertEquals(List.of(invalidCondition),
                constraintViolations.stream().findFirst().map(ConstraintViolation::getInvalidValue).orElse(null));
    }
}