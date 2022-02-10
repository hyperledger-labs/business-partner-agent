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

import io.micronaut.context.event.ApplicationEventPublisher;
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
import org.hyperledger.aries.api.credential_definition.CredentialDefinition;
import org.hyperledger.aries.api.credential_definition.CredentialDefinition.CredentialDefinitionRequest;
import org.hyperledger.aries.api.credentials.Credential;
import org.hyperledger.aries.api.credentials.CredentialAttributes;
import org.hyperledger.aries.api.credentials.CredentialPreview;
import org.hyperledger.aries.api.issue_credential_v1.*;
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecord;
import org.hyperledger.aries.api.revocation.RevokeRequest;
import org.hyperledger.aries.api.schema.SchemaSendResponse;
import org.hyperledger.bpa.api.aries.AriesCredential;
import org.hyperledger.bpa.api.aries.SchemaAPI;
import org.hyperledger.bpa.api.exception.EntityNotFoundException;
import org.hyperledger.bpa.api.exception.IssuerException;
import org.hyperledger.bpa.api.exception.NetworkException;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.api.notification.CredentialAcceptedEvent;
import org.hyperledger.bpa.api.notification.CredentialIssuedEvent;
import org.hyperledger.bpa.api.notification.CredentialProblemEvent;
import org.hyperledger.bpa.config.AcaPyConfig;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.config.RuntimeConfig;
import org.hyperledger.bpa.controller.api.issuer.CredDef;
import org.hyperledger.bpa.controller.api.issuer.CredEx;
import org.hyperledger.bpa.controller.api.issuer.IssueIndyCredentialRequest;
import org.hyperledger.bpa.impl.aries.schema.SchemaService;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.persistence.model.BPACredentialDefinition;
import org.hyperledger.bpa.persistence.model.BPACredentialExchange;
import org.hyperledger.bpa.persistence.model.BPASchema;
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
public class IssuerCredentialManager extends BaseIssuerManager {

    @Inject
    AriesClient ac;

    @Inject
    AcaPyConfig acaPyConfig;

    @Inject
    SchemaService schemaService;

    @Inject
    BPACredentialDefinitionRepository credDefRepo;

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    IssuerCredExRepository issuerCredExRepo;

    @Inject
    Converter conv;

    @Inject
    RuntimeConfig config;

    @Inject
    BPAMessageSource.DefaultMessageSource msg;

    @Inject
    ApplicationEventPublisher eventPublisher;

    // Indy Credential Definition Management

    public List<CredDef> listCredDefs() {
        List<CredDef> result = new ArrayList<>();
        credDefRepo.findAll().forEach(db -> result.add(CredDef.from(db)));
        return result;
    }

    public CredDef createCredDef(@NonNull String schemaId, @NonNull String tag, boolean supportRevocation) {
        CredDef result;
        try {
            String sId = StringUtils.strip(schemaId);
            String t = StringUtils.trim(tag);
            Optional<SchemaSendResponse.Schema> ariesSchema = ac.schemasGetById(sId);
            if (ariesSchema.isEmpty()) {
                throw new WrongApiUsageException(msg.getMessage("api.schema.restriction.schema.not.found.on.ledger",
                        Map.of("id", sId)));
            }

            Optional<BPASchema> bpaSchema = schemaService.getSchemaFor(sId);
            if (bpaSchema.isEmpty()) {
                // schema exists on ledger, but no in db, let's add it.
                SchemaAPI schema = schemaService.addIndySchema(ariesSchema.get().getId(), null, null, null);
                if (schema == null) {
                    throw new IssuerException(msg.getMessage("api.issuer.schema.failure", Map.of("id", sId)));
                }
                bpaSchema = schemaService.getSchemaFor(schema.getSchemaId());
            }
            // send credDef to ledger...
            // will create if needed, otherwise return existing...
            CredentialDefinitionRequest request = CredentialDefinitionRequest.builder()
                    .schemaId(schemaId)
                    .tag(t)
                    .supportRevocation(supportRevocation)
                    .revocationRegistrySize(config.getRevocationRegistrySize())
                    .build();
            Optional<CredentialDefinition.CredentialDefinitionResponse> response = ac
                    .credentialDefinitionsCreate(request);
            if (response.isPresent()) {
                // check to see if we have already saved this cred def.
                if (credDefRepo.findByCredentialDefinitionId(response.get().getCredentialDefinitionId()).isEmpty()) {
                    // doesn't exist, save it to the db...
                    BPACredentialDefinition credDef = BPACredentialDefinition.builder()
                            .schema(bpaSchema.orElseThrow())
                            .credentialDefinitionId(response.get().getCredentialDefinitionId())
                            .isSupportRevocation(supportRevocation)
                            .revocationRegistrySize(config.getRevocationRegistrySize())
                            .tag(t)
                            .build();
                    BPACredentialDefinition saved = credDefRepo.save(credDef);
                    result = CredDef.from(saved);
                } else {
                    throw new WrongApiUsageException(msg.getMessage("api.issuer.creddef.already.exists",
                            Map.of("id", sId, "tag", t)));
                }
            } else {
                log.error("Credential Definition not created.");
                throw new IssuerException(msg.getMessage("api.issuer.creddef.ledger.failure"));
            }
        } catch (IOException e) {
            log.error("aca-py not reachable", e);
            throw new NetworkException(msg.getMessage("acapy.unavailable"), e);
        }
        return result;
    }

