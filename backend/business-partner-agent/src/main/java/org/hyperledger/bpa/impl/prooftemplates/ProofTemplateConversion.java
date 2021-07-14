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
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.api.present_proof.PresentProofRequest;
import org.hyperledger.bpa.api.exception.PartnerException;
import org.hyperledger.bpa.model.*;
import org.hyperledger.bpa.model.prooftemplate.BPAAttribute;
import org.hyperledger.bpa.model.prooftemplate.BPAAttributeGroup;
import org.hyperledger.bpa.repository.PartnerRepository;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.Valid;
import java.time.Clock;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
@Singleton
public class ProofTemplateConversion {

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    Clock clock;

    @NonNull
    public PresentProofRequest proofRequestViaVisitorFrom(@NonNull UUID partnerId,
            @NonNull @Valid BPAProofTemplate proofTemplate) {
        final Partner partner = partnerRepo.findById(partnerId)
                .orElseThrow(() -> new PartnerException("Partner not found"));
        if (!partner.hasConnectionId()) {
            throw new PartnerException("Partner has no aca-py connection");
        }

        ProofTemplateElementVisitor proofTemplateElementVisitor = new ProofTemplateElementVisitor(
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

    @NotNull
    private Stream<Pair<String, BPAAttribute>> pairSchemaIdWithAttributes(@NonNull BPAAttributeGroup ag) {
        Pair<String, BPAAttribute> pair = new Pair<>(ag.getSchemaId(), null);
        return ag.getAttributes().stream().map(pair::withRight);
    }

}
