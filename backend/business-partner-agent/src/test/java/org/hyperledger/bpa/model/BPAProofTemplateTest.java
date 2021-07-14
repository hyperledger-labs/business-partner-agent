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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.validation.validator.Validator;
import org.hyperledger.bpa.impl.aries.config.SchemaService;
import org.hyperledger.bpa.impl.verification.prooftemplates.ValidAttributeGroup;
import org.hyperledger.bpa.impl.verification.prooftemplates.ValidBPASchemaId;
import org.hyperledger.bpa.model.prooftemplate2.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@MicronautTest
class BPAProofTemplateTest {
    @Inject
    Validator validator;
    @Inject
    ObjectMapper om;
    @Inject
    SchemaService schemaService;

    @MockBean(SchemaService.class)
    SchemaService schemaService() {
        return Mockito.mock(SchemaService.class);
    }

    @Test
    // FIXME this test is broken, because AttributeGroups from ProofTemplates are
    // not custom validated.
    void testThatAttributeGroupsAreVerified() {
        Mockito.when(schemaService.getSchemaFor("mySchemaId"))
                .thenReturn(Optional.of(new BPASchema()));
        Mockito.when(schemaService.getSchemaAttributeNames("mySchemaId"))
                .thenReturn(Set.of("anotherAttributeName"));
        BPAProofTemplate sut = BPAProofTemplate.builder()
                .id(UUID.randomUUID())
                .name("MyTestTemplate")
                .attributeGroups(
                        BPAAttributeGroups.builder()
                                .attributeGroup(BPAAttributeGroup.builder()
                                        .schemaId("notExistingSchema")
                                        .attribute(BPAAttribute.builder()
                                                .name("myAttributeName")
                                                .build())
                                        .build())
                                .attributeGroup(BPAAttributeGroup.builder()
                                        .schemaId("mySchemaId")
                                        .attribute(BPAAttribute.builder()
                                                .name("notASchemaAttribute")
                                                .build())
                                        .build())
                                .build())
                .build();

        Set<ConstraintViolation<BPAProofTemplate>> constraintViolations = validator.validate(sut);
        Assertions.assertEquals(2, constraintViolations.size());
        List<Object> violatingValues = constraintViolations.stream().map(ConstraintViolation::getMessageTemplate)
                .collect(Collectors.toList());
        Assertions.assertTrue(violatingValues.contains(ValidBPASchemaId.MESSAGE_TEMPLATE));
        Assertions.assertTrue(violatingValues.contains(ValidAttributeGroup.MESSAGE_TEMPLATE));
    }

    @Test
    void testThatSerializationWorksBothWays() throws JsonProcessingException {
        BPAProofTemplate proofTemplate = BPAProofTemplate.builder()
                .id(UUID.randomUUID())
                .name("MyTestTemplate")
                .attributeGroups(
                        BPAAttributeGroups.builder()
                                .attributeGroup(BPAAttributeGroup.builder()
                                        .schemaId("mySchemaId")
                                        .attribute(BPAAttribute.builder()
                                                .name("myAttributeName")
                                                .condition(BPACondition.builder()
                                                        .value("any")
                                                        .operator(ValueOperators.LESS_THAN)
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                .build();
        String string = om.writeValueAsString(proofTemplate);
        BPAProofTemplate result = om.readValue(string, BPAProofTemplate.class);
        Assertions.assertEquals(proofTemplate, result);
    }
}