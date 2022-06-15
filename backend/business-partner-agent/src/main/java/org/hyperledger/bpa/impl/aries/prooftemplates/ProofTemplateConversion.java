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
package org.hyperledger.bpa.impl.aries.prooftemplates;

import io.micronaut.core.annotation.NonNull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.api.present_proof.PresentProofRequest;
import org.hyperledger.bpa.api.aries.SchemaAPI;
import org.hyperledger.bpa.api.exception.PartnerException;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.impl.aries.schema.SchemaService;
import org.hyperledger.bpa.impl.util.Pair;
import org.hyperledger.bpa.persistence.model.BPAProofTemplate;
import org.hyperledger.bpa.persistence.model.Partner;
import org.hyperledger.bpa.persistence.model.prooftemplate.BPAAttribute;
import org.hyperledger.bpa.persistence.model.prooftemplate.BPAAttributeGroup;
import org.hyperledger.bpa.persistence.repository.PartnerRepository;

import javax.validation.Valid;
import java.time.Clock;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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

    @Inject
    BPAMessageSource.DefaultMessageSource ms;

    @NonNull
    public PresentProofRequest proofRequestViaVisitorFrom(@NonNull UUID partnerId,
            @NonNull @Valid BPAProofTemplate proofTemplate) {
        final Partner partner = partnerRepo.findById(partnerId)
                .orElseThrow(
                        () -> new PartnerException(ms.getMessage("api.partner.not.found", Map.of("id", partnerId))));
        if (!partner.hasConnectionId()) {
            throw new PartnerException(ms.getMessage("api.partner.no.connection"));
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

    private Optional<String> resolveLedgerSchemaId(String databaseSchemaId) {
        return schemaService.getSchema(UUID.fromString(databaseSchemaId)).map(SchemaAPI::getSchemaId);
    }

    @NonNull
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
