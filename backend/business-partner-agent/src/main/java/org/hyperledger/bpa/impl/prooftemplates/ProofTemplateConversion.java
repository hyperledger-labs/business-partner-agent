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

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.util.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.aries.api.present_proof.PresentProofRequest;
import org.hyperledger.bpa.api.aries.AriesProofExchange;
import org.hyperledger.bpa.api.aries.SchemaAPI;
import org.hyperledger.bpa.api.exception.PartnerException;
import org.hyperledger.bpa.controller.api.prooftemplates.*;
import org.hyperledger.bpa.impl.aries.config.SchemaService;
import org.hyperledger.bpa.model.BPAProofTemplate;
import org.hyperledger.bpa.model.Partner;
import org.hyperledger.bpa.model.prooftemplate.BPAAttribute;
import org.hyperledger.bpa.model.prooftemplate.BPAAttributeGroup;
import org.hyperledger.bpa.model.prooftemplate.ValueOperators;
import org.hyperledger.bpa.repository.PartnerRepository;
import org.hyperledger.bpa.util.Pair;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.Valid;
import java.time.Clock;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Singleton
public class ProofTemplateConversion {

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    Clock clock;

    @Inject
    SchemaService schemaService;

    public AriesProofExchange.ProofTemplateInfo requestToTemplate(PresentProofRequest.ProofRequest proofRequest) {
        if (proofRequest == null) {
            return null;
        }
        // TODO If not able to convert return JSON (advanced case)
        List<AttributeGroup> attributeGroup = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(proofRequest.getRequestedAttributes())) {
            proofRequest.getRequestedAttributes().forEach((groupName, attributes) -> {
                AttributeGroup.AttributeGroupBuilder gb = AttributeGroup.builder();
                if (StringUtils.isNotEmpty(attributes.getName())) {
                    Attribute.AttributeBuilder ab = Attribute.builder();
                    ab.name(attributes.getName());
                    findMatchingPredicates(attributes.getName(), proofRequest.getRequestedPredicates())
                            .ifPresent(ab::conditions);
                    gb.attribute(ab.build());
                } else {
                    List<Attribute> bpaAttributes = new ArrayList<>();
                    attributes.getNames().forEach(n -> {
                        Attribute.AttributeBuilder ab = Attribute.builder();
                        ab.name(n);
                        findMatchingPredicates(n, proofRequest.getRequestedPredicates())
                                .ifPresent(ab::conditions);
                        bpaAttributes.add(ab.build());
                    });
                    gb.attributes(bpaAttributes);
                }
                gb.nonRevoked(attributes.getNonRevoked() != null && attributes.getNonRevoked().isSet());
                if (CollectionUtils.isNotEmpty(attributes.getRestrictions())) {
                    if (attributes.getRestrictions().size() > 1) {
                        // TODO json fallback
                        log.debug("should return json");
                    }
                    gb.schemaLevelRestrictions(SchemaRestrictions.fromProofRestrictions(
                            PresentProofRequest.ProofRequest.ProofRestrictions.fromJsonObject(
                                    attributes.getRestrictions().get(0))));
                }
                attributeGroup.add(gb.build());
            });
        }
        return AriesProofExchange.ProofTemplateInfo
                .builder()
                .proofRequest(proofRequest)
                .proofTemplate(ProofTemplate.builder()
                        .name(proofRequest.getName())
                        .attributeGroups(attributeGroup)
                        .build())
                .build();
    }

    private Optional<List<ValueCondition>> findMatchingPredicates(@NonNull String name,
            Map<String, PresentProofRequest.ProofRequest.ProofRequestedPredicates> requestedPredicates) {
        if (CollectionUtils.isNotEmpty(requestedPredicates)) {
            return Optional.of(requestedPredicates.values().stream()
                    .filter(predicate -> name.equals(predicate.getName()))
                    .map(predicate -> ValueCondition.builder()
                            .operator(ValueOperators.fromPType(predicate.getPType()))
                            .value(String.valueOf(predicate.getPValue()))
                            .build())
                    .collect(Collectors.toList()));
        }
        return Optional.empty();
    }

    @NonNull
    public PresentProofRequest proofRequestViaVisitorFrom(@NonNull UUID partnerId,
            @NonNull @Valid BPAProofTemplate proofTemplate) {
        final Partner partner = partnerRepo.findById(partnerId)
                .orElseThrow(() -> new PartnerException("Partner not found"));
        if (!partner.hasConnectionId()) {
            throw new PartnerException("Partner has no aca-py connection");
        }

        ProofTemplateElementVisitor proofTemplateElementVisitor = new ProofTemplateElementVisitor(
                this::resolveLedgerSchemaId,
                new RevocationTimeStampProvider(clock));

        proofTemplateElementVisitor.visit(proofTemplate);
        proofTemplate.streamAttributeGroups()
                .forEach(proofTemplateElementVisitor::visit);
        proofTemplate.streamAttributeGroups()
                .flatMap(this::pairSchemaIdWithAttributes)
                .forEach(proofTemplateElementVisitor::visit);

        return PresentProofRequest.builder()
                .proofRequest(proofTemplateElementVisitor.getResult())
                .connectionId(partner.getConnectionId())
                .build();
    }

    public Optional<String> resolveLedgerSchemaId(String databaseSchemaId) {
        return schemaService.getSchema(UUID.fromString(databaseSchemaId)).map(SchemaAPI::getSchemaId);
    }

    @NotNull
    private Stream<Pair<String, BPAAttribute>> pairSchemaIdWithAttributes(@NonNull BPAAttributeGroup ag) {
        Optional<Pair.PairBuilder<String, BPAAttribute>> pairBuilder = resolveLedgerSchemaId(ag.getSchemaId())
                .map(Pair.<String, BPAAttribute>builder()::left);
        return pairBuilder.map(
                pair -> ag.getAttributes().stream()
                        .map(pair::right)
                        .map(Pair.PairBuilder::build))
                .orElse(Stream.empty());
    }

}
