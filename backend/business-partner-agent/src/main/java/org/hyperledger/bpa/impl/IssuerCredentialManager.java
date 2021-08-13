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
import com.google.gson.Gson;
import io.micronaut.core.annotation.Nullable;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.acy_py.generated.model.V20CredFilter;
import org.hyperledger.acy_py.generated.model.V20CredFilterIndy;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.credential_definition.CredentialDefinition;
import org.hyperledger.aries.api.credential_definition.CredentialDefinition.CredentialDefinitionRequest;
import org.hyperledger.aries.api.credentials.Credential;
import org.hyperledger.aries.api.credentials.CredentialAttributes;
import org.hyperledger.aries.api.credentials.CredentialPreview;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialProposalRequest;
import org.hyperledger.aries.api.issue_credential_v2.V2CredentialSendRequest;
import org.hyperledger.aries.api.revocation.RevokeRequest;
import org.hyperledger.aries.api.schema.SchemaSendResponse;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.api.ExchangeVersion;
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
import org.hyperledger.bpa.impl.util.TimeUtil;
import org.hyperledger.bpa.model.BPACredentialDefinition;
import org.hyperledger.bpa.model.BPACredentialExchange;
import org.hyperledger.bpa.model.BPASchema;
import org.hyperledger.bpa.model.Partner;
import org.hyperledger.bpa.repository.BPACredentialDefinitionRepository;
import org.hyperledger.bpa.repository.BPACredentialExchangeRepository;
import org.hyperledger.bpa.repository.PartnerRepository;

