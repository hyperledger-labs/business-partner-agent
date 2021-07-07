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
package org.hyperledger.bpa.impl.aries.config;

import io.micronaut.cache.annotation.Cacheable;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.data.exceptions.DataAccessException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.acy_py.generated.model.SchemaSendResult;
import org.hyperledger.acy_py.generated.model.TxnOrSchemaSendResult;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.schema.SchemaSendRequest;
import org.hyperledger.aries.api.schema.SchemaSendResponse;
import org.hyperledger.bpa.api.aries.SchemaAPI;
import org.hyperledger.bpa.api.exception.NetworkException;
import org.hyperledger.bpa.api.exception.SchemaException;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.config.RuntimeConfig;
import org.hyperledger.bpa.config.SchemaConfig;
import org.hyperledger.bpa.controller.api.admin.AddTrustedIssuerRequest;
import org.hyperledger.bpa.impl.activity.Identity;
import org.hyperledger.bpa.impl.util.AriesStringUtil;
import org.hyperledger.bpa.model.BPASchema;
import org.hyperledger.bpa.repository.BPASchemaRepository;

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
    RestrictionsManager restrictionsManager;

    @Inject
    List<SchemaConfig> schemas;

    @Inject
    RuntimeConfig rc;

    @Inject
    Identity id;

    @Inject
    BPAMessageSource.DefaultMessageSource ms;

    public SchemaAPI createSchema(@NonNull String schemaName, @NonNull String schemaVersion,
            @NonNull List<String> attributes, @NonNull String schemaLabel, String defaultAttributeName) {
        SchemaAPI result;
        // ensure no leading or trailing spaces on attribute names... bad things happen
        // when crypto signing.
        attributes.replaceAll(AriesStringUtil::schemaAttributeFormat);
        try {
            // send schema to ledger...
            SchemaSendRequest request = SchemaSendRequest.builder()
                    .schemaName(AriesStringUtil.schemaAttributeFormat(schemaName))
                    .schemaVersion(schemaVersion)
                    .attributes(attributes)
                    .build();
            Optional<TxnOrSchemaSendResult> response = ac.schemas(request);
            if (response.isPresent()) {
                // save it to the db...
                SchemaSendResult ssr = response.get().getSent();
                result = this.addSchema(ssr.getSchemaId(), schemaLabel, defaultAttributeName, null);
            } else {
                log.error("Schema not created.");
                throw new SchemaException("Schema not created; could not complete request with ledger");
            }

        } catch (IOException e) {
            log.error("aca-py not reachable", e);
            throw new NetworkException("No aries connection", e);
        }
        return result;
    }

    public @Nullable SchemaAPI addSchema(@NonNull String schemaId, @Nullable String label,
            @Nullable String defaultAttributeName, @Nullable List<AddTrustedIssuerRequest> restrictions) {
        SchemaAPI schema = addSchema(schemaId, label, defaultAttributeName);
        if (schema == null) {
            throw new WrongApiUsageException("Schema config could not be added");
        }
        if (CollectionUtils.isNotEmpty(restrictions)) {
            restrictions
                    .forEach(r -> restrictionsManager.addRestriction(schema.getId(), r.getIssuerDid(), r.getLabel()));
        }

        return schema;
    }

    @Nullable
    SchemaAPI addSchema(@NonNull String schemaId, @Nullable String label,
            @Nullable String defaultAttributeName) {
        SchemaAPI result = null;
        String sId = StringUtils.strip(schemaId);

        if (schemaRepo.findBySchemaId(sId).isPresent()) {
            throw new WrongApiUsageException("Schema with id: " + sId + " already exists.");
        }

        try {
            Optional<SchemaSendResponse.Schema> ariesSchema = ac.schemasGetById(sId);
            if (ariesSchema.isPresent()) {
                BPASchema dbS = BPASchema.builder()
                        .label(label)
                        .schemaId(sId)
                        .schemaAttributeNames(new LinkedHashSet<>(ariesSchema.get().getAttrNames()))
                        .defaultAttributeName(defaultAttributeName)
                        .seqNo(ariesSchema.get().getSeqNo())
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
        schemaRepo.findAll().forEach(dbS -> result.add(SchemaAPI.from(dbS, id)));
        return result;
    }

    public Optional<SchemaAPI> getSchema(@NonNull UUID id) {
        Optional<BPASchema> schema = schemaRepo.findById(id);
        return schema.map(SchemaAPI::from);
    }

    public void deleteSchema(@NonNull UUID id) {
        schemaRepo.findById(id).ifPresent(s -> {
            try {
                schemaRepo.deleteById(id);
            } catch (DataAccessException e) {
                log.error("Could not delete schema", e);
                throw new WrongApiUsageException(ms.getMessage("api.schema.constrain.violation"));
            }
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
            final Optional<SchemaSendResponse.Schema> schema = ac
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
        for (SchemaConfig schema : schemas) {
            schemaRepo.findBySchemaId(schema.getId()).ifPresentOrElse(
                    dbSchema -> log.debug("Schema with id {} already exists", schema.getId()),
                    () -> {
                        try {
                            SchemaAPI schemaAPI = addSchema(schema.getId(), schema.getLabel(),
                                    schema.getDefaultAttributeName());
                            if (schemaAPI != null) {
                                restrictionsManager.addRestriction(
                                        schemaAPI.getId(), schema.getRestrictions());
                            }
                        } catch (Exception e) {
                            log.warn("Could not add schema id: {}", schema.getId(), e);
                        }
                    });
        }
    }
}
