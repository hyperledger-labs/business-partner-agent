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

import com.fasterxml.jackson.databind.JsonNode;
import io.micronaut.context.event.ApplicationEventPublisher;
import io.micronaut.core.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.acy_py.generated.model.*;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.ExchangeVersion;
import org.hyperledger.aries.api.credential_definition.CredentialDefinition;
import org.hyperledger.aries.api.credential_definition.CredentialDefinition.CredentialDefinitionRequest;
import org.hyperledger.aries.api.credentials.Credential;
import org.hyperledger.aries.api.credentials.CredentialAttributes;
import org.hyperledger.aries.api.credentials.CredentialPreview;
import org.hyperledger.aries.api.exception.AriesException;
import org.hyperledger.aries.api.issue_credential_v1.*;
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecord;
import org.hyperledger.aries.api.issue_credential_v2.V2IssueIndyCredentialEvent;
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
import org.hyperledger.bpa.api.notification.CredentialProposalEvent;
import org.hyperledger.bpa.config.AcaPyConfig;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.config.RuntimeConfig;
import org.hyperledger.bpa.controller.api.issuer.*;
import org.hyperledger.bpa.controller.api.issuer.CredDef;
import org.hyperledger.bpa.controller.api.issuer.CredEx;
import org.hyperledger.bpa.controller.api.issuer.CredentialOfferRequest;
import org.hyperledger.bpa.controller.api.issuer.IssueCredentialSendRequest;
import org.hyperledger.bpa.impl.aries.schema.SchemaService;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.persistence.model.BPACredentialDefinition;
import org.hyperledger.bpa.persistence.model.BPACredentialExchange;
import org.hyperledger.bpa.persistence.model.BPASchema;
import org.hyperledger.bpa.persistence.model.Partner;
import org.hyperledger.bpa.persistence.repository.BPACredentialDefinitionRepository;
import org.hyperledger.bpa.persistence.repository.IssuerCredExRepository;
import org.hyperledger.bpa.persistence.repository.PartnerRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class IssuerCredentialManager extends BaseCredentialManager {

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

    // Credential Definition Management

    public List<CredDef> listCredDefs() {
        List<CredDef> result = new ArrayList<>();
        credDefRepo.findAll().forEach(db -> result.add(CredDef.from(db)));
        return result;
    }

    public List<IssuanceTemplate> listIssuanceTemplates() {
        List<IssuanceTemplate> result = new ArrayList<>();
        credDefRepo.findAll().forEach(db -> result.add(IssuanceTemplate.from(db)));
        // schemaService.listSchemas()
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

    // Credential Management - Called By User

    /**
     * Issuer initialises the credential exchange with an offer. There is no
     * preexisting proposal from the holder.
     *
     * @param request {@link IssueCredentialRequest}
     * @return credential exchange id
     */
    public String issueCredential(@NonNull IssueCredentialRequest request) {
        Partner dbPartner = partnerRepo.findById(request.getPartnerId())
                .orElseThrow(() -> new IssuerException(msg.getMessage("api.partner.not.found",
                        Map.of("id", request.getPartnerId()))));

        BPACredentialDefinition dbCredDef = credDefRepo.findById(request.getCredDefId())
                .orElseThrow(() -> new IssuerException(
                        msg.getMessage("api.issuer.creddef.not.found", Map.of("id", request.getCredDefId()))));

        Map<String, String> document = conv.toStringMap(request.getDocument());

        checkAttributes(document, dbCredDef);

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
            exResult = sendV1Credential(proposal);
        } else {
            exVersion = ExchangeVersion.V2;
            exResult = sendV2Credential(proposal);
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

    public void reIssueCredential(@NonNull UUID exchangeId) {
        BPACredentialExchange credEx = issuerCredExRepo.findById(exchangeId).orElseThrow(EntityNotFoundException::new);
        if (credEx.roleIsIssuer() && credEx.stateIsRevoked()) {
            issueCredential(IssueCredentialRequest.builder()
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
    private void checkAttributes(Map<String, String> document, BPACredentialDefinition dbCredDef) {
        Set<String> documentAttributeNames = document.keySet();
        Set<String> schemaAttributeNames = dbCredDef.getSchema().getSchemaAttributeNames();
        if (!documentAttributeNames.equals(schemaAttributeNames)) {
            throw new IssuerException(msg.getMessage("api.issuer.credential.document.mismatch",
                    Map.of("doc", documentAttributeNames, "schema", schemaAttributeNames)));
        }
    }

    private ExchangeResult sendV1Credential(@NonNull V1CredentialProposalRequest proposal) {
        try {
            return ac.issueCredentialSend(proposal)
                    .map(ExchangeResult::fromV1)
                    .orElseThrow();
        } catch (IOException e) {
            throw new NetworkException(msg.getMessage("acapy.unavailable"), e);
        }
    }

    private ExchangeResult sendV2Credential(@NonNull V1CredentialProposalRequest proposal) {
        try {
            return ac.issueCredentialV2Send(proposal)
                    .map(ExchangeResult::fromV2)
                    .orElseThrow();
        } catch (IOException e) {
            throw new NetworkException(msg.getMessage("acapy.unavailable"), e);
        }
    }

    public List<CredEx> listCredentialExchanges(@Nullable CredentialExchangeRole role, @Nullable UUID partnerId) {
        List<BPACredentialExchange> exchanges = issuerCredExRepo.listOrderByUpdatedAtDesc();
        // now, lets get credentials...
        return exchanges.stream()
                .filter(x -> {
                    if (role != null) {
                        return role.equals(x.getRole());
                    }
                    return true;
                })
                .filter(x -> x.getPartner() != null)
                .filter(x -> {
                    if (partnerId != null) {
                        return x.getPartner().getId().equals(partnerId);
                    }
                    return true;
                })
                .map(ex -> CredEx.from(ex, conv.toAPIObject(ex.getPartner())))
                .collect(Collectors.toList());
    }

    public CredEx getCredEx(@NonNull UUID id) {
        BPACredentialExchange credEx = issuerCredExRepo.findById(id).orElseThrow(EntityNotFoundException::new);
        return CredEx.from(credEx, conv.toAPIObject(credEx.getPartner()));
    }

    public CredEx revokeCredentialExchange(@NonNull UUID id) {
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

    /**
     * Send partner a credential (counter) offer in reference to a proposal (Not to
     * be confused with the automated send-offer flow).
     *
     * @param id           credential exchange id
     * @param counterOffer {@link CredentialOfferRequest}
     * @return {@link CredEx} updated credential exchange, if found
     */
    public CredEx sendCredentialOffer(@NonNull UUID id, @NonNull CredentialOfferRequest counterOffer) {
        BPACredentialExchange credEx = issuerCredExRepo.findById(id).orElseThrow(EntityNotFoundException::new);
        if (!credEx.stateIsProposalReceived()) {
            throw new WrongApiUsageException(msg.getMessage("api.issuer.credential.send.offer.wrong.state",
                    Map.of("state", credEx.getState())));
        }
        List<CredentialAttributes> attributes;
        if (counterOffer.acceptAll()) {
            attributes = credEx.getCredentialProposal() != null
                    ? credEx.getCredentialProposal().getIndy().getAttributes()
                    : List.of();
        } else {
            attributes = counterOffer.toCredentialAttributes();
        }
        String credDefId = credEx.getCredDef() != null ? credEx.getCredDef().getCredentialDefinitionId() : null;
        if (StringUtils.isNotEmpty(counterOffer.getCredDefId()) && !counterOffer.getCredDefId().equals(credDefId)) {
            BPACredentialDefinition counterCredDef = credDefRepo
                    .findByCredentialDefinitionId(counterOffer.getCredDefId())
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
                                .attributes(attributes.stream().map(a -> CredAttrSpec
                                        .builder()
                                        .name(a.getName()).value(a.getValue()).mimeType(a.getMimeType())
                                        .build()).collect(Collectors.toList()))
                                .build())
                        .build())
                .build();
        try {
            if (ExchangeVersion.V1.equals(credEx.getExchangeVersion())) {
                ac.issueCredentialRecordsSendOffer(credEx.getCredentialExchangeId(), v1Offer);
            } else {
                ac.issueCredentialV2RecordsSendOffer(credEx.getCredentialExchangeId(), v1Offer);
            }
        } catch (IOException e) {
            throw new NetworkException(msg.getMessage("acapy.unavailable"), e);
        } catch (AriesException e) {
            if (e.getCode() == 400) {
                String message = msg.getMessage("api.issuer.credential.exchange.problem");
                credEx.pushStates(CredentialExchangeState.PROBLEM);
                issuerCredExRepo.updateAfterEventNoRevocationInfo(
                        credEx.getId(), credEx.getState(), credEx.getStateToTimestamp(), message);
                throw new WrongApiUsageException(message);
            }
            throw e;
        }
        Credential credential = Credential.builder()
                .attrs(attributes.stream()
                        .collect(Collectors.toMap(CredentialAttributes::getName, CredentialAttributes::getValue)))
                .build();
        credEx.setIndyCredential(credential);
        issuerCredExRepo.updateCredential(credEx.getId(), credential);
        return CredEx.from(credEx);
    }

    public void declineCredentialProposal(@NonNull UUID id, @Nullable String message) {
        if (StringUtils.isEmpty(message)) {
            message = msg.getMessage("api.issuer.credential.exchange.declined");
        }
        BPACredentialExchange credEx = getCredentialExchange(id);
        credEx.pushStates(CredentialExchangeState.DECLINED, Instant.now());
        issuerCredExRepo.updateAfterEventNoRevocationInfo(credEx.getId(), credEx.getState(),
                credEx.getStateToTimestamp(),
                message);
        declineCredentialExchange(credEx, message);
    }

    // Credential Management - Called By Event Handler

    public void handleCredentialProposal(@NonNull V1CredentialExchange ex, ExchangeVersion exchangeVersion) {
        partnerRepo.findByConnectionId(ex.getConnectionId()).ifPresent(partner -> {
            BPACredentialExchange.BPACredentialExchangeBuilder b = BPACredentialExchange
                    .builder()
                    .partner(partner)
                    .role(CredentialExchangeRole.ISSUER)
                    .state(ex.getState())
                    .pushStateChange(ex.getState(), Instant.now())
                    .exchangeVersion(exchangeVersion)
                    .credentialExchangeId(ex.getCredentialExchangeId())
                    .threadId(ex.getThreadId())
                    .credentialProposal(ex.getCredentialProposalDict() != null
                            ? BPACredentialExchange.ExchangePayload
                                    .indy(ex.getCredentialProposalDict().getCredentialProposal())
                            : null);
            // preselecting first match
            credDefRepo.findBySchemaId(ex.getCredentialProposalDict().getSchemaId()).stream().findFirst()
                    .ifPresentOrElse(dbCredDef -> {
                        b.schema(dbCredDef.getSchema()).credDef(dbCredDef);
                        issuerCredExRepo.save(b.build());
                    }, () -> {
                        b.errorMsg(msg.getMessage("api.holder.issuer.has.no.creddef",
                                Map.of("id", ex.getCredentialProposalDict().getSchemaId())));
                        issuerCredExRepo.save(b.build());
                    });
            fireCredentialProposalEvent();
        });
    }

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

    /**
     * Handle issue credential v2 state changes
     *
     * @param ex {@link V20CredExRecord}
     */
    public void handleV2CredentialExchange(@NonNull V20CredExRecord ex) {
        issuerCredExRepo.findByCredentialExchangeId(ex.getCredentialExchangeId())
                .ifPresent(bpaEx -> {
                    if (bpaEx.stateIsNotDeclined()) {
                        CredentialExchangeState state = ex.getState();
                        if (StringUtils.isNotEmpty(ex.getErrorMsg())) {
                            state = CredentialExchangeState.PROBLEM;
                        }
                        bpaEx.pushStates(state, ex.getUpdatedAt());
                        issuerCredExRepo.updateAfterEventNoRevocationInfo(bpaEx.getId(),
                                bpaEx.getState(), bpaEx.getStateToTimestamp(), ex.getErrorMsg());
                        if (ex.stateIsCredentialIssued() && ex.autoIssueEnabled()) {
                            ex.getByFormat().findValuesInIndyCredIssue().ifPresent(
                                    attr -> issuerCredExRepo.updateCredential(bpaEx.getId(),
                                            Credential.builder().attrs(attr).build()));
                        }
                    }
                });
    }

    /**
     * Handle issue credential v2 revocation info
     *
     * @param revocationInfo {@link V2IssueIndyCredentialEvent}
     */
    public void handleIssueCredentialV2Indy(V2IssueIndyCredentialEvent revocationInfo) {
        // Note: This event contains no role info, so we have to check this here
        // explicitly
        issuerCredExRepo.findByCredentialExchangeId(revocationInfo.getCredExId()).ifPresent(bpaEx -> {
            if (bpaEx.roleIsIssuer() && StringUtils.isNotEmpty(revocationInfo.getRevRegId())) {
                issuerCredExRepo.updateRevocationInfo(bpaEx.getId(), revocationInfo.getRevRegId(),
                        revocationInfo.getCredRevId());
            } else if (bpaEx.roleIsHolder() && StringUtils.isNotEmpty(revocationInfo.getCredIdStored())) {
                issuerCredExRepo.updateReferent(bpaEx.getId(), revocationInfo.getCredIdStored());
                // holder event is missing the credRevId
                try {
                    ac.credential(revocationInfo.getCredIdStored()).ifPresent(
                            c -> issuerCredExRepo.updateRevocationInfo(bpaEx.getId(), c.getRevRegId(), c.getCredRevId()));
                } catch (IOException e) {
                    log.error(msg.getMessage("acapy.unavailable"));
                }
            }
        });
    }

    /**
     * In v2 (indy and w3c) a holder can decide to skip negotiation and directly
     * start the whole flow with a request. So we check if there is a preceding
     * record if not decline with problem report TODO support v2 credential request
     * without prior negotiation
     * 
     * @param ex {@link V20CredExRecord v2CredEx}
     */
    public void handleV2CredentialRequest(@NonNull V20CredExRecord ex) {
        issuerCredExRepo.findByCredentialExchangeId(ex.getCredentialExchangeId()).ifPresentOrElse(db -> {
            try {
                if (Boolean.FALSE.equals(acaPyConfig.getAutoRespondCredentialRequest())) {
                    ac.issueCredentialV2RecordsIssue(ex.getCredentialExchangeId(),
                            V20CredIssueRequest.builder().build());
                }
                db.pushStates(ex.getState(), ex.getUpdatedAt());
                issuerCredExRepo.updateAfterEventNoRevocationInfo(db.getId(),
                        db.getState(), db.getStateToTimestamp(), ex.getErrorMsg());
            } catch (IOException e) {
                log.error(msg.getMessage("acapy.unavailable"));
            }
        }, () -> {
            try {
                ac.issueCredentialV2RecordsProblemReport(ex.getCredentialExchangeId(), V20CredIssueProblemReportRequest
                        .builder()
                        .description(
                                "starting a credential exchange without prior negotiation is not supported by this agent")
                        .build());
                log.warn("Received credential request without existing offer, dropping request");
            } catch (IOException e) {
                log.error(msg.getMessage("acapy.unavailable"));
            }
        });
    }

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

    private void fireCredentialProposalEvent() {
        eventPublisher.publishEventAsync(new CredentialProposalEvent());
    }

    private String schemaLabel(@NonNull BPACredentialExchange db) {
        if (db.getIndyCredential() != null && db.getIndyCredential().getSchemaId() != null) {
            return schemaService.getSchemaLabel(db.getIndyCredential().getSchemaId());
        }
        return "";
    }

    /**
     * Internal transfer POJO
     */
    @Data
    @Builder
    public static final class IssueCredentialRequest {
        private UUID credDefId;
        private UUID partnerId;
        private ExchangeVersion exchangeVersion;
        private JsonNode document;

        public boolean isV1() {
            return exchangeVersion == null || ExchangeVersion.V1.equals(exchangeVersion);
        }

        public static IssueCredentialRequest from(IssueCredentialSendRequest r) {
            return IssueCredentialRequest
                    .builder()
                    .credDefId(UUID.fromString(r.getCredDefId()))
                    .partnerId(UUID.fromString(r.getPartnerId()))
                    .exchangeVersion(r.getExchangeVersion())
                    .document(r.getDocument())
                    .build();
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