import javax.inject.Inject;
import javax.inject.Singleton;
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

    public SchemaAPI createSchema(@NonNull String schemaName, @NonNull String schemaVersion,
            @NonNull List<String> attributes, @NonNull String schemaLabel, String defaultAttributeName) {
        return schemaService.createSchema(schemaName, schemaVersion, attributes, schemaLabel, defaultAttributeName);
    }

    public List<SchemaAPI> listSchemas() {
        return schemaService.listSchemas();
    }

    public Optional<SchemaAPI> readSchema(@NonNull UUID id) {
        return schemaService.getSchema(id);
    }

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

    public String issueCredentialSend(@NonNull IssueCredentialRequest request) {
        Map<String, Object> document = conv.toMap(request.getDocument());
        final Optional<Partner> dbPartner = partnerRepo.findById(request.getPartnerId());
        if (dbPartner.isEmpty()) {
            throw new IssuerException(String.format("Could not find partner with id '%s'", request.getPartnerId()));
        }
        final Optional<BPACredentialDefinition> dbCredDef = credDefRepo.findById(request.getCredDefId());
        if (dbCredDef.isEmpty()) {
            throw new IssuerException(String.format("Could not find credential definition with id '%s'", request.getCredDefId()));
        }
        // ok, we have partner/connection, we have a cred def/schema
        // let's make sure the document matches up with the schema.
        // attribute names must match exactly
        Set<String> documentAttributeNames = document.keySet();
        Set<String> schemaAttributeNames = dbCredDef.get().getSchema().getSchemaAttributeNames();
        if (!documentAttributeNames.equals(schemaAttributeNames)) {
            throw new IssuerException(String.format("Document attributes %s do not match schema attributes %s",
                    documentAttributeNames, schemaAttributeNames));
        }

        String connectionId = dbPartner.get().getConnectionId();
        String schemaId = dbCredDef.get().getSchema().getSchemaId();
        String credentialDefinitionId = dbCredDef.get().getCredentialDefinitionId();
        if (request.isV1()) {
            return sendV1Credential(connectionId,
                    schemaId,
                    credentialDefinitionId,
                    new CredentialPreview(CredentialAttributes.from(document)));
        } else {
            return sendV2Credential(connectionId,
                    schemaId,
                    credentialDefinitionId, V2CredentialSendRequest.V2CredentialPreview
                            .builder()
                            .attributes(CredentialAttributes.from(document))
                            .build());
        }
    }

    private String sendV1Credential(@NonNull String connectionId, @NonNull String schemaId,
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
                    .orElseThrow().getCredentialExchangeId();
        } catch (IOException e) {
            throw new NetworkException(msg.getMessage("acapy.unavailable"), e);
        }
    }

    private String sendV2Credential(@NonNull String connectionId, @NonNull String schemaId,
            @NonNull String credDefId,@NonNull V2CredentialSendRequest.V2CredentialPreview attributes)  {
        try {
            return ac.issueCredentialV2Send(V2CredentialSendRequest
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
                    .build()).orElseThrow().getCredExId();
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
                .map(o -> {
                    // for issuers, let's return a nearly complete credential (attrs, schema id,
                    // cred def id)
                    // instead of what is stored (only the attrs)
                    if (CredentialExchangeRole.ISSUER.equals(o.getRole())) {
                        buildFromProposal(o).ifPresent(c -> o.setCredential(conv.toMap(c)));
                    }
                    return CredEx.from(o);
                })
                .collect(Collectors.toList());
    }

    private Optional<Credential> buildFromProposal(@NonNull BPACredentialExchange exchange) {
        Optional<Credential> result = Optional.empty();
        // TODO this is creative but not how it is supposed to work ;)
        LinkedHashMap credentialProposal = (LinkedHashMap) exchange.getCredentialProposal().get("credential_proposal");
        if (credentialProposal != null) {
            ArrayList<LinkedHashMap> attributes = (ArrayList) credentialProposal.get("attributes");
            if (attributes != null) {
                final Map<String, String> attrs = attributes
                        .stream()
                        .collect(Collectors.toMap(s -> (String) s.get("name"),
                                s -> (String) s.get("value")));

                Credential credential = new Credential();
                credential.setSchemaId(exchange.getSchema().getSchemaId());
                credential.setCredentialDefinitionId(exchange.getCredDef().getCredentialDefinitionId());
                credential.setAttrs(attrs);
                result = Optional.of(credential);
            }
        }
        return result;
    }

    public void handleV1CredentialExchange(@NonNull V1CredentialExchange exchange) {
        switch (exchange.getState()) {
        case OFFER_SENT:
            // create a record...
            createCredentialExchange(exchange);
            break;
        case REQUEST_RECEIVED:
        case CREDENTIAL_ISSUED:
        case CREDENTIAL_ACKED:
            updateCredentialExchange(exchange);
            break;
        default:
            log.debug(String.format("Unhandled credential exchange: role = %s, state = %s", exchange.getRole(),
                    exchange.getState()));
            break;
        }
    }

    // TODO refactor v1
    // Merge v1 logic with v2 logic in client
    // Implement v2 handlers here, probably merge with v1 handler as the data is nearly the same
    // probably needs a intermediate model

    private void createCredentialExchange(@NonNull V1CredentialExchange exchange) {

        HashMap<String, Object> cp = new Gson().fromJson(exchange.getCredentialProposalDict(), HashMap.class);
        HashMap<String, Object> co = new Gson().fromJson(exchange.getCredentialOfferDict(), HashMap.class);
        // these should exist as we used them to issue the credential...
        Optional<BPACredentialDefinition> dbCredDef = credDefRepo
                .findByCredentialDefinitionId(exchange.getCredentialDefinitionId());
        Optional<Partner> dbPartner = partnerRepo.findByConnectionId(exchange.getConnectionId());
        if (dbCredDef.isPresent() && dbPartner.isPresent()) {
            BPACredentialExchange cex = BPACredentialExchange.builder()
                    .type(CredentialType.INDY)
                    .schema(dbCredDef.get().getSchema())
                    .partner(dbPartner.get())
                    .credDef(dbCredDef.get())
                    .role(exchange.getRole())
                    .state(exchange.getState())
                    .threadId(exchange.getThreadId())
                    .credentialExchangeId(exchange.getCredentialExchangeId())
                    .credentialProposal(cp)
                    .credentialOffer(co)
                    .updatedAt(TimeUtil.parseZonedTimestamp(exchange.getUpdatedAt()))
                    .build();

            credExRepo.save(cex);
        } else {
            log.error(String.format(
                    "Could not create credential exchange record. Cred. Def ID (%s) or Partner/Connection id (%s) not found",
                    exchange.getCredentialDefinitionId(), exchange.getConnectionId()));
        }
    }

    private void updateCredentialExchange(@NonNull V1CredentialExchange ex) {
        credExRepo.findByCredentialExchangeId(ex.getCredentialExchangeId()).ifPresent(bpaEx -> {
            if (ex.getCredential() != null) {
                Credential credential = ex.getCredential();
                bpaEx.setCredential(conv.toMap(credential));
                // label, try to get the value of the schema's default attribute
                if (credential.getAttrs() != null) {
                    bpaEx.setLabel(labelStrategy.apply(credential));
                } else {
                    // grab it from the proposal...
                    buildFromProposal(bpaEx).ifPresent(c -> bpaEx.setLabel(labelStrategy.apply(c)));
                }
            }
            bpaEx.setState(ex.getState());
            bpaEx.setUpdatedAt(TimeUtil.parseZonedTimestamp(ex.getUpdatedAt()));
            bpaEx.setRevRegId(ex.getRevocRegId());
            bpaEx.setCredRevId(ex.getRevocationId());
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
                result = Optional.of(CredEx.from(credEx.get()));
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
}
