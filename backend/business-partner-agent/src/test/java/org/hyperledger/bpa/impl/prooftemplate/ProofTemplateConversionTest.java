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
package org.hyperledger.bpa.impl.prooftemplate;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.hyperledger.acy_py.generated.model.IndyProofReqPredSpec;
import org.hyperledger.aries.api.present_proof.PresentProofRequest;
import org.hyperledger.bpa.model.BPAProofTemplate;
import org.hyperledger.bpa.model.prooftemplate.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

@MicronautTest
public class ProofTemplateConversionTest extends ProofTemplateConversionTestBase {

    @Test
    public void testThatATemplateWithOneAttributeFromOneSchemaIsConvertedCorrectly() {
        UUID schemaId = prepareSchemaWithAttributes("mySchemaId", "name");
        prepareConnectionId("myConnectionId");
        BPAProofTemplate template = BPAProofTemplate.builder()
                .id(UUID.randomUUID())
                .name("MyTestTemplate")
                .attributeGroups(BPAAttributeGroups.builder()
                        .attributeGroup(BPAAttributeGroup.builder()
                                .schemaId(schemaId.toString())
                                .attribute(BPAAttribute.builder()
                                        .name("name")
                                        .build())
                                .build())
                        .build())
                .build();
        PresentProofRequest expected = PresentProofRequest.builder()
                .connectionId("myConnectionId")
                .proofRequest(PresentProofRequest.ProofRequest.builder()
                        .name("MyTestTemplate")
                        .requestedAttribute("mySchemaId",
                                PresentProofRequest.ProofRequest.ProofRequestedAttributes.builder()
                                        .names(List.of("name"))
                                        .restriction(
                                                PresentProofRequest.ProofRequest.ProofRestrictions.builder()
                                                        .schemaId("mySchemaId")
                                                        .build()
                                                        .toJsonObject())
                                        .build())
                        .build())
                .build();
        PresentProofRequest actual = proofTemplateConversion.proofRequestViaVisitorFrom(UUID.randomUUID(), template);
        assertWithAcapy(actual);
        assertEqualAttributesInProofRequests(expected, actual);
    }

    @Test
    public void testThatATemplateWithSeveralAttributesFromTwoSchemaIsConvertedCorrectly() {
        UUID schemaId1 = prepareSchemaWithAttributes("schema1", "name1", "name2");
        UUID schemaId2 = prepareSchemaWithAttributes("schema2", "name1", "name2", "name3");
        prepareConnectionId("myConnectionId");

        BPAProofTemplate template = BPAProofTemplate.builder()
                .id(UUID.randomUUID())
                .name("MyTestTemplate")
                .attributeGroups(BPAAttributeGroups.builder()
                        .attributeGroup(BPAAttributeGroup.builder()
                                .schemaId(schemaId1.toString())
                                .attribute(BPAAttribute.builder()
                                        .name("name1")
                                        .build())
                                .attribute(BPAAttribute.builder()
                                        .name("name2")
                                        .build())
                                .build())
                        .attributeGroup(BPAAttributeGroup.builder()
                                .schemaId(schemaId2.toString())
                                .attribute(BPAAttribute.builder()
                                        .name("name1")
                                        .build())
                                .attribute(BPAAttribute.builder()
                                        .name("name2")
                                        .build())
                                .attribute(BPAAttribute.builder()
                                        .name("name3")
                                        .build())
                                .build())
                        .build())
                .build();

        PresentProofRequest expected = PresentProofRequest.builder()
                .connectionId("myConnectionId")
                .proofRequest(PresentProofRequest.ProofRequest.builder()
                        .name("MyTestTemplate")
                        .requestedAttribute("schema1",
                                PresentProofRequest.ProofRequest.ProofRequestedAttributes.builder()
                                        .names(List.of("name1", "name2"))
                                        .restriction(
                                                PresentProofRequest.ProofRequest.ProofRestrictions.builder()
                                                        .schemaId("schema1")
                                                        .build()
                                                        .toJsonObject())
                                        .build())
                        .requestedAttribute("schema2",
                                PresentProofRequest.ProofRequest.ProofRequestedAttributes.builder()
                                        .names(List.of("name1", "name2", "name3"))
                                        .restriction(
                                                PresentProofRequest.ProofRequest.ProofRestrictions.builder()
                                                        .schemaId("schema2")
                                                        .build()
                                                        .toJsonObject())
                                        .build())
                        .build())
                .build();
        PresentProofRequest actual = proofTemplateConversion.proofRequestViaVisitorFrom(UUID.randomUUID(), template);
        assertWithAcapy(actual);
        assertEqualAttributesInProofRequests(expected, actual);
    }

