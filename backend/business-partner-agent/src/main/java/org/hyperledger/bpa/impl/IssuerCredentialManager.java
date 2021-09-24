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
package org.hyperledger.bpa.impl;

import com.fasterxml.jackson.databind.JsonNode;
import io.micronaut.core.annotation.Nullable;
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
import org.hyperledger.aries.api.credential_definition.CredentialDefinition;
import org.hyperledger.aries.api.credential_definition.CredentialDefinition.CredentialDefinitionRequest;
import org.hyperledger.aries.api.credentials.Credential;
import org.hyperledger.aries.api.credentials.CredentialAttributes;
import org.hyperledger.aries.api.credentials.CredentialPreview;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialProposalRequest;
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecord;
import org.hyperledger.aries.api.issue_credential_v2.V2IssueIndyCredentialEvent;
import org.hyperledger.aries.api.revocation.RevokeRequest;
import org.hyperledger.aries.api.schema.SchemaSendResponse;
import org.hyperledger.bpa.api.aries.ExchangeVersion;
import org.hyperledger.bpa.api.aries.SchemaAPI;
import org.hyperledger.bpa.api.exception.EntityNotFoundException;
import org.hyperledger.bpa.api.exception.IssuerException;
import org.hyperledger.bpa.api.exception.NetworkException;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.config.RuntimeConfig;
import org.hyperledger.bpa.controller.api.issuer.CredDef;
import org.hyperledger.bpa.controller.api.issuer.CredEx;
import org.hyperledger.bpa.controller.api.issuer.CredentialOfferRequest;
import org.hyperledger.bpa.controller.api.issuer.IssueCredentialSendRequest;
import org.hyperledger.bpa.impl.aries.config.SchemaService;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.model.BPACredentialDefinition;
import org.hyperledger.bpa.model.BPACredentialExchange;
import org.hyperledger.bpa.model.BPASchema;
import org.hyperledger.bpa.model.Partner;
import org.hyperledger.bpa.repository.BPACredentialDefinitionRepository;
import org.hyperledger.bpa.repository.BPACredentialExchangeRepository;
import org.hyperledger.bpa.repository.PartnerRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class IssuerCredentialManager {

    @Inject
    AriesClient ac;

    @Inject
    SchemaService schemaService;

    @Inject
    BPACredentialDefinitionRepository credDefRepo;

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    BPACredentialExchangeRepository credExRepo;

    @Inject
    Converter conv;

    @Inject
    RuntimeConfig config;

    @Inject
    BPAMessageSource.DefaultMessageSource msg;

    // Credential Definition Management

    public List<CredDef> listCredDefs() {
        List<CredDef> result = new ArrayList<>();
        credDefRepo.findAll().forEach(db -> result.add(CredDef.from(db)));
        return result;
    }

    public CredDef createCredDef(@NonNull String schemaId, @NonNull String tag, boolean supportRevocation) {
        CredDef result;
        try {
            String sId = StringUtils.strip(schemaId);
            Optional<SchemaSendResponse.Schema> ariesSchema = ac.schemasGetById(sId);
            if (ariesSchema.isEmpty()) {
                throw new WrongApiUsageException(String.format("No schema with id '%s' found on ledger.", sId));
            }

            Optional<BPASchema> bpaSchema = schemaService.getSchemaFor(sId);
            if (bpaSchema.isEmpty()) {
                // schema exists on ledger, but no in db, let's add it.
                SchemaAPI schema = schemaService.addSchema(ariesSchema.get().getId(), null, null, null);
                if (schema == null) {
                    throw new IssuerException(String.format("Could not add schema with id '%s' to database.", sId));
                }
                bpaSchema = schemaService.getSchemaFor(schema.getSchemaId());
            }
            // send creddef to ledger...
            // will create if needed, otherwise return existing...
            CredentialDefinitionRequest request = CredentialDefinitionRequest.builder()
                    .schemaId(schemaId)
                    .tag(tag)
                    .supportRevocation(supportRevocation)
                    .revocationRegistrySize(config.getRevocationRegistrySize())
                    .build();
            Optional<CredentialDefinition.CredentialDefinitionResponse> response = ac
                    .credentialDefinitionsCreate(request);
            if (response.isPresent()) {
                // check to see if we have already saved this cred def.
                if (credDefRepo.findByCredentialDefinitionId(response.get().getCredentialDefinitionId()).isEmpty()) {
                    // doesn't exist, save it to the db...
                    BPACredentialDefinition cdef = BPACredentialDefinition.builder()
                            .schema(bpaSchema.get())
                            .credentialDefinitionId(response.get().getCredentialDefinitionId())
                            .isSupportRevocation(supportRevocation)
                            .revocationRegistrySize(config.getRevocationRegistrySize())
                            .tag(tag)
                            .build();
                    BPACredentialDefinition saved = credDefRepo.save(cdef);
                    result = CredDef.from(saved);
                } else {
                    throw new WrongApiUsageException(
                            String.format("Schema already as a Credential Definition with tag '%s'", tag));
                }
            } else {
                log.error("Credential Definition not created.");
                throw new IssuerException("Credential Definition not created; could not complete request with ledger");
            }
        } catch (IOException e) {
            log.error("aca-py not reachable", e);
            throw new NetworkException(msg.getMessage("acapy.unavailable"), e);
        }
        return result;
    }

    public void deleteCredDef(@NonNull UUID id) {
        int recs = credExRepo.countIdByCredDefId(id);
        if (recs == 0) {
            credDefRepo.deleteById(id);
        } else {
            throw new IssuerException("Credential Definition cannot be deleted, it has been used to issue credentials");
        }
    }

    // Credential Management - Called By User

    public String issueCredential(@NonNull IssueCredentialRequest request) {
        Partner dbPartner = partnerRepo.findById(request.getPartnerId())
                .orElseThrow(() -> new IssuerException(String.format("Could not find partner with id '%s'",
                        request.getPartnerId())));

        BPACredentialDefinition dbCredDef = credDefRepo.findById(request.getCredDefId())
                .orElseThrow(() -> new IssuerException(
                        String.format("Could not find credential definition with id '%s'", request.getCredDefId())));

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
                .credential(Credential.builder()
                        .attrs(document)
                        .build())
                .credentialExchangeId(exResult.getCredentialExchangeId())
                .threadId(exResult.getThreadId())
                .exchangeVersion(exVersion)
                .build();
        credExRepo.save(cex);

        return exResult.getCredentialExchangeId();
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
            throw new IssuerException(String.format("Document attributes %s do not match schema attributes %s",
                    documentAttributeNames, schemaAttributeNames));
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
        List<BPACredentialExchange> exchanges = credExRepo.listOrderByUpdatedAtDesc();
        // now, lets get credentials...
        return exchanges.stream()
                .filter(x -> {
                    if (role != null) {
                        return role.equals(x.getRole());
                    }
                    return true;
                })
                .filter(x -> {
                    if (partnerId != null && x.getPartner() != null) {
                        return x.getPartner().getId().equals(partnerId);
                    }
                    return true;
                })
                .map(ex -> CredEx.from(ex, conv.toAPIObject(ex.getPartner())))
                .collect(Collectors.toList());
    }

    public CredEx revokeCredentialExchange(@NonNull UUID id) {
        if (!config.getTailsServerConfigured()) {
            throw new IssuerException(msg.getMessage("api.issuer.no.tails.server"));
        }
        BPACredentialExchange credEx = credExRepo.findById(id).orElseThrow(EntityNotFoundException::new);
        if (StringUtils.isEmpty(credEx.getRevRegId())) {
            throw new IssuerException(msg.getMessage("api.issuer.credential.missing.revocation.info"));
        }
        try {
            ac.revocationRevoke(RevokeRequest
                    .builder()
                    .credRevId(credEx.getCredRevId())
                    .revRegId(credEx.getRevRegId())
                    .publish(Boolean.TRUE)
                    .build());
            credEx.setRevoked(Boolean.TRUE);
            credEx.setState(CredentialExchangeState.REVOKED);
            credEx.pushStateChange(CredentialExchangeState.REVOKED, Instant.now());
            credExRepo.update(credEx);
            return CredEx.from(credEx);
        } catch (IOException e) {
            throw new NetworkException(msg.getMessage("acapy.unavailable"), e);
        }
    }

    /**
     * Send partner a credential offer in reference to a proposal (Not to be
     * confused with the automated send-offer flow).
     * 
     * @param id           credential exchange id
     * @param counterOffer {@link CredentialOfferRequest}
     * @return {@link CredEx} updated credential exchange, if found
     */
    public CredEx sendCredentialOffer(@NonNull UUID id, @NonNull CredentialOfferRequest counterOffer) {
        BPACredentialExchange credEx = credExRepo.findById(id).orElseThrow(EntityNotFoundException::new);
        if (!CredentialExchangeState.PROPOSAL_RECEIVED.equals(credEx.getState())) {
            throw new WrongApiUsageException(msg.getMessage("api.issuer.credential.send.offer.wrong.state",
                    Map.of("state", credEx.getState())));
        }
        List<CredentialAttributes> attributes;
        if (counterOffer.acceptAll()) {
            attributes = credEx.getCredentialProposal() != null
                    ? credEx.getCredentialProposal().getAttributes()
                    : List.of();
        } else {
            attributes = counterOffer.getAttributes();
        }
        V10CredentialBoundOfferRequest v1Offer = V10CredentialBoundOfferRequest
                .builder()
                .counterProposal(CredentialProposal
                        .builder()
                        .schemaId(credEx.getSchema() != null ? credEx.getSchema().getSchemaId() : null)
                        .credDefId(credEx.getCredDef() != null
                                ? credEx.getCredDef().getCredentialDefinitionId()
                                : null)
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
            Credential credential = Credential.builder()
                    .attrs(attributes.stream()
                            .collect(Collectors.toMap(CredentialAttributes::getName, CredentialAttributes::getValue)))
                    .build();
            credEx.setCredential(credential);
            credExRepo.updateCredential(credEx.getId(), credential);
        } catch (IOException e) {
            throw new NetworkException(msg.getMessage("acapy.unavailable"), e);
        }
        return CredEx.from(credEx);
    }

    // Credential Management - Called By Event Handler

    public void handleCredentialProposal(@NonNull V1CredentialExchange ex, ExchangeVersion exchangeVersion) {
        partnerRepo.findByConnectionId(ex.getConnectionId()).ifPresent(partner -> {
            BPACredentialExchange.BPACredentialExchangeBuilder b = BPACredentialExchange
                    .builder()
                    .partner(partner)
                    .role(CredentialExchangeRole.ISSUER)
                    .state(ex.getState())
                    .exchangeVersion(exchangeVersion)
                    .pushStateChange(ex.getState(), Instant.now())
                    .credentialExchangeId(ex.getCredentialExchangeId())
                    .threadId(ex.getThreadId())
                    .credentialProposal(ex.getCredentialProposalDict() != null
                            ? ex.getCredentialProposalDict().getCredentialProposal()
                            : null);
            credDefRepo.findBySchemaId(ex.getCredentialProposalDict().getSchemaId()).ifPresentOrElse(dbCredDef -> {
                b.schema(dbCredDef.getSchema()).credDef(dbCredDef);
                credExRepo.save(b.build());
            }, () -> {
                b.errorMsg("Issuer has no operable credential  definition for proposal spec: "
                        + ex.getCredentialProposalDict().getSchemaId());
                credExRepo.save(b.build());
            });
        });
    }

    /**
     * Handle issue credential v1 state changes and revocation info
     *
     * @param ex {@link V1CredentialExchange}
     */
    public void handleV1CredentialExchange(@NonNull V1CredentialExchange ex) {
        credExRepo.findByCredentialExchangeId(ex.getCredentialExchangeId()).ifPresent(bpaEx -> {
            if (bpaEx.getState() != null) {
                bpaEx.pushStateChange(ex.getState(), Instant.now());
                credExRepo.updateAfterEventWithRevocationInfo(bpaEx.getId(),
                        ex.getState(), bpaEx.getStateToTimestamp(),
                        ex.getRevocRegId(), ex.getRevocationId(), ex.getErrorMsg());
            }
            if (ex.isCredentialAcked() && ex.isAutoIssueEnabled()) {
                ex.findAttributesInCredentialOfferDict().ifPresent(
                        attr -> credExRepo.updateCredential(bpaEx.getId(), Credential.builder().attrs(attr).build()));
            }
        });
    }

    /**
     * Handle issue credential v2 state changes
     *
     * @param ex {@link V20CredExRecord}
     */
    public void handleV2CredentialExchange(@NonNull V20CredExRecord ex) {
        credExRepo.findByCredentialExchangeId(ex.getCredExId())
                .ifPresent(bpaEx -> {
                    if (ex.getState() != null) {
                        bpaEx.pushStateChange(ex.getState(), Instant.now());
                    }
                    credExRepo.updateAfterEventNoRevocationInfo(bpaEx.getId(),
                            ex.getState(), bpaEx.getStateToTimestamp(), ex.getErrorMsg());
                    if (ex.isDone() && ex.isAutoIssueEnabled()) {
                        ex.getByFormat().findValuesInIndyCredIssue().ifPresent(
                                attr -> credExRepo.updateCredential(bpaEx.getId(),
                                        Credential.builder().attrs(attr).build()));
                    }
                });
    }

    /**
     * Handle issue credential v2 revocation info
     *
     * @param revocationInfo {@link V2IssueIndyCredentialEvent}
     */
    public void handleIssueCredentialV2Indy(V2IssueIndyCredentialEvent revocationInfo) {
        credExRepo.findByCredentialExchangeId(revocationInfo.getCredExId()).ifPresent(bpaEx -> credExRepo
                .updateRevocationInfo(bpaEx.getId(), revocationInfo.getRevRegId(), revocationInfo.getCredRevId()));
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
                    .credentialExchangeId(ex.getCredExId())
                    .threadId(ex.getThreadId())
                    .build();
        }
    }
}
