/*
  Copyright (c) 2020 - for information on the respective copyright owner
  see the NOTICE file and/or the repository at
  https://github.com/hyperledger-labs/business-partner-agent

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package org.hyperledger.bpa.impl.aries.config;

import io.micronaut.cache.annotation.Cacheable;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.bpa.api.aries.SchemaAPI;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.config.SchemaConfig;
import org.hyperledger.bpa.impl.util.AriesStringUtil;
import org.hyperledger.bpa.model.BPASchema;
import org.hyperledger.bpa.repository.BPASchemaRepository;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.*;

@Slf4j
@Singleton
public class SchemaService {

    @Inject
    BPASchemaRepository schemaRepo;

    @Inject
    AriesClient ac;

    @Inject
    CredentialDefinitionManager credDefMgmt;

    @Inject
    List<SchemaConfig> schemas;

    // CRUD Methods

    public SchemaAPI addSchema(@NonNull String schemaId, @Nullable String label,
            @Nullable String defaultAttributeName) {
        return addSchema(schemaId, label, defaultAttributeName, false);
    }

    SchemaAPI addSchema(@NonNull String schemaId, @Nullable String label,
            @Nullable String defaultAttributeName, boolean isReadOnly) {
        SchemaAPI result = null;
        String sId = StringUtils.strip(schemaId);

        if (schemaRepo.findBySchemaId(sId).isPresent()) {
            throw new WrongApiUsageException("Schema with id: " + sId + " already exists.");
        }

        try {
            Optional<org.hyperledger.aries.api.schema.SchemaSendResponse.Schema> ariesSchema = ac.schemasGetById(sId);
            if (ariesSchema.isPresent()) {
                BPASchema dbS = BPASchema.builder()
                        .label(label)
                        .schemaId(sId)
                        .schemaAttributeNames(new LinkedHashSet<>(ariesSchema.get().getAttrNames()))
                        .defaultAttributeName(defaultAttributeName)
                        .seqNo(ariesSchema.get().getSeqNo())
                        .isReadOnly(isReadOnly)
                        .build();
                BPASchema saved = schemaRepo.save(dbS);
                result = SchemaAPI.from(saved);
            } else {
                log.error("Schema with id: {} does not exist on the ledger, skipping.", schemaId);
            }
        } catch (IOException e) {
            log.error("aca-py not reachable", e);
        }
        return result;
    }

    public Optional<SchemaAPI> updateSchema(@NonNull UUID id, @Nullable String defaultAttribute) {
        Optional<BPASchema> schema = schemaRepo.findById(id);
        if (schema.isPresent()) {
            schemaRepo.updateDefaultAttributeName(id, defaultAttribute);
            schema.get().setDefaultAttributeName(defaultAttribute);
            return Optional.of(SchemaAPI.from(schema.get()));
        }
        return Optional.empty();
    }

    public List<SchemaAPI> listSchemas() {
        List<SchemaAPI> result = new ArrayList<>();
        schemaRepo.findAll().forEach(dbS -> result.add(SchemaAPI.from(dbS)));
        return result;
    }

    public Optional<SchemaAPI> getSchema(@NonNull UUID id) {
        Optional<BPASchema> schema = schemaRepo.findById(id);
        return schema.map(SchemaAPI::from);
    }

    public void deleteSchema(@NonNull UUID id) {
        schemaRepo.findById(id).ifPresent(s -> {
            schemaRepo.deleteById(id);
            credDefMgmt.deleteBySchema(s);
        });
    }

    public Optional<BPASchema> getSchemaFor(@Nullable String schemaId) {
        if (StringUtils.isNotEmpty(schemaId)) {
            return schemaRepo.findBySchemaId(schemaId);
        }
        return Optional.empty();
    }

    @Cacheable("schema-attr-cache")
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

    @Cacheable("schema-label-cache")
    public @Nullable String getSchemaLabel(@NonNull String schemaId) {
        String result = null;
        Optional<BPASchema> schema = schemaRepo.findBySchemaId(schemaId);
        if (schema.isPresent()) {
            result = schema.get().getLabel();
        }
        if (StringUtils.isEmpty(result)) {
            result = AriesStringUtil.schemaGetName(schemaId);
        }
        return result;
    }

    public void resetWriteOnlySchemas() {
        schemaRepo.deleteByIsReadOnly(Boolean.TRUE);
        credDefMgmt.resetReadOnly();

        for (SchemaConfig schema : schemas) {
            try {
                SchemaAPI schemaAPI = addSchema(schema.getId(), schema.getLabel(),
                        schema.getDefaultAttributeName(), true);
                credDefMgmt.addCredentialDefinition(
                        schemaAPI.getId(), schemaAPI.getSeqNo(), Boolean.TRUE, schema.getCredentialDefinitionId());
            } catch (Exception e) {
                if (e instanceof WrongApiUsageException) {
                    log.warn("Schema already exists: {}", schema.getId());
                } else {
                    log.warn("Could not add schema id: {}", schema.getId(), e);
                }

            }
        }
    }
}
