/**
 * Copyright (c) 2020 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/organizational-agent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hyperledger.oa.impl.aries;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.oa.api.ApiConstants;
import org.hyperledger.oa.api.CredentialType;
import org.hyperledger.oa.api.aries.SchemaAPI;
import org.hyperledger.oa.api.exception.WrongApiUsageException;
import org.hyperledger.oa.config.runtime.RequiresAries;
import org.hyperledger.oa.model.Schema;
import org.hyperledger.oa.repository.SchemaRepository;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@RequiresAries
public class SchemaService {

    @Inject
    SchemaRepository schemaRepo;

    @Inject
    AriesClient ac;

    // CRUD Methods

    public SchemaAPI addSchema(@NonNull String schemaId, @Nullable String label) {
        SchemaAPI result = null;
        String sId = StringUtils.strip(schemaId);
        final CredentialType credType = CredentialType.fromSchemaId(sId);
        if (schemaRepo.findByType(credType).isPresent()) {
            throw new WrongApiUsageException("Schema with type: " + credType + " already exists.");
        }

        try {
            Optional<org.hyperledger.aries.api.schema.SchemaSendResponse.Schema> ariesSchema = ac.schemasGetById(sId);
            if (ariesSchema.isPresent()) {
                Schema dbS = Schema
                        .builder()
                        .label(label)
                        .type(credType)
                        .schemaId(sId)
                        .seqNo(ariesSchema.get().getSeqNo())
                        .build();
                Schema saved = schemaRepo.save(dbS);
                result = SchemaAPI.from(saved);
            }
        } catch (IOException e) {
            log.error("aca-py not reachable", e);
        }
        return result;
    }

    public List<SchemaAPI> listSchemas() {
        List<SchemaAPI> result = new ArrayList<>();
        schemaRepo.findAll().forEach(dbS -> result.add(SchemaAPI.from(dbS)));
        return result;
    }

    public void deleteSchema(@NonNull UUID id) {
        schemaRepo.deleteById(id);
    }

    public @Nullable Schema getSchemaFor(CredentialType type) {
        Schema result = null;
        final Optional<Schema> dbSchema = schemaRepo.findByType(type);
        if (dbSchema.isPresent()) {
            result = dbSchema.get();
        } else if (CredentialType.BANK_ACCOUNT_CREDENTIAL.equals(type)) {
            // falling back to defaults
            result = Schema
                    .builder()
                    .schemaId(ApiConstants.BANK_ACCOUNT_SCHEMA_ID)
                    .seqNo(ApiConstants.BANK_ACCOUNT_SCHEMA_SEQ)
                    .build();
        }
        return result;
    }

    // TODO cache forever
    public Set<String> getSchemaAttributeNames(@NonNull String schemaId) {
        Set<String> result = new LinkedHashSet<>();
        try {
            final Optional<org.hyperledger.aries.api.schema.SchemaSendResponse.Schema> schema = ac
                    .schemasGetById(schemaId);
            if (schema.isPresent()) {
                result = new LinkedHashSet<>(schema.get().getAttrNames());
            }
        } catch (IOException e) {
            log.error("aca-py not reachable", e);
        }
        return result;
    }
}
