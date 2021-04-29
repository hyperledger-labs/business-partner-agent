/*
 *
 * Copyright (c) 2021 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/business-partner-agent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.hyperledger.bpa.impl;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.credential_definition.CredentialDefinition.CredentialDefinitionRequest;
import org.hyperledger.aries.api.credential_definition.CredentialDefinition.CredentialDefinitionResponse;
import org.hyperledger.aries.api.schema.SchemaSendResponse;
import org.hyperledger.bpa.api.aries.SchemaAPI;
import org.hyperledger.bpa.api.exception.IssuerException;
import org.hyperledger.bpa.api.exception.NetworkException;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.config.RuntimeConfig;
import org.hyperledger.bpa.controller.api.issuer.CredDef;
import org.hyperledger.bpa.impl.activity.Identity;
import org.hyperledger.bpa.impl.aries.config.SchemaService;
import org.hyperledger.bpa.model.BPACredentialDefinition;
import org.hyperledger.bpa.model.BPASchema;
import org.hyperledger.bpa.repository.BPACredentialDefinitionRepository;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    RuntimeConfig config;

    public SchemaAPI createSchema(@NonNull String schemaName, @NonNull String schemaVersion,
            @NonNull List<String> attributes, @NonNull String schemaLabel, String defaultAttributeName) {
        return schemaService.createSchema(schemaName, schemaVersion, attributes, schemaLabel, defaultAttributeName);
    }

    String getDid() {
        return id.getMyDid() == null ? "" : id.getMyDid();
    }

    public List<SchemaAPI> listSchemas() {
        return schemaService.listSchemas(getDid());
    }

    public Optional<SchemaAPI> readSchema(@NonNull UUID id) {
        return schemaService.getSchema(id);
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
            Optional<CredentialDefinitionResponse> response = ac.credentialDefinitionsCreate(request);
            if (response.isPresent()) {
                // save it to the db...
                CredentialDefinitionResponse cdr = response.get();
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
}
