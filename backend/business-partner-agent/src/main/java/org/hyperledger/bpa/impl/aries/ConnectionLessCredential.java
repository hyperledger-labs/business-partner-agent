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
package org.hyperledger.bpa.impl.aries;

import io.micronaut.context.annotation.Value;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.acy_py.generated.model.InvitationRecord;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialFreeOfferHelper;
import org.hyperledger.aries.config.GsonConfig;
import org.hyperledger.bpa.api.exception.EntityNotFoundException;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.controller.IssuerController;
import org.hyperledger.bpa.controller.api.issuer.IssueConnectionLessRequest;
import org.hyperledger.bpa.impl.activity.DocumentValidator;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.model.BPACredentialDefinition;
import org.hyperledger.bpa.model.BPACredentialExchange;
import org.hyperledger.bpa.model.Partner;
import org.hyperledger.bpa.repository.BPACredentialDefinitionRepository;
import org.hyperledger.bpa.repository.IssuerCredExRepository;
import org.hyperledger.bpa.repository.PartnerRepository;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

/**
 * First try on attaching a credential offer in the OOB invitation. As with the
 * connection-less proof request the request consists of two parts. As there is
 * no wallet that currently supports this the flow is not really tested.
 */
@Slf4j
@Singleton
public class ConnectionLessCredential {

    @Value("${bpa.did.prefix}")
    String didPrefix;

    @Value("${bpa.scheme}")
    String scheme;

    @Value("${bpa.host}")
    String host;

    @Inject
    Converter conv;

    @Inject
    BPACredentialDefinitionRepository credDefRepo;

    @Inject
    IssuerCredExRepository credExRepo;

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    DocumentValidator validator;

    @Inject
    BPAMessageSource.DefaultMessageSource ms;

    private final V1CredentialFreeOfferHelper h;

    @Inject
    public ConnectionLessCredential(AriesClient ac) {
        this.h = new V1CredentialFreeOfferHelper(ac);
    }

    /**
     * Step 1: Prepare offer und return URL
     * 
     * @param request {@link IssueConnectionLessRequest}
     * @return location of the offer
     */
    public URI issueConnectionLess(@NonNull IssueConnectionLessRequest request) {
        BPACredentialDefinition dbCredDef = credDefRepo.findById(request.getCredDefId())
                .orElseThrow(() -> new WrongApiUsageException(
                        ms.getMessage("api.issuer.creddef.not.found", Map.of("id", request.getCredDefId()))));
        validator.validateAttributesAgainstSchema(request.getDocument(), dbCredDef.getSchema().getSchemaId());

        Map<String, String> document = conv.toStringMap(request.getDocument());

        V1CredentialFreeOfferHelper.CredentialFreeOffer freeOffer = h
                .buildFreeOffer(dbCredDef.getCredentialDefinitionId(), document);

        log.debug("{}", GsonConfig.defaultNoEscaping().toJson(freeOffer));

        Partner p = persistPartner(freeOffer.getInvitationRecord());
        persistCredentialExchange(freeOffer, dbCredDef, p);

        return createURI(IssuerController.ISSUER_CONTROLLER_BASE_URL + "/issue-credential/connection-less/"
                + freeOffer.getInvitationRecord().getInviMsgId());
    }

    /**
     * Step 2: Return the base64 encoded invitation plus attachment
     * 
     * @param invMessageId invitation message id
     * @return base64 encoded invitation URL
     */
    public String handleConnectionLess(@NonNull UUID invMessageId) {
        log.debug("Handling connectionless credential request: {}", invMessageId);
        Partner ex = partnerRepo.findByInvitationMsgId(invMessageId.toString())
                .orElseThrow(EntityNotFoundException::new);
        if (ex.getInvitationRecord() == null) {
            throw new IllegalStateException(ms.getMessage("api.issuer.connectionless.invitation.not.found",
                    Map.of("id", invMessageId)));
        }
        // getInvitationUrl() has an encoding issue
        byte[] envelopeBase64 = Base64.getEncoder().encode(
                GsonConfig.defaultNoEscaping().toJson(
                        ex.getInvitationRecord().getInvitation()).getBytes(StandardCharsets.UTF_8));
        return "didcomm://" + host + "?oob=" + new String(envelopeBase64, StandardCharsets.UTF_8);
    }

    private void persistCredentialExchange(
            @NonNull V1CredentialFreeOfferHelper.CredentialFreeOffer r, @NonNull BPACredentialDefinition dbCredDef,
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
                        ? r.getCredentialProposalDict().getCredentialProposal()
                        : null);
        credExRepo.save(b.build());
    }

    private Partner persistPartner(InvitationRecord r) {
        return partnerRepo.save(Partner
                .builder()
                .ariesSupport(Boolean.TRUE)
                .invitationMsgId(r.getInviMsgId())
                .did(didPrefix + ConnectionManager.UNKNOWN_DID)
                .state(ConnectionState.INVITATION)
                .pushStateChange(ConnectionState.INVITATION, Instant.now())
                .invitationRecord(r)
                .incoming(Boolean.TRUE)
                .tags(new HashSet<>())
                .trustPing(Boolean.FALSE)
                .build());
    }

    private URI createURI(String path) {
        return URI.create(scheme + "://" + host + path);
    }
}
