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
package org.hyperledger.bpa.impl.aries.prooftemplate;

import io.micronaut.test.annotation.MockBean;
import jakarta.inject.Inject;
import org.hyperledger.aries.api.present_proof.PresentProofRequest;
import org.hyperledger.bpa.RunWithAries;
import org.hyperledger.bpa.api.aries.SchemaAPI;
import org.hyperledger.bpa.impl.aries.prooftemplates.ProofTemplateConversion;
import org.hyperledger.bpa.impl.aries.schema.SchemaService;
import org.hyperledger.bpa.persistence.model.BPASchema;
import org.hyperledger.bpa.persistence.model.Partner;
import org.hyperledger.bpa.persistence.repository.PartnerRepository;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

public class ProofTemplateConversionTestBase extends RunWithAries {
    @Inject
    SchemaService schemaService;
    @Inject
    PartnerRepository partnerRepo;
    @Inject
    ProofTemplateConversion proofTemplateConversion;
    @Inject
    Clock clock;

    @MockBean(SchemaService.class)
    SchemaService schemaService() {
        return Mockito.mock(SchemaService.class);
    }

    @MockBean(PartnerRepository.class)
    PartnerRepository partnerRepository() {
        return Mockito.mock(PartnerRepository.class);
    }

    @MockBean(Clock.class)
    Clock testClock() {
        return Clock.fixed(Instant.now(), ZoneId.systemDefault());
    }

    protected UUID prepareSchemaWithAttributes(String ledgerSchemaId, String... attributes) {
        UUID schemaId = UUID.randomUUID();
        Mockito.when(schemaService.getSchema(schemaId))
                .thenReturn(Optional.of(SchemaAPI.builder().schemaId(ledgerSchemaId).id(schemaId).build()));
        Mockito.when(schemaService.getSchemaFor(ledgerSchemaId))
                .thenReturn(Optional.of(BPASchema.builder().schemaId(ledgerSchemaId).id(schemaId).build()));
        Mockito.when(schemaService.getSchemaAttributeNames(ledgerSchemaId))
                .thenReturn(Set.of(attributes));
        return schemaId;
    }

    protected void prepareConnectionId(String connectionId) {
        Mockito.when(partnerRepo.findById(Mockito.any(UUID.class)))
                .thenReturn(Optional.of(Partner.builder()
                        .connectionId(connectionId)
                        .build()));
    }

    protected void assertWithAcapy(PresentProofRequest proofRequest) {
        try {
            ac.presentProofCreateRequest(proofRequest);
        } catch (IOException e) {
            Assertions.fail("aca-py cannot process the request " + proofRequest, e);
        }
    }

    protected void assertEqualAttributesInProofRequests(PresentProofRequest expected, PresentProofRequest actual) {
        Assertions.assertEquals(expected.getConnectionId(), actual.getConnectionId());

        Map<String, PresentProofRequest.ProofRequest.ProofRequestedAttributes> actualAttributes = new HashMap<>(actual
                .getProofRequest().getRequestedAttributes());
        expected.getProofRequest().getRequestedAttributes()
                .forEach((key, value) -> assertEqualRequestedAttributes(key, value, actualAttributes.get(key)));
    }

    private void assertEqualRequestedAttributes(String groupName,
            PresentProofRequest.ProofRequest.ProofRequestedAttributes expected,
            PresentProofRequest.ProofRequest.ProofRequestedAttributes actual) {
        if (actual == null) {
            if (expected != null) {
                Assertions
                        .fail(String.format("The ProofRequestedAttributes with the name %s does not exist", groupName));
            } else {
                Assertions.fail("expected and actual are not set.");
            }
        }
        Assertions.assertEquals(expected.getNonRevoked(), actual.getNonRevoked(), String.format(
                "The non-revocation does not match for ProofRequestedAttributes with the name %s", groupName));
        Assertions.assertEquals(sortedAttributeNames(expected), sortedAttributeNames(actual), String.format(
                "The attribute names do not match for ProofRequestedAttributes with the name %s", groupName));
        Assertions.assertEquals(flattenRestrictions(expected), flattenRestrictions(actual),
                String.format("The restrictions objects do not match for ProofRequestedAttributes with the name %s",
                        expected.getName()));
    }

    @NotNull
    private List<String> sortedAttributeNames(PresentProofRequest.ProofRequest.ProofRequestedAttributes expected) {
        return expected.getNames().stream().sorted().collect(Collectors.toList());
    }

    private List<String> flattenRestrictions(PresentProofRequest.ProofRequest.ProofRequestedAttributes expected) {
        return expected.getRestrictions().stream()
                .map(json -> json.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue().toString())
                        .sorted().collect(Collectors.joining("\n")))
                .sorted().collect(Collectors.toList());
    }
}
