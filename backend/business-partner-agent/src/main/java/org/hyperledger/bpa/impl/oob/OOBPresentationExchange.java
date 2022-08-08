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
package org.hyperledger.bpa.impl.oob;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.present_proof.PresentProofRequest;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord;
import org.hyperledger.aries.api.present_proof.PresentationFreeOfferHelper;
import org.hyperledger.aries.config.GsonConfig;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.controller.api.invitation.APICreateInvitationResponse;
import org.hyperledger.bpa.controller.api.proof.RequestOOBPresentationRequest;
import org.hyperledger.bpa.impl.aries.prooftemplates.ProofTemplateManager;
import org.hyperledger.bpa.persistence.model.BPAProofTemplate;
import org.hyperledger.bpa.persistence.model.Partner;
import org.hyperledger.bpa.persistence.model.PartnerProof;
import org.hyperledger.bpa.persistence.model.converter.ExchangePayload;
import org.hyperledger.bpa.persistence.repository.PartnerProofRepository;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Singleton
public class OOBPresentationExchange extends OOBBase {

    private final ProofTemplateManager templateManager;

    private final PresentationFreeOfferHelper h;

    private final PartnerProofRepository partnerProofRepo;

    @Inject
    public OOBPresentationExchange(
            AriesClient ac,
            ProofTemplateManager templateManager,
            PartnerProofRepository partnerProofRepo) {
        this.h = PresentationFreeOfferHelper.builder()
                .acaPy(ac)
                .build();
        this.templateManager = templateManager;
        this.partnerProofRepo = partnerProofRepo;
    }

    public APICreateInvitationResponse requestConnectionLess(@NonNull RequestOOBPresentationRequest req) {
        PresentProofRequest proofRequest = templateManager.templateToIndyProofRequest(req.getTemplateId());
        PresentationFreeOfferHelper.PresentationFreeOffer freeOffer;
        if (req.exchangeIsV1()) {
            freeOffer= h.buildV1Indy(proofRequest);
        } else {
            freeOffer = h.buildV2Indy(proofRequest);
        }

        log.debug("{}", GsonConfig.defaultNoEscaping().toJson(freeOffer));

        Partner p = persistPartner(freeOffer.getInvitationRecord(), req.getAlias(), req.getTrustPing(), req.getTag());
        persistProof(req.getTemplateId(), freeOffer.getPresentationExchangeRecord(), p);

        return buildResponse(freeOffer.getInvitationRecord().getInviMsgId());
    }

    private void persistProof(
            @NonNull UUID templateId,
            @NonNull PresentationExchangeRecord ex,
            @NonNull Partner p) {
        final PartnerProof pp = PartnerProof
                .builder()
                .state(ex.getState())
                .type(CredentialType.INDY)
                .presentationExchangeId(ex.getPresentationExchangeId())
                .role(ex.getRole())
                .threadId(ex.getThreadId())
                .proofRequest(ExchangePayload.buildForProofRequest(ex))
                .proofTemplate(BPAProofTemplate.builder().id(templateId).build())
                .exchangeVersion(ex.getVersion())
                .pushStateChange(ex.getState(), Instant.now())
                .partner(p)
                .credentialExchange(null)
                .build();
        partnerProofRepo.save(pp);
    }
}