    public void deleteCredDef(@NonNull UUID id) {
        int recs = issuerCredExRepo.countIdByCredDefId(id);
        if (recs == 0) {
            credDefRepo.deleteById(id);
        } else {
            throw new IssuerException(msg.getMessage("api.issuer.creddef.in.use"));
        }
    }

    // Indy Credential Management - Called By User

    /**
     * Issuer initialises the indy credential exchange with an offer. There is no
     * preexisting proposal from the holder.
     *
     * @param request {@link IssueIndyCredentialRequest}
     * @return credential exchange id
     */
    public String issueIndyCredential(@NonNull IssueIndyCredentialRequest request) {
        Partner dbPartner = partnerRepo.findById(request.getPartnerId())
                .orElseThrow(() -> new IssuerException(msg.getMessage("api.partner.not.found",
                        Map.of("id", request.getPartnerId()))));

        BPACredentialDefinition dbCredDef = credDefRepo.findById(request.getCredDefId())
                .orElseThrow(() -> new IssuerException(
                        msg.getMessage("api.issuer.creddef.not.found", Map.of("id", request.getCredDefId()))));

        Map<String, String> document = conv.toStringMap(request.getDocument());

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
                .credentialProposal(new CredentialPreview(CredentialAttributes.fromMap(document)))
                .credentialDefinitionId(credentialDefinitionId)
                .build();

        if (request.isV1()) {
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
        issuerCredExRepo.save(cex);

        fireCredentialIssuedEvent(cex);
        return exResult.getCredentialExchangeId();
    }

    public void reIssueIndyCredential(@NonNull UUID exchangeId) {
        BPACredentialExchange credEx = issuerCredExRepo.findById(exchangeId).orElseThrow(EntityNotFoundException::new);
        if (credEx.roleIsIssuer() && credEx.stateIsRevoked()) {
            issueIndyCredential(IssueIndyCredentialRequest.builder()
                    .partnerId(credEx.getPartner() != null ? credEx.getPartner().getId() : null)
                    .credDefId(credEx.getCredDef() != null ? credEx.getCredDef().getId() : null)
                    .document(conv.mapToNode(credEx.getIndyCredential().getAttrs()))
                    .exchangeVersion(credEx.getExchangeVersion())
                    .build());
        } else {
            throw new IssuerException(
                    msg.getMessage("api.issuer.reissue.wrong.state", Map.of("state", credEx.getState())));
        }
    }

    /**
     * Check if the supplied attributes match the schema
     *
     * @param document  the credential
     * @param dbCredDef {@link BPACredentialDefinition}
     */
    private void checkCredentialAttributes(Map<String, String> document, BPACredentialDefinition dbCredDef) {
        Set<String> documentAttributeNames = document.keySet();
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

    public CredEx revokeIndyCredential(@NonNull UUID id) {
        if (!config.getTailsServerConfigured()) {
            throw new IssuerException(msg.getMessage("api.issuer.no.tails.server"));
        }
        BPACredentialExchange credEx = issuerCredExRepo.findById(id).orElseThrow(EntityNotFoundException::new);
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

    @Override
    protected CredEx sendOffer(@NonNull BPACredentialExchange credEx, @NotNull Map<String, String> attributes,
            @NonNull IdWrapper ids) throws IOException {
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
                                .attributes(attributes.entrySet().stream().map(a -> CredAttrSpec
                                        .builder()
                                        .name(a.getKey()).value(a.getValue())
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

    // Credential Management - Called By Event Handler

    /**
     * In v1 (indy) this message can only be received after a preceding Credential
     * Offer, meaning the holder can never start with a Credential Request, so it is
     * ok to directly auto accept the request
     * 
     * @param ex {@link V1CredentialExchange}
     */
    public void handleV1CredentialRequest(@NonNull V1CredentialExchange ex) {
        try {
            if (Boolean.FALSE.equals(acaPyConfig.getAutoRespondCredentialRequest())) {
                ac.issueCredentialRecordsIssue(ex.getCredentialExchangeId(),
                        V1CredentialIssueRequest.builder().build());
            }
            handleV1CredentialExchange(ex); // save state changes
        } catch (IOException e) {
            log.error(msg.getMessage("acapy.unavailable"));
        }
    }

    /**
     * Handle issue credential v1 state changes and revocation info
     *
     * @param ex {@link V1CredentialExchange}
     */
    public void handleV1CredentialExchange(@NonNull V1CredentialExchange ex) {
        issuerCredExRepo.findByCredentialExchangeId(ex.getCredentialExchangeId()).ifPresent(bpaEx -> {
            boolean notDeclined = bpaEx.stateIsNotDeclined();
            CredentialExchangeState state = ex.getState() != null ? ex.getState() : CredentialExchangeState.PROBLEM;
            bpaEx.pushStates(state, ex.getUpdatedAt());
            if (StringUtils.isNotEmpty(ex.getErrorMsg())) {
                if (notDeclined) {
                    issuerCredExRepo.updateAfterEventNoRevocationInfo(bpaEx.getId(),
                            bpaEx.getState(), bpaEx.getStateToTimestamp(), ex.getErrorMsg());
                    fireCredentialProblemEvent(bpaEx);
                }
            } else {
                issuerCredExRepo.updateAfterEventWithRevocationInfo(bpaEx.getId(),
                        bpaEx.getState(), bpaEx.getStateToTimestamp(),
                        ex.getRevocRegId(), ex.getRevocationId(), ex.getErrorMsg());
            }
            if (ex.stateIsCredentialAcked() && ex.autoIssueEnabled()) {
                ex.findAttributesInCredentialOfferDict().ifPresent(
                        attr -> {
                            issuerCredExRepo.updateCredential(bpaEx.getId(), Credential.builder().attrs(attr).build());
                            fireCredentialAcceptedEvent(bpaEx);
                        });
            }
        });
    }

    // Events

    private void fireCredentialIssuedEvent(@NonNull BPACredentialExchange db) {
        eventPublisher.publishEventAsync(CredentialIssuedEvent.builder()
                .credential(AriesCredential.fromBPACredentialExchange(db, schemaLabel(db)))
                .build());
    }

    private void fireCredentialAcceptedEvent(@NonNull BPACredentialExchange db) {
        eventPublisher.publishEventAsync(CredentialAcceptedEvent.builder()
                .credential(AriesCredential.fromBPACredentialExchange(db, schemaLabel(db)))
                .build());
    }

    private void fireCredentialProblemEvent(@NonNull BPACredentialExchange db) {
        eventPublisher.publishEventAsync(CredentialProblemEvent.builder()
                .credential(AriesCredential.fromBPACredentialExchange(db, schemaLabel(db)))
                .build());
    }

    private String schemaLabel(@NonNull BPACredentialExchange db) {
        if (db.getIndyCredential() != null && db.getIndyCredential().getSchemaId() != null) {
            return schemaService.getSchemaLabel(db.getIndyCredential().getSchemaId());
        }
        return "";
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
