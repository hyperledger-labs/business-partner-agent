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
package org.hyperledger.bpa.impl.aries.credential;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.acy_py.generated.model.CredAttrSpec;
import org.hyperledger.acy_py.generated.model.CredentialProposal;
import org.hyperledger.acy_py.generated.model.V10CredentialBoundOfferRequest;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.ExchangeVersion;
import org.hyperledger.aries.api.credentials.Credential;
import org.hyperledger.aries.api.credentials.CredentialAttributes;
import org.hyperledger.aries.api.credentials.CredentialPreview;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialProposalRequest;
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecord;
import org.hyperledger.aries.api.revocation.RevokeRequest;
import org.hyperledger.bpa.api.exception.IssuerException;
import org.hyperledger.bpa.api.exception.NetworkException;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.controller.api.issuer.CredEx;
import org.hyperledger.bpa.controller.api.issuer.IssueCredentialRequest;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.persistence.model.BPACredentialDefinition;
import org.hyperledger.bpa.persistence.model.BPACredentialExchange;
import org.hyperledger.bpa.persistence.model.Partner;
import org.hyperledger.bpa.persistence.repository.BPACredentialDefinitionRepository;
import org.hyperledger.bpa.persistence.repository.IssuerCredExRepository;
import org.hyperledger.bpa.persistence.repository.PartnerRepository;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles all credential issuer logic that is specific to indy
 */
@Slf4j
@Singleton
public class IssuerIndyManager {

    @Inject
    AriesClient ac;

    @Inject
    BPACredentialDefinitionRepository credDefRepo;

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    IssuerCredExRepository issuerCredExRepo;

    @Inject
    Converter conv;

    @Inject
    BPAMessageSource.DefaultMessageSource msg;

    // Indy Credential Management - Called By User

    /**
     * Issuer initialises the indy credential exchange with an offer. There is no
     * preexisting proposal from the holder.
     *
     * @param request {@link IssueCredentialRequest}
     * @return {@link BPACredentialExchange}
     */
    public BPACredentialExchange issueIndyCredential(
            @NonNull IssueCredentialRequest.IssueIndyCredentialRequest request) {
        Partner dbPartner = partnerRepo.findById(request.getPartnerId())
                .orElseThrow(() -> new IssuerException(msg.getMessage("api.partner.not.found",
                        Map.of("id", request.getPartnerId()))));

        BPACredentialDefinition dbCredDef = credDefRepo.findById(request.getCredDefId())
                .orElseThrow(() -> new IssuerException(
                        msg.getMessage("api.issuer.creddef.not.found", Map.of("id", request.getCredDefId()))));

        // TODO: Use object array with name, value and mime-type instead of Map
        List<CredentialAttributes> document = request.getDocument();

        checkCredentialAttributes(document, dbCredDef);

        String connectionId = dbPartner.getConnectionId();
        String schemaId = dbCredDef.getSchema().getSchemaId();
        String credentialDefinitionId = dbCredDef.getCredentialDefinitionId();

        ExchangeResult exResult;
        ExchangeVersion exVersion;

        V1CredentialProposalRequest proposal = V1CredentialProposalRequest
                .builder()
                .connectionId(Objects.requireNonNull(connectionId))
                .schemaId(schemaId)
                .credentialProposal(new CredentialPreview(request.getDocument()))
                .credentialDefinitionId(credentialDefinitionId)
                .build();

        if (request.exchangeIsV1()) {
            exVersion = ExchangeVersion.V1;
            exResult = sendV1IndyCredential(proposal);
        } else {
            exVersion = ExchangeVersion.V2;
            exResult = sendV2IndyCredential(proposal);
        }

        BPACredentialExchange cex = BPACredentialExchange.builder()
                .schema(dbCredDef.getSchema())
                .partner(dbPartner)
                .credDef(dbCredDef)
                .role(CredentialExchangeRole.ISSUER)
                .state(CredentialExchangeState.OFFER_SENT)
                .pushStateChange(CredentialExchangeState.OFFER_SENT, Instant.now())
                // as I'm the issuer I know what I have issued, no need to get this info from
                // the exchange record again
                .indyCredential(Credential.builder()
                        .schemaId(schemaId)
                        .attrs(document)
                        .build())
                .credentialExchangeId(exResult.getCredentialExchangeId())
                .threadId(exResult.getThreadId())
                .exchangeVersion(exVersion)
                .build();
        return issuerCredExRepo.save(cex);
    }

    public void reIssueIndyCredential(@NonNull BPACredentialExchange credEx) {
        if (credEx.roleIsIssuer() && credEx.stateIsRevoked()) {
            issueIndyCredential(IssueCredentialRequest.IssueIndyCredentialRequest.builder()
                    .partnerId(credEx.getPartner() != null ? credEx.getPartner().getId() : null)
                    .credDefId(credEx.getCredDef() != null ? credEx.getCredDef().getId() : null)
                    .document(new ArrayList<>(Objects.requireNonNull(credEx.getIndyCredential()).getAttrs()))
                    .exchangeVersion(credEx.getExchangeVersion())
                    .build());
        } else {
            throw new IssuerException(
                    msg.getMessage("api.issuer.reissue.wrong.state", Map.of("state", credEx.getState())));
        }
    }

