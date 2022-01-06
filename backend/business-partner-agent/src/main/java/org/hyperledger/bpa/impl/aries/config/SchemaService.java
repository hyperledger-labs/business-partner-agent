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
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.exception.AriesException;
import org.hyperledger.aries.api.schema.SchemaSendRequest;
import org.hyperledger.aries.api.schema.SchemaSendResponse;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.api.aries.SchemaAPI;
import org.hyperledger.bpa.api.exception.EntityNotFoundException;
import org.hyperledger.bpa.api.exception.NetworkException;
import org.hyperledger.bpa.api.exception.SchemaException;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.config.SchemaConfig;
import org.hyperledger.bpa.controller.api.admin.AddTrustedIssuerRequest;
import org.hyperledger.bpa.impl.activity.Identity;
import org.hyperledger.bpa.impl.util.AriesStringUtil;
import org.hyperledger.bpa.model.BPASchema;
import org.hyperledger.bpa.repository.BPASchemaRepository;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
            Optional<SchemaSendResponse> response = ac.schemas(request);
            if (response.isPresent()) {
                // save it to the db...
                SchemaSendResponse ssr = response.get();
                result = this.addIndySchema(ssr.getSchemaId(), schemaLabel, defaultAttributeName, null);
            } else {
                log.error("Schema not created.");
                throw new SchemaException(ms.getMessage("api.schema.creation.failed"));
            }
        } catch (AriesException ae) {
            log.error("Aries Exception sending schema to ledger", ae);
            throw new SchemaException(ms.getMessage("api.schema.ledger.error", Map.of("message", ae.getMessage())));
        } catch (IOException e) {
            log.error("aca-py not reachable", e);
            throw new NetworkException(ms.getMessage("acapy.unavailable"), e);
        }
        return result;
    }

    @Nullable
    public SchemaAPI addIndySchema(@NonNull String schemaId, @Nullable String label,
            @Nullable String defaultAttributeName, @Nullable List<AddTrustedIssuerRequest> restrictions) {
        SchemaAPI schema = addIndySchema(schemaId, label, defaultAttributeName);
        if (schema == null) {
            throw new WrongApiUsageException(ms.getMessage("api.schema.creation.adding.failed"));
        }
        if (CollectionUtils.isNotEmpty(restrictions)) {
            restrictions
                    .forEach(r -> restrictionsManager.addRestriction(schema.getId(), r.getIssuerDid(), r.getLabel()));
        }

        return schema;
    }

    @Nullable
    public SchemaAPI addIndySchema(@NonNull String schemaId, @Nullable String label,
            @Nullable String defaultAttributeName) {
        SchemaAPI result;
        String sId = StringUtils.strip(schemaId);

        if (schemaRepo.findBySchemaId(sId).isPresent()) {
            throw new WrongApiUsageException(ms.getMessage("api.schema.already.exists", Map.of("id", sId)));
        }

        try {
            Optional<SchemaSendResponse.Schema> ariesSchema = ac.schemasGetById(sId);
            if (ariesSchema.isPresent()) {
                BPASchema dbS = BPASchema.builder()
                        .label(label != null ? label : AriesStringUtil.schemaGetName(schemaId))
                        .schemaId(ariesSchema.get().getId())
                        .schemaAttributeNames(new LinkedHashSet<>(ariesSchema.get().getAttrNames()))
                        .defaultAttributeName(defaultAttributeName)
                        .seqNo(ariesSchema.get().getSeqNo())
                        .type(CredentialType.INDY)
                        .build();
                BPASchema saved = schemaRepo.save(dbS);
                result = SchemaAPI.from(saved);
            } else {
                throw new EntityNotFoundException(ms.getMessage("api.schema.already.exists.ledger",
                        Map.of("id", sId)));
            }
        } catch (AriesException ae) {
            throw new SchemaException(ms.getMessage("api.schema.creation.general.error",
                    Map.of("message", ae.getMessage())));
        } catch (IOException e) {
            log.error("aca-py not reachable", e);
            throw new NetworkException(ms.getMessage("acapy.unavailable"), e);
        }
        return result;
    }

    @Nullable
    public SchemaAPI addJsonLDSchema(@NonNull String schemaId, @Nullable String label,
            @Nullable String defaultAttributeName, @NonNull String ldType, @NonNull Set<String> attributes) {

        try {
            new URI(schemaId);
        } catch (URISyntaxException e) {
            throw new WrongApiUsageException(ms.getMessage("api.schema.ld.id.parse.error"));
        }

        BPASchema dbS = BPASchema.builder()
                .label(label)
                .schemaId(schemaId)
                .schemaAttributeNames(attributes)
                .defaultAttributeName(defaultAttributeName)
                .type(CredentialType.JSON_LD)
                .ldType(ldType)
                .build();
        BPASchema saved = schemaRepo.save(dbS);
        return SchemaAPI.from(saved);
    }

    public SchemaAPI updateSchema(@NonNull UUID id, @Nullable String defaultAttribute) {
        BPASchema schema = schemaRepo.findById(id).orElseThrow(EntityNotFoundException::new);
        schemaRepo.updateDefaultAttributeName(id, defaultAttribute);
        schema.setDefaultAttributeName(defaultAttribute);
        return SchemaAPI.from(schema);
    }

    public List<SchemaAPI> listSchemas() {
        return StreamSupport.stream(schemaRepo.findAll().spliterator(), false)
                .map(dbS -> SchemaAPI.from(dbS, id))
                .collect(Collectors.toList());
    }

    public List<SchemaAPI> listLdSchemas() {
        return schemaRepo
                .findByType(CredentialType.JSON_LD)
                .stream()
                .map(s -> SchemaAPI.from(s, false, false))
                .collect(Collectors.toList());
    }

    public Optional<SchemaAPI> getSchema(@NonNull UUID id) {
        return schemaRepo.findById(id).map(SchemaAPI::from);
    }

    public void deleteSchema(@NonNull UUID id) {
        schemaRepo.findById(id).ifPresentOrElse(s -> {
            try {
                schemaRepo.deleteById(id);
            } catch (DataAccessException e) {
                log.error("Could not delete schema", e);
                throw new WrongApiUsageException(ms.getMessage("api.schema.constrain.violation"));
            }
        }, EntityNotFoundException::new);
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
            if (StringUtils.isEmpty(result) && schema.get().typeIsJsonLd()) {
                result = schema.get().getLdType();
            }
        }
        if (StringUtils.isEmpty(result) && AriesStringUtil.isIndySchemaId(schemaId)) {
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
                            SchemaAPI schemaAPI = addIndySchema(schema.getId(), schema.getLabel(),
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
