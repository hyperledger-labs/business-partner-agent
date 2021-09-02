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
import org.hyperledger.acy_py.generated.model.V20CredExRecord;
import org.hyperledger.acy_py.generated.model.V20CredFilter;
import org.hyperledger.acy_py.generated.model.V20CredFilterIndy;
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
import org.hyperledger.aries.api.issue_credential_v2.V2CredentialSendRequest;
import org.hyperledger.aries.api.issue_credential_v2.V2IssueIndyCredentialEvent;
import org.hyperledger.aries.api.revocation.RevokeRequest;
import org.hyperledger.aries.api.schema.SchemaSendResponse;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.api.aries.ExchangeVersion;
import org.hyperledger.bpa.api.aries.SchemaAPI;
import org.hyperledger.bpa.api.exception.IssuerException;
import org.hyperledger.bpa.api.exception.NetworkException;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.config.RuntimeConfig;
import org.hyperledger.bpa.controller.api.issuer.CredDef;
import org.hyperledger.bpa.controller.api.issuer.CredEx;
import org.hyperledger.bpa.controller.api.issuer.IssueCredentialSendRequest;
import org.hyperledger.bpa.impl.activity.LabelStrategy;
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
    LabelStrategy labelStrategy;

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
            CredentialDefinitionRequest request = CredentialDefinitionRequest.builder()
                    .schemaId(schemaId)
                    .tag(tag)
                    .supportRevocation(supportRevocation)
                    .revocationRegistrySize(config.getRevocationRegistrySize())
                    .build();
            Optional<CredentialDefinition.CredentialDefinitionResponse> response = ac
                    .credentialDefinitionsCreate(request);
            if (response.isPresent()) {
                // save it to the db...
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

    // Credential Management

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
        if (request.isV1()) {
            exVersion = ExchangeVersion.V1;
            exResult = sendV1Credential(Objects.requireNonNull(connectionId),
                    schemaId,
                    credentialDefinitionId,
                    new CredentialPreview(CredentialAttributes.fromMap(document)));
        } else {
            exVersion = ExchangeVersion.V2;
            exResult = sendV2Credential(Objects.requireNonNull(connectionId),
                    schemaId,
                    credentialDefinitionId, V2CredentialSendRequest.V2CredentialPreview
                            .builder()
                            .attributes(CredentialAttributes.fromMap(document))
                            .build());
        }

        // as I'm the issuer I know what I have issued, no need to get this info from
        // the exchange again
        Credential credential = Credential.builder()
                .schemaId(schemaId)
                .credentialDefinitionId(credentialDefinitionId)
                .attrs(document)
                .build();

        BPACredentialExchange cex = BPACredentialExchange.builder()
                .type(CredentialType.INDY)
                .schema(dbCredDef.getSchema())
                .partner(dbPartner)
                .credDef(dbCredDef)
                .role(CredentialExchangeRole.ISSUER)
                .state(CredentialExchangeState.OFFER_SENT)
                .credential(conv.toMap(credential))
                .credentialExchangeId(exResult.getCredentialExchangeId())
                .threadId(exResult.getThreadId())
                .label(labelStrategy.apply(credential))
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

    private ExchangeResult sendV1Credential(@NonNull String connectionId, @NonNull String schemaId,
            @NonNull String credDefId, @NonNull CredentialPreview attributes) {
        try {
            return ac.issueCredentialSend(
                    V1CredentialProposalRequest
                            .builder()
                            .connectionId(connectionId)
                            .schemaId(schemaId)
                            .credentialProposal(attributes)
                            .credentialDefinitionId(credDefId)
                            .build())
                    .map(ExchangeResult::fromV1)
                    .orElseThrow();
        } catch (IOException e) {
            throw new NetworkException(msg.getMessage("acapy.unavailable"), e);
        }
    }

    private ExchangeResult sendV2Credential(@NonNull String connectionId, @NonNull String schemaId,
            @NonNull String credDefId, @NonNull V2CredentialSendRequest.V2CredentialPreview attributes) {
        try {
            V2CredentialSendRequest v2SendRequest = V2CredentialSendRequest
                    .builder()
                    .connectionId(connectionId)
                    .credentialPreview(attributes)
                    .filter(V20CredFilter
                            .builder()
                            .indy(V20CredFilterIndy
                                    .builder()
                                    .schemaId(schemaId)
                                    .credDefId(credDefId)
                                    .build())
                            .build())
                    .build();
            return ac.issueCredentialV2Send(v2SendRequest)
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
                .map(ex -> CredEx.from(ex, Optional.of(conv)))
                .collect(Collectors.toList());
    }

    /**
     * Handle issue credential v1 state changes and revocation info
     * 
     * @param ex {@link V1CredentialExchange}
     */
    public void handleV1CredentialExchange(@NonNull V1CredentialExchange ex) {
        credExRepo.findByCredentialExchangeId(ex.getCredentialExchangeId()).ifPresent(bpaEx -> {
            bpaEx.setState(ex.getState());
            bpaEx.setRevRegId(ex.getRevocRegId());
            bpaEx.setCredRevId(ex.getRevocationId());
            credExRepo.update(bpaEx);
        });
    }

    /**
     * Handle issue credential v2 state changes
     * 
     * @param ex {@link V20CredExRecord}
     */
    public void handleV2CredentialExchange(@NonNull V20CredExRecord ex) {
        credExRepo.findByCredentialExchangeId(ex.getCredExId())
                .ifPresent(
                        bpaEx -> credExRepo.updateState(bpaEx.getId(), CredentialExchangeState.fromV2(ex.getState())));
    }

    /**
     * Handle issue credential v2 revocation info
     * 
     * @param revocationInfo {@link V2IssueIndyCredentialEvent}
     */
    public void handleIssueCredentialV2Indy(V2IssueIndyCredentialEvent revocationInfo) {
        credExRepo.findByCredentialExchangeId(revocationInfo.getCredExId()).ifPresent(bpaEx -> {
            bpaEx.setRevRegId(revocationInfo.getRevRegId());
            bpaEx.setCredRevId(revocationInfo.getCredRevId());
            credExRepo.update(bpaEx);
        });
    }

    public Optional<CredEx> revokeCredentialExchange(@NonNull UUID id) {
        Optional<CredEx> result = Optional.empty();
        if (!config.getTailsServerConfigured()) {
            throw new IssuerException(msg.getMessage("api.issuer.no.tails.server"));
        }
        Optional<BPACredentialExchange> credEx = credExRepo.findById(id);
        if (credEx.isPresent()) {
            if (StringUtils.isEmpty(credEx.get().getRevRegId())) {
                throw new IssuerException(msg.getMessage("api.issuer.credential.missing.revocation.info"));
            }
            try {
                ac.revocationRevoke(RevokeRequest
                        .builder()
                        .credRevId(credEx.get().getCredRevId())
                        .revRegId(credEx.get().getRevRegId())
                        .publish(Boolean.TRUE)
                        .build());
                credExRepo.updateRevoked(credEx.get().getId(), Boolean.TRUE);
                credEx.get().setRevoked(Boolean.TRUE);
                result = Optional.of(CredEx.from(credEx.get(), Optional.of(conv)));
            } catch (IOException e) {
                throw new NetworkException(msg.getMessage("acapy.unavailable"), e);
            }
        }
        return result;
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