    public CredEx revokeIndyCredential(BPACredentialExchange credEx) {
        if (StringUtils.isEmpty(credEx.getRevRegId())) {
            throw new IssuerException(msg.getMessage("api.issuer.credential.missing.revocation.info"));
        }
        try {
            ac.revocationRevoke(RevokeRequest
                    .builder()
                    .credRevId(credEx.getCredRevId())
                    .revRegId(credEx.getRevRegId())
                    .publish(Boolean.TRUE)
                    .connectionId(credEx.getPartner() != null ? credEx.getPartner().getConnectionId() : null)
                    .notify(Boolean.TRUE)
                    .build());
            credEx.setRevoked(Boolean.TRUE);
            credEx.pushStates(CredentialExchangeState.CREDENTIAL_REVOKED);
            issuerCredExRepo.update(credEx);
            return CredEx.from(credEx);
        } catch (IOException e) {
            throw new NetworkException(msg.getMessage("acapy.unavailable"), e);
        }
    }

    public CredEx sendOffer(@NonNull BPACredentialExchange credEx, @NotNull List<CredentialAttributes> attributes,
            @NonNull IssuerManager.IdWrapper ids) throws IOException {
        String credDefId = credEx.getCredDef() != null ? credEx.getCredDef().getCredentialDefinitionId() : null;
        if (StringUtils.isNotEmpty(ids.credDefId()) && !StringUtils.equals(credDefId, ids.credDefId())) {
            BPACredentialDefinition counterCredDef = credDefRepo
                    .findByCredentialDefinitionId(ids.credDefId())
                    .orElseThrow(() -> new WrongApiUsageException(
                            msg.getMessage("api.issuer.credential.send.offer.wrong.creddef")));
            credDefId = counterCredDef.getCredentialDefinitionId();
            credEx.setCredDef(counterCredDef);
            issuerCredExRepo.update(credEx);
        }
        V10CredentialBoundOfferRequest v1Offer = V10CredentialBoundOfferRequest
                .builder()
                .counterProposal(CredentialProposal
                        .builder()
                        .schemaId(credEx.getSchema() != null ? credEx.getSchema().getSchemaId() : null)
                        .credDefId(credDefId)
                        .credentialProposal(org.hyperledger.acy_py.generated.model.CredentialPreview
                                .builder()
                                .attributes(attributes.stream().map(attr -> CredAttrSpec
                                        .builder()
                                        .name(attr.getName())
                                        .value(attr.getValue())
                                        .mimeType(attr.getMimeType())
                                        .build()).collect(Collectors.toList()))
                                .build())
                        .build())
                .build();

        if (ExchangeVersion.V1.equals(credEx.getExchangeVersion())) {
            ac.issueCredentialRecordsSendOffer(credEx.getCredentialExchangeId(), v1Offer);
        } else {
            ac.issueCredentialV2RecordsSendOffer(credEx.getCredentialExchangeId(), v1Offer);
        }

        Credential credential = Credential.builder()
                .attrs(attributes)
                .build();
        credEx.setIndyCredential(credential);
        issuerCredExRepo.updateCredential(credEx.getId(), credential);
        return CredEx.from(credEx);
    }

    /**
     * Check if the supplied attributes match the schema
     *
     * @param document  the credential
     * @param dbCredDef {@link BPACredentialDefinition}
     */
    private void checkCredentialAttributes(List<CredentialAttributes> document,
            BPACredentialDefinition dbCredDef) {
        Set<String> documentAttributeNames = document.stream().map(CredentialAttributes::getName)
                .collect(Collectors.toSet());
        Set<String> schemaAttributeNames = dbCredDef.getSchema().getSchemaAttributeNames();
        if (!documentAttributeNames.equals(schemaAttributeNames)) {
            throw new IssuerException(msg.getMessage("api.issuer.credential.document.mismatch",
                    Map.of("doc", documentAttributeNames, "schema", schemaAttributeNames)));
        }
    }

    private ExchangeResult sendV1IndyCredential(@NonNull V1CredentialProposalRequest proposal) {
        try {
            return ac.issueCredentialSend(proposal)
                    .map(ExchangeResult::fromV1)
                    .orElseThrow();
        } catch (IOException e) {
            throw new NetworkException(msg.getMessage("acapy.unavailable"), e);
        }
    }

    private ExchangeResult sendV2IndyCredential(@NonNull V1CredentialProposalRequest proposal) {
        try {
            return ac.issueCredentialV2Send(proposal)
                    .map(ExchangeResult::fromV2)
                    .orElseThrow();
        } catch (IOException e) {
            throw new NetworkException(msg.getMessage("acapy.unavailable"), e);
        }
    }

    @Data
    @Builder
    private static final class ExchangeResult {
        private String credentialExchangeId;
        private String threadId;

        public static ExchangeResult fromV1(@NonNull V1CredentialExchange ex) {
            return ExchangeResult
                    .builder()
                    .credentialExchangeId(ex.getCredentialExchangeId())
                    .threadId(ex.getThreadId())
                    .build();
        }

        public static ExchangeResult fromV2(@NonNull V20CredExRecord ex) {
            return ExchangeResult
                    .builder()
                    .credentialExchangeId(ex.getCredentialExchangeId())
                    .threadId(ex.getThreadId())
                    .build();
        }
    }
}
