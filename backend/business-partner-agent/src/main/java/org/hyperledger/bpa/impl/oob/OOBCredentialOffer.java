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
import org.hyperledger.aries.api.credentials.Credential;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.aries.api.issue_credential_v1.CredentialFreeOfferHelper;
import org.hyperledger.aries.config.GsonConfig;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.controller.InvitationController;
import org.hyperledger.bpa.controller.api.invitation.APICreateInvitationResponse;
import org.hyperledger.bpa.controller.api.issuer.IssueOOBCredentialRequest;
import org.hyperledger.bpa.impl.activity.DocumentValidator;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.persistence.model.BPACredentialDefinition;
import org.hyperledger.bpa.persistence.model.BPACredentialExchange;
import org.hyperledger.bpa.persistence.model.Partner;
import org.hyperledger.bpa.persistence.model.converter.ExchangePayload;
import org.hyperledger.bpa.persistence.repository.BPACredentialDefinitionRepository;
import org.hyperledger.bpa.persistence.repository.IssuerCredExRepository;

import java.time.Instant;
import java.util.Map;

/**
 * First try on attaching a credential offer in the OOB invitation. As with the
 * connection-less proof request the request consists of two parts. As there is
 * no wallet that currently supports this the flow is not really tested.
 */
@Slf4j
@Singleton
public class OOBCredentialOffer extends OOBBase {

    @Inject
    Converter conv;

    @Inject
    BPACredentialDefinitionRepository credDefRepo;

    @Inject
    IssuerCredExRepository credExRepo;

    @Inject
    DocumentValidator validator;

    @Inject
    BPAMessageSource.DefaultMessageSource ms;

    private final CredentialFreeOfferHelper h;

    @Inject
    public OOBCredentialOffer(AriesClient ac) {
        this.h = new CredentialFreeOfferHelper(ac);
    }

    /**
     * Step 1: Prepare offer und return URL
     *
     * @param req {@link IssueOOBCredentialRequest}
     * @return location of the offer
     */
    // TODO LD-Credential Support
    public APICreateInvitationResponse issueConnectionLess(@NonNull IssueOOBCredentialRequest req) {
        BPACredentialDefinition dbCredDef = credDefRepo.findById(req.getCredDefId())
                .orElseThrow(() -> new WrongApiUsageException(
                        ms.getMessage("api.issuer.creddef.not.found", Map.of("id", req.getCredDefId()))));
        validator.validateAttributesAgainstIndySchema(req.getDocument(), dbCredDef.getSchema().getSchemaId());

        Map<String, String> document = conv.toStringMap(req.getDocument());

        CredentialFreeOfferHelper.CredentialFreeOffer freeOffer;
        if (req.exchangeIsV1()) {
            freeOffer = h.buildFreeOffer(dbCredDef.getCredentialDefinitionId(), document);
        } else {
            freeOffer = h.buildFreeOfferIndyV2(dbCredDef.getCredentialDefinitionId(), document);
        }

        log.debug("{}", GsonConfig.defaultNoEscaping().toJson(freeOffer));

        Partner p = persistPartner(freeOffer.getInvitationRecord(), req.getAlias(), req.getTrustPing(), req.getTag());
        persistCredentialExchange(freeOffer, document, dbCredDef, p);

        return APICreateInvitationResponse.builder()
                .invitationUrl(
                        createURI(InvitationController.INVITATION_CONTROLLER_BASE_URL
                                + "/oob-attachment/"
                                + freeOffer.getInvitationRecord().getInviMsgId()).toString())
                .invitationId(freeOffer.getInvitationRecord().getInviMsgId())
                .build();
    }

    private void persistCredentialExchange(
            @NonNull CredentialFreeOfferHelper.CredentialFreeOffer r,
            @NonNull Map<String, String> document,
            @NonNull BPACredentialDefinition dbCredDef,
            @NonNull Partner p) {
        BPACredentialExchange.BPACredentialExchangeBuilder b = BPACredentialExchange
                .builder()
                .schema(dbCredDef.getSchema())
                .credDef(dbCredDef)
                .partner(p)
                .role(CredentialExchangeRole.ISSUER)
                .state(CredentialExchangeState.OFFER_SENT)
                .pushStateChange(CredentialExchangeState.OFFER_SENT, Instant.now())
                .credentialExchangeId(r.getCredentialExchangeId())
                .threadId(r.getThreadId())
                .credentialOffer(r.getCredentialProposalDict() != null
                        ? ExchangePayload
                                .indy(r.getCredentialProposalDict().getCredentialProposal())
                        : null)
                .indyCredential(Credential.builder()
                        .attrs(document)
                        .build());
        credExRepo.save(b.build());
    }
}
