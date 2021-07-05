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

import com.google.gson.Gson;
import io.micronaut.core.annotation.Nullable;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.acy_py.generated.model.CredentialDefinitionSendResult;
import org.hyperledger.acy_py.generated.model.TxnOrCredentialDefinitionSendResult;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.credential_definition.CredentialDefinition.CredentialDefinitionRequest;
import org.hyperledger.aries.api.credentials.Credential;
import org.hyperledger.aries.api.credentials.CredentialAttributes;
import org.hyperledger.aries.api.credentials.CredentialPreview;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialProposalRequest;
import org.hyperledger.aries.api.schema.SchemaSendResponse;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.api.aries.SchemaAPI;
import org.hyperledger.bpa.api.exception.IssuerException;
import org.hyperledger.bpa.api.exception.NetworkException;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.config.RuntimeConfig;
import org.hyperledger.bpa.controller.api.issuer.CredDef;
import org.hyperledger.bpa.controller.api.issuer.CredEx;
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
public class IssuerManager {

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
            Optional<TxnOrCredentialDefinitionSendResult> response = ac.credentialDefinitionsCreate(request);
            if (response.isPresent()) {
                // save it to the db...
                CredentialDefinitionSendResult cdr = response.get().getSent();
                BPACredentialDefinition cdef = BPACredentialDefinition.builder()
                        .schema(bpaSchema.get())
                        .credentialDefinitionId(cdr.getCredentialDefinitionId())
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
            throw new NetworkException("No aries connection", e);
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

    public Optional<V1CredentialExchange> issueCredentialSend(@NonNull UUID credDefId, @NonNull UUID partnerId,
            @NonNull Map<String, Object> document) {
        final Optional<Partner> dbPartner = partnerRepo.findById(partnerId);
        if (dbPartner.isEmpty()) {
            throw new IssuerException(String.format("Could not find partner with id '%s'", partnerId));
        }
        final Optional<BPACredentialDefinition> dbCredDef = credDefRepo.findById(credDefId);
        if (dbCredDef.isEmpty()) {
            throw new IssuerException(String.format("Could not find credential definition with id '%s'", credDefId));
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

        try {
            return ac.issueCredentialSend(
                    V1CredentialProposalRequest
                            .builder()
                            .connectionId(dbPartner.get().getConnectionId())
                            .schemaId(dbCredDef.get().getSchema().getSchemaId())
                            .credentialProposal(
                                    new CredentialPreview(
                                            CredentialAttributes.from(document)))
                            .credentialDefinitionId(dbCredDef.get().getCredentialDefinitionId())
                            .build());
        } catch (IOException e) {
            throw new NetworkException("No aries connection", e);
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
                        Credential c = this.buildFromProposal(o);
                        o.setCredential(conv.toMap(c));
                    }
                    return CredEx.from(o);
                })
                .collect(Collectors.toList());
    }

    private Credential buildFromProposal(@NonNull BPACredentialExchange exchange) {
        Credential result = null;
        // TODO this is creative but not how it is supposed to work ;)
        LinkedHashMap credentialProposal = (LinkedHashMap) exchange.getCredentialProposal().get("credential_proposal");
        if (credentialProposal != null) {
            ArrayList<LinkedHashMap> attributes = (ArrayList) credentialProposal.get("attributes");
            if (attributes != null) {
                final Map<String, String> attrs = attributes
                        .stream()
                        .collect(Collectors.toMap(s -> (String) s.get("name"),
                                s -> (String) s.get("value")));

                result = new Credential();
                result.setSchemaId(exchange.getSchema().getSchemaId());
                result.setCredentialDefinitionId(exchange.getCredDef().getCredentialDefinitionId());
                result.setAttrs(attrs);
            }
        }
        return result;
    }

    private BPACredentialExchange createCredentialExchange(@NonNull V1CredentialExchange exchange) {

        HashMap<String, Object> cp = new Gson().fromJson(exchange.getCredentialProposalDict(),
                HashMap.class);
        HashMap<String, Object> co = new Gson().fromJson(exchange.getCredentialOfferDict(),
                HashMap.class);
        // these should exist as we used them to issue the credential...
        Optional<BPACredentialDefinition> dbCredDef = credDefRepo
                .findByCredentialDefinitionId(exchange.getCredentialDefinitionId());
        Optional<Partner> dbPartner = partnerRepo.findByConnectionId(exchange.getConnectionId());
        if (dbCredDef.isPresent() && dbPartner.isPresent()) {
            BPACredentialExchange cex = BPACredentialExchange.builder()
                    .type(CredentialType.SCHEMA_BASED)
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

            return credExRepo.save(cex);
        } else {
            throw new IssuerException(String.format(
                    "Could not create credential exchange record. Cred. Def ID (%s) or Partner/Connection id (%s) not found",
                    exchange.getCredentialDefinitionId(), exchange.getConnectionId()));
        }
    }

    private void updateCredentialExchange(@NonNull String credentialExchangeId,
            @NonNull CredentialExchangeState state,
            @NonNull String updatedAt,
            Credential credential) {
        Optional<BPACredentialExchange> cex = credExRepo.findByCredentialExchangeId(credentialExchangeId);
        if (cex.isPresent()) {
            if (credential != null) {
                cex.get().setCredential(conv.toMap(credential));
                // label, try to get the value of the schema's default attribute
                if (credential.getAttrs() != null) {
                    cex.get().setLabel(labelStrategy.apply(credential));
                } else {
                    // grab it from the proposal...
                    Credential c = this.buildFromProposal(cex.get());
                    if (c != null) {
                        cex.get().setLabel(labelStrategy.apply(c));
                    }
                }
            }
            cex.get().setState(state);
            cex.get().setUpdatedAt(TimeUtil.parseZonedTimestamp(updatedAt));
            credExRepo.update(cex.get());
        }
    }

    public void handleCredentialExchange(@NonNull V1CredentialExchange exchange) {
        switch (exchange.getState()) {
        case OFFER_SENT:
            // create a record...
            createCredentialExchange(exchange);
            break;
        case REQUEST_RECEIVED:
        case CREDENTIAL_ISSUED:
        case CREDENTIAL_ACKED:
            updateCredentialExchange(exchange.getCredentialExchangeId(), exchange.getState(),
                    exchange.getUpdatedAt(), exchange.getCredential());
            break;
        default:
            log.debug(String.format("Unhandled credential exchange: role = %s, state = %s", exchange.getRole(),
                    exchange.getState()));
            break;
        }
    }
}
