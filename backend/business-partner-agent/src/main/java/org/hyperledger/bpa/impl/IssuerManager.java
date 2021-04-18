package org.hyperledger.bpa.impl;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.credential_definition.CredentialDefinition.*;
import org.hyperledger.aries.api.schema.SchemaSendRequest;
import org.hyperledger.aries.api.schema.SchemaSendResponse;
import org.hyperledger.bpa.api.aries.SchemaAPI;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.impl.activity.Identity;
import org.hyperledger.bpa.impl.aries.config.SchemaService;
import org.hyperledger.bpa.impl.util.AriesStringUtil;
import org.hyperledger.bpa.model.BPACredentialDefinition;
import org.hyperledger.bpa.model.BPASchema;
import org.hyperledger.bpa.repository.BPACredentialDefinitionRepository;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
            }

        } catch (IOException e) {
            log.error("aca-py not reachable", e);
        }
        return result;
    }

    public List<SchemaAPI> listSchemas() {
        Predicate<SchemaAPI> byDid = s -> s.getSchemaId().startsWith(AriesStringUtil.getLastSegment(id.getMyDid()));
        List<SchemaAPI> schemas = schemaService.listSchemas();
        // only want mine...
        List<SchemaAPI> filtered = schemas.stream().filter(byDid).collect(Collectors.toList());
        return filtered;
    }

    public Optional<SchemaAPI> readSchema(@NonNull UUID id) {
        return schemaService.getSchema(id);
    }

    public Object createCredDef(@NonNull String schemaId, @NonNull String tag, boolean supportRevocation,
            int revocationRegistrySize) {
        Object result = null;
        try {
            String sId = StringUtils.strip(schemaId);
            Optional<org.hyperledger.aries.api.schema.SchemaSendResponse.Schema> ariesSchema = ac.schemasGetById(sId);
            if (!ariesSchema.isPresent()) {
                throw new WrongApiUsageException("No schema with id " + sId + " found on ledger.");
            }

            Optional<BPASchema> bpaSchema = schemaService.getSchemaFor(sId);
            if (!bpaSchema.isPresent()) {
                // schema exists on ledger, but no in db, let's add it.
                SchemaAPI schema = schemaService.addSchema(ariesSchema.get().getId(), null, null, null);
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
                result = credDefRepo.save(cdef);
            } else {
                log.error("Credential Definition not created.");
            }
        } catch (IOException e) {
            log.error("aca-py not reachable", e);
        }
        return result;
    }
}
