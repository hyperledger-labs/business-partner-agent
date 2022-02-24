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
package org.hyperledger.bpa.impl.aries.creddef;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.credential_definition.CredentialDefinition;
import org.hyperledger.aries.api.schema.SchemaSendResponse;
import org.hyperledger.bpa.api.aries.SchemaAPI;
import org.hyperledger.bpa.api.exception.IssuerException;
import org.hyperledger.bpa.api.exception.NetworkException;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.config.RuntimeConfig;
import org.hyperledger.bpa.controller.api.issuer.CredDef;
import org.hyperledger.bpa.impl.aries.schema.SchemaService;
import org.hyperledger.bpa.persistence.model.BPACredentialDefinition;
import org.hyperledger.bpa.persistence.model.BPASchema;
import org.hyperledger.bpa.persistence.repository.BPACredentialDefinitionRepository;
import org.hyperledger.bpa.persistence.repository.IssuerCredExRepository;

import java.io.IOException;
import java.util.*;

@Slf4j
@Singleton
public class CredDefManager {

    @Inject
    AriesClient ac;

    @Inject
    SchemaService schemaService;

    @Inject
    BPACredentialDefinitionRepository credDefRepo;

    @Inject
    BPAMessageSource.DefaultMessageSource msg;

    @Inject
    RuntimeConfig config;

    @Inject
    IssuerCredExRepository issuerCredExRepo;

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
            CredentialDefinition.CredentialDefinitionRequest request = CredentialDefinition.CredentialDefinitionRequest
                    .builder()
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
}
