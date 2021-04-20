package org.hyperledger.bpa.impl;

import com.google.gson.Gson;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.credential_definition.CredentialDefinition.CredentialDefinitionRequest;
import org.hyperledger.aries.api.credential_definition.CredentialDefinition.CredentialDefinitionResponse;
import org.hyperledger.aries.api.credentials.Credential;
import org.hyperledger.aries.api.credentials.CredentialAttributes;
import org.hyperledger.aries.api.credentials.CredentialPreview;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialProposalRequest;
import org.hyperledger.aries.api.schema.SchemaSendRequest;
import org.hyperledger.aries.api.schema.SchemaSendResponse;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.api.aries.SchemaAPI;
import org.hyperledger.bpa.api.exception.IssuerException;
import org.hyperledger.bpa.api.exception.NetworkException;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.controller.api.issuer.CredDef;
import org.hyperledger.bpa.impl.activity.Identity;
import org.hyperledger.bpa.impl.activity.LabelStrategy;
import org.hyperledger.bpa.impl.aries.config.SchemaService;
import org.hyperledger.bpa.impl.util.AriesStringUtil;
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
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class IssuerManager {

    @Inject
    Identity id;

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

    public SchemaAPI createSchema(@NonNull String schemaName, @NonNull String schemaVersion,
            @NonNull List<String> attributes, @NonNull String schemaLabel, String defaultAttributeName) {
        SchemaAPI result = null;
        // ensure no leading or trailing spaces on attribute names... bad things happen
        // when crypto signing.
        attributes.replaceAll(s -> AriesStringUtil.schemaAttributeFormat(s));
        try {
            // send schema to ledger...
            SchemaSendRequest request = SchemaSendRequest.builder()
                    .schemaName(AriesStringUtil.schemaAttributeFormat(schemaName))
                    .schemaVersion(schemaVersion)
                    .attributes(attributes)
                    .build();
            Optional<SchemaSendResponse> response = ac.schemas(request);
            if (response.isPresent()) {
                // save it to the db...
                SchemaSendResponse ssr = response.get();
                result = schemaService.addSchema(ssr.getSchemaId(), schemaLabel, defaultAttributeName, null);
            } else {
                log.error("Schema not created.");
                throw new IssuerException("Schema not created; could not complete request with ledger");
            }

        } catch (IOException e) {
            log.error("aca-py not reachable", e);
            throw new NetworkException("No aries connection", e);
        }
        return result;
    }

    @NonNull
    String getDid() {
        return id.getMyDid() == null ? "" : id.getMyDid();
    }

    public List<SchemaAPI> listSchemas() {
        String did = AriesStringUtil.getLastSegment(getDid());
        Predicate<SchemaAPI> byDid = s -> s.getSchemaId().startsWith(did);
        List<SchemaAPI> schemas = schemaService.listSchemas();
        // only want mine...
        return schemas.stream().filter(byDid).collect(Collectors.toList());
    }

    public Optional<SchemaAPI> readSchema(@NonNull UUID id) {
        return schemaService.getSchema(id);
    }

    public CredDef createCredDef(@NonNull String schemaId, @NonNull String tag, boolean supportRevocation,
            int revocationRegistrySize) {
        CredDef result = null;
        try {
            String sId = StringUtils.strip(schemaId);
            Optional<SchemaSendResponse.Schema> ariesSchema = ac.schemasGetById(sId);
            if (!ariesSchema.isPresent()) {
                throw new WrongApiUsageException(String.format("No schema with id '%s' found on ledger.", sId));
            }

            Optional<BPASchema> bpaSchema = schemaService.getSchemaFor(sId);
            if (!bpaSchema.isPresent()) {
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
                    .revocationRegistrySize(revocationRegistrySize)
                    .build();
            Optional<CredentialDefinitionResponse> response = ac.credentialDefinitionsCreate(request);
            if (response.isPresent()) {
                // save it to the db...
                CredentialDefinitionResponse cdr = response.get();
                BPACredentialDefinition cdef = BPACredentialDefinition.builder()
                        .schema(bpaSchema.get())
                        .credentialDefinitionId(cdr.getCredentialDefinitionId())
                        .isSupportRevocation(supportRevocation)
                        .revocationRegistrySize(revocationRegistrySize)
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

    public Object issueCredentialSend(@NonNull UUID credDefId, @NonNull UUID partnerId,
            @NonNull Map<String, Object> document) {
        // find all the data
        // find cred def - will give us schema id and credential definition id
        // find partner - will give us connectionId
        // build aries request
        // send aries request
        final Optional<Partner> dbPartner = partnerRepo.findById(partnerId);
        if (!dbPartner.isPresent()) {
            throw new IssuerException(String.format("Could not find partner with id '%s'", partnerId));
        }
        final Optional<BPACredentialDefinition> dbCredDef = credDefRepo.findById(credDefId);
        if (!dbCredDef.isPresent()) {
            throw new IssuerException(String.format("Could not find credential definition with id '%s'", credDefId));
        }
        try {
            Optional<V1CredentialExchange> exchange = ac.issueCredentialSend(
                    V1CredentialProposalRequest
                            .builder()
                            .connectionId(dbPartner.get().getConnectionId())
                            .schemaId(dbCredDef.get().getSchema().getSchemaId())
                            .credentialProposal(
                                    new CredentialPreview(
                                            CredentialAttributes.from(document)))
                            .credentialDefinitionId(dbCredDef.get().getCredentialDefinitionId())
                            .build());
            if (exchange.isPresent()) {
                // should this go in a handler for offer_sent?
                HashMap<String, Object> cp = new Gson().fromJson(exchange.get().getCredentialProposalDict(),
                        HashMap.class);
                HashMap<String, Object> co = new Gson().fromJson(exchange.get().getCredentialOfferDict(),
                        HashMap.class);
                // NOTE: using toLowerCase, ColumnTransformer not working :(
                BPACredentialExchange cex = BPACredentialExchange.builder()
                        .type(CredentialType.SCHEMA_BASED)
                        .schema(dbCredDef.get().getSchema())
                        .partner(dbPartner.get())
                        .credDef(dbCredDef.get())
                        .role(exchange.get().getRole().name().toLowerCase(Locale.getDefault()))
                        .state(exchange.get().getState().name().toLowerCase(Locale.getDefault()))
                        .threadId(exchange.get().getThreadId())
                        .credentialExchangeId(exchange.get().getCredentialExchangeId())
                        .credentialProposal(cp)
                        .credentialOffer(co)
                        .updatedAt(TimeUtil.parseZonedTimestamp(exchange.get().getUpdatedAt()))
                        .build();

                BPACredentialExchange saved = credExRepo.save(cex);
                return saved;
            } else {
                throw new IssuerException(String.format("Could not issue the credential for definition '%s'",
                        dbCredDef.get().getCredentialDefinitionId()));
            }
        } catch (IOException e) {
            throw new NetworkException("No aries connection", e);
        }
    }

    private void updateCredentialExchange(@NonNull String credentialExchangeId,
            @NonNull String state,
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
                    LinkedHashMap credentialProposal = (LinkedHashMap) cex.get().getCredentialProposal()
                            .get("credential_proposal");
                    if (credentialProposal != null) {
                        ArrayList<LinkedHashMap> attributes = (ArrayList) credentialProposal.get("attributes");
                        if (attributes != null) {
                            final Map<String, String> attrs = attributes
                                    .stream()
                                    .collect(Collectors.toMap(s -> (String) s.get("name"),
                                            s -> (String) s.get("value")));

                            Credential c = new Credential();
                            c.setSchemaId(credential.getSchemaId());
                            c.setCredentialDefinitionId(credential.getCredentialDefinitionId());
                            c.setAttrs(attrs);
                            cex.get().setLabel(labelStrategy.apply(c));
                        }
                    }
                }
            }
            cex.get().setState(state.toLowerCase(Locale.getDefault()));
            cex.get().setUpdatedAt(TimeUtil.parseZonedTimestamp(updatedAt));
            credExRepo.update(cex.get());
        }
    }

    public void handleCredentialExchange(@NonNull V1CredentialExchange exchange) {
        // when we have different logic, we can separate into a switch statement
        // for now, we are just updating the db record.
        // credential is handled if it exists.
        updateCredentialExchange(exchange.getCredentialExchangeId(), exchange.getState().name(),
                exchange.getUpdatedAt(), exchange.getCredential());
    }
}