    @Test
    public void testOneAttributeGroupsWithRevocation() {
        UUID schemaId = prepareSchemaWithAttributes("mySchemaId", "name1", "name2");
        prepareConnectionId("myConnectionId");

        BPAProofTemplate template = BPAProofTemplate.builder()
                .id(UUID.randomUUID())
                .name("MyTestTemplate")
                .attributeGroups(BPAAttributeGroups.builder()
                        .attributeGroup(BPAAttributeGroup.builder()
                                .schemaId(schemaId.toString())
                                .nonRevoked(true)
                                .attribute(BPAAttribute.builder()
                                        .name("name1")
                                        .build())
                                .attribute(BPAAttribute.builder()
                                        .name("name2")
                                        .build())
                                .build())
                        .build())
                .build();

        long epochMillis = clock.millis();
        long epochSeconds = epochMillis / 1000L;

        PresentProofRequest expected = PresentProofRequest.builder()
                .connectionId("myConnectionId")
                .proofRequest(PresentProofRequest.ProofRequest.builder()
                        .requestedAttribute("mySchemaId",
                                PresentProofRequest.ProofRequest.ProofRequestedAttributes.builder()
                                        .names(List.of("name1", "name2"))
                                        .nonRevoked(PresentProofRequest.ProofRequest.ProofNonRevoked.builder()
                                                .to(epochSeconds)
                                                .from(epochSeconds)
                                                .build())
                                        .restriction(
                                                PresentProofRequest.ProofRequest.ProofRestrictions.builder()
                                                        .schemaId("mySchemaId")
                                                        .build()
                                                        .toJsonObject())
                                        .build())
                        .build())
                .build();
        PresentProofRequest actual = proofTemplateConversion.proofRequestViaVisitorFrom(UUID.randomUUID(), template);
        assertWithAcapy(actual);
        actual.getProofRequest().getRequestedAttributes().values().forEach(
                attributes -> Assertions.assertEquals(epochSeconds, attributes.getNonRevoked().getFrom().longValue()));
        assertEqualAttributesInProofRequests(expected, actual);
    }

    @Test
    public void testTwoAttributeGroupsWithRevocation() {
        UUID schemaId1 = prepareSchemaWithAttributes("mySchemaId1", "name1", "name2");
        UUID schemaId2 = prepareSchemaWithAttributes("mySchemaId2", "name3", "name4");
        prepareConnectionId("myConnectionId");

        BPAProofTemplate template = BPAProofTemplate.builder()
                .id(UUID.randomUUID())
                .name("MyTestTemplate")
                .attributeGroups(BPAAttributeGroups.builder()
                        .attributeGroup(BPAAttributeGroup.builder()
                                .schemaId(schemaId1.toString())
                                .nonRevoked(true)
                                .attribute(BPAAttribute.builder()
                                        .name("name1")
                                        .build())
                                .attribute(BPAAttribute.builder()
                                        .name("name2")
                                        .build())
                                .build())
                        .attributeGroup(BPAAttributeGroup.builder()
                                .schemaId(schemaId2.toString())
                                .nonRevoked(true)
                                .attribute(BPAAttribute.builder()
                                        .name("name3")
                                        .build())
                                .attribute(BPAAttribute.builder()
                                        .name("name4")
                                        .build())
                                .build())
                        .build())
                .build();

        long epochMillis = clock.millis();
        long epochSeconds = epochMillis / 1000L;

        PresentProofRequest expected = PresentProofRequest.builder()
                .connectionId("myConnectionId")
                .proofRequest(PresentProofRequest.ProofRequest.builder()
                        .requestedAttribute("mySchemaId1",
                                PresentProofRequest.ProofRequest.ProofRequestedAttributes.builder()
                                        .names(List.of("name1", "name2"))
                                        .nonRevoked(PresentProofRequest.ProofRequest.ProofNonRevoked.builder()
                                                .to(epochSeconds)
                                                .from(epochSeconds)
                                                .build())
                                        .restriction(
                                                PresentProofRequest.ProofRequest.ProofRestrictions.builder()
                                                        .schemaId("mySchemaId1")
                                                        .build()
                                                        .toJsonObject())
                                        .build())
                        .requestedAttribute("mySchemaId2",
                                PresentProofRequest.ProofRequest.ProofRequestedAttributes.builder()
                                        .names(List.of("name3", "name4"))
                                        .nonRevoked(PresentProofRequest.ProofRequest.ProofNonRevoked.builder()
                                                .to(epochSeconds)
                                                .from(epochSeconds)
                                                .build())
                                        .restriction(
                                                PresentProofRequest.ProofRequest.ProofRestrictions.builder()
                                                        .schemaId("mySchemaId2")
                                                        .build()
                                                        .toJsonObject())
                                        .build())
                        .build())
                .build();
        PresentProofRequest actual = proofTemplateConversion.proofRequestViaVisitorFrom(UUID.randomUUID(), template);
        assertWithAcapy(actual);
        actual.getProofRequest().getRequestedAttributes().values().forEach(
                attributes -> Assertions.assertEquals(epochSeconds, attributes.getNonRevoked().getFrom().longValue()));
        assertEqualAttributesInProofRequests(expected, actual);
    }

    @Test
    public void testOneAttributeGroupsAndOnePredicateWithRevocation() {
        UUID schemaId = prepareSchemaWithAttributes("mySchemaId1", "name1", "name2", "secret1");
        prepareConnectionId("myConnectionId");

        BPAProofTemplate template = BPAProofTemplate.builder()
                .id(UUID.randomUUID())
                .name("MyTestTemplate")
                .attributeGroups(BPAAttributeGroups.builder()
                        .attributeGroup(BPAAttributeGroup.builder()
                                .schemaId(schemaId.toString())
                                .nonRevoked(true)
                                .attribute(BPAAttribute.builder()
                                        .name("name1")
                                        .build())
                                .attribute(BPAAttribute.builder()
                                        .name("name2")
                                        .build())
                                .attribute(BPAAttribute.builder()
                                        .name("secret1")
                                        .condition(BPACondition.builder()
                                                .operator(ValueOperators.GREATER_THAN_OR_EQUAL_TO)
                                                .value("10")
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();

        long epochMillis = clock.millis();
        long epochSeconds = epochMillis / 1000L;

        PresentProofRequest expected = PresentProofRequest.builder()
                .connectionId("myConnectionId")
                .proofRequest(PresentProofRequest.ProofRequest.builder()
                        .requestedAttribute("mySchemaId1",
                                PresentProofRequest.ProofRequest.ProofRequestedAttributes.builder()
                                        .names(List.of("name1", "name2"))
                                        .nonRevoked(PresentProofRequest.ProofRequest.ProofNonRevoked.builder()
                                                .to(epochSeconds)
                                                .from(epochSeconds)
                                                .build())
                                        .restriction(
                                                PresentProofRequest.ProofRequest.ProofRestrictions.builder()
                                                        .schemaId("mySchemaId1")
                                                        .build()
                                                        .toJsonObject())
                                        .build())
                        .requestedPredicate("mySchemaId1",
                                PresentProofRequest.ProofRequest.ProofRequestedPredicates.builder()
                                        .name("secret1")
                                        .nonRevoked(PresentProofRequest.ProofRequest.ProofNonRevoked.builder()
                                                .to(epochSeconds)
                                                .from(epochSeconds)
                                                .build())
                                        .pType(IndyProofReqPredSpec.PTypeEnum.GREATER_THAN_OR_EQUAL_TO)
                                        .pValue(10)
                                        .build())
                        .build())
                .build();
        PresentProofRequest actual = proofTemplateConversion.proofRequestViaVisitorFrom(UUID.randomUUID(), template);
        assertWithAcapy(actual);
        actual.getProofRequest().getRequestedAttributes().values().forEach(
                attributes -> Assertions.assertEquals(epochSeconds, attributes.getNonRevoked().getFrom().longValue()));
        assertEqualAttributesInProofRequests(expected, actual);
    }

    @Test
    public void testThatATemplateWithOneAttributeFromOneSchemaAndIssuedByIsConvertedCorrectly() {
        UUID schemaId = prepareSchemaWithAttributes("mySchemaId", "name");
        prepareConnectionId("myConnectionId");
        BPAProofTemplate template = BPAProofTemplate.builder()
                .id(UUID.randomUUID())
                .name("MyTestTemplate")
                .attributeGroups(BPAAttributeGroups.builder()
                        .attributeGroup(BPAAttributeGroup.builder()
                                .schemaLevelRestrictions(List.of(BPASchemaRestrictions.builder()
                                        .issuerDid("issuerDid")
                                        .build()))
                                .schemaId(schemaId.toString())
                                .attribute(BPAAttribute.builder()
                                        .name("name")
                                        .build())
                                .build())
                        .build())
                .build();
        PresentProofRequest expected = PresentProofRequest.builder()
                .connectionId("myConnectionId")
                .proofRequest(PresentProofRequest.ProofRequest.builder()
                        .name("MyTestTemplate")
                        .requestedAttribute("mySchemaId",
                                PresentProofRequest.ProofRequest.ProofRequestedAttributes.builder()
                                        .names(List.of("name"))
                                        .restriction(
                                                PresentProofRequest.ProofRequest.ProofRestrictions.builder()
                                                        .schemaId("mySchemaId")
                                                        .issuerDid("issuerDid")
                                                        .build()
                                                        .toJsonObject())
                                        .build())
                        .build())
                .build();
        PresentProofRequest actual = proofTemplateConversion.proofRequestViaVisitorFrom(UUID.randomUUID(), template);
        assertWithAcapy(actual);
        assertEqualAttributesInProofRequests(expected, actual);
    }

    @Test
    public void testAttributeGroupWithTwoRestrictions() {
        UUID schemaId = prepareSchemaWithAttributes("mySchemaId", "name");
        prepareConnectionId("myConnectionId");
        BPAProofTemplate template = BPAProofTemplate.builder()
                .id(UUID.randomUUID())
                .name("Attribute Two Restrictions")
                .attributeGroups(BPAAttributeGroups.builder()
                        .attributeGroup(BPAAttributeGroup.builder()
                                .schemaLevelRestrictions(List.of(
                                        BPASchemaRestrictions.builder()
                                                .issuerDid("issuerDid1")
                                                .build(),
                                        BPASchemaRestrictions.builder()
                                                .issuerDid("issuerDid2")
                                                .build()))
                                .schemaId(schemaId.toString())
                                .attribute(BPAAttribute.builder()
                                        .name("name")
                                        .condition(BPACondition
                                                .builder()
                                                .value("test")
                                                .operator(ValueOperators.EQUALS)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();
        PresentProofRequest expected = PresentProofRequest.builder()
                .connectionId("myConnectionId")
                .proofRequest(PresentProofRequest.ProofRequest.builder()
                        .name("MyTestTemplate")
                        .requestedAttribute("mySchemaId",
                                PresentProofRequest.ProofRequest.ProofRequestedAttributes.builder()
                                        .names(List.of("name"))
                                        .restriction(
                                                PresentProofRequest.ProofRequest.ProofRestrictions.builder()
                                                        .schemaId("mySchemaId")
                                                        .issuerDid("issuerDid1")
                                                        .addAttributeValueRestriction("name", "test")
                                                        .build()
                                                        .toJsonObject())
                                        .restriction(
                                                PresentProofRequest.ProofRequest.ProofRestrictions.builder()
                                                        .schemaId("mySchemaId")
                                                        .issuerDid("issuerDid2")
                                                        .addAttributeValueRestriction("name", "test")
                                                        .build()
                                                        .toJsonObject())
                                        .build())
                        .build())
                .build();
        PresentProofRequest actual = proofTemplateConversion.proofRequestViaVisitorFrom(UUID.randomUUID(), template);
        assertWithAcapy(actual);
        assertEqualAttributesInProofRequests(expected, actual);
    }

    @Test
    public void testPredicateWithTwoRestrictions() {
        UUID schemaId = prepareSchemaWithAttributes("mySchemaId", "name");
        prepareConnectionId("myConnectionId");
        BPAProofTemplate template = BPAProofTemplate.builder()
                .id(UUID.randomUUID())
                .name("Predicate Two Restrictions")
                .attributeGroups(BPAAttributeGroups.builder()
                        .attributeGroup(BPAAttributeGroup.builder()
                                .schemaLevelRestrictions(List.of(
                                        BPASchemaRestrictions.builder()
                                                .issuerDid("issuerDid1")
                                                .build(),
                                        BPASchemaRestrictions.builder()
                                                .issuerDid("issuerDid2")
                                                .build()))
                                .schemaId(schemaId.toString())
                                .attribute(BPAAttribute.builder()
                                        .name("name")
                                        .condition(BPACondition
                                                .builder()
                                                .value("1234")
                                                .operator(ValueOperators.LESS_THAN)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();
        PresentProofRequest expected = PresentProofRequest.builder()
                .connectionId("myConnectionId")
                .proofRequest(PresentProofRequest.ProofRequest.builder()
                        .name("MyTestTemplate")
                        .requestedPredicate("mySchemaId",
                                PresentProofRequest.ProofRequest.ProofRequestedPredicates.builder()
                                        .name("name")
                                        .pType(IndyProofReqPredSpec.PTypeEnum.LESS_THAN)
                                        .pValue(1234)
                                        .restriction(
                                                PresentProofRequest.ProofRequest.ProofRestrictions.builder()
                                                        .schemaId("mySchemaId")
                                                        .issuerDid("issuerDid1")
                                                        .build()
                                                        .toJsonObject())
                                        .restriction(
                                                PresentProofRequest.ProofRequest.ProofRestrictions.builder()
                                                        .schemaId("mySchemaId")
                                                        .issuerDid("issuerDid2")
                                                        .build()
                                                        .toJsonObject())
                                        .build())
                        .build())
                .build();
        PresentProofRequest actual = proofTemplateConversion.proofRequestViaVisitorFrom(UUID.randomUUID(), template);
        assertWithAcapy(actual);
        assertEqualAttributesInProofRequests(expected, actual);
    }
}
