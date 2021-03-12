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

import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.exception.AriesException;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.controller.api.admin.RestrictionResponse;
import org.hyperledger.bpa.model.BPARestrictions;
import org.hyperledger.bpa.model.BPASchema;
import org.hyperledger.bpa.repository.BPARestrictionsRepository;
import org.hyperledger.bpa.repository.BPASchemaRepository;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Parses runtime config and updates credential definition configuration
 */
@Slf4j
@Singleton
public class RestrictionsManager {

    @Inject
    @Setter(AccessLevel.PACKAGE)
    AriesClient ac;

    @Inject
    BPARestrictionsRepository repo;

    @Inject
    BPASchemaRepository schemaRepo;

    public Optional<RestrictionResponse> addRestriction(
            @NonNull UUID sId, @NonNull String issuerDid, @Nullable String label) {
        Optional<BPASchema> dbSchema = schemaRepo.findById(sId);
        if (dbSchema.isEmpty()) {
            throw new WrongApiUsageException("Schema with id: " + sId + " does not exist in the db");
        }
        return addRestriction(sId, Boolean.FALSE,
                List.of(Map.of("issuerDid", issuerDid, "label", label != null ? label : "")));
    }

    Optional<RestrictionResponse> addRestriction(
            @NonNull UUID schemaId,
            @NonNull Boolean isReadOnly,
            @Nullable List<Map<String, String>> config) {
        ResultWrapper result = new ResultWrapper();
        if (CollectionUtils.isNotEmpty(config)) {
            config.forEach(c -> {
                String issuerDid = c.get("issuerDid");
                if (StringUtils.isNotEmpty(issuerDid)) {
                    try {
                        ac.ledgerDidVerkey(issuerDid).ifPresent(verkey -> {
                            BPARestrictions def = BPARestrictions
                                    .builder()
                                    .issuerDid(issuerDid)
                                    .label(c.get("label"))
                                    .schema(BPASchema.builder().id(schemaId).build())
                                    .isReadOnly(isReadOnly)
                                    .build();
                            BPARestrictions db = repo.save(def);
                            result.setConfig(RestrictionResponse
                                    .builder()
                                    .id(db.getId())
                                    .label(db.getLabel())
                                    .issuerDid(db.getIssuerDid())
                                    .build());

                        });
                    } catch (IOException e) {
                        log.error("aca-py not available", e);
                    } catch (AriesException e) {
                        if (e.getCode() == 404) {
                            log.warn("Did: {} is not on the ledger", issuerDid);
                        }
                    }
                }
            });
        }
        return Optional.ofNullable(result.getConfig());
    }

    void resetReadOnly() {
        repo.deleteByIsReadOnly(Boolean.TRUE);
    }

    public void deleteCredentialDefinition(@NonNull UUID id) {
        repo.deleteById(id);
    }

    void deleteBySchema(@NonNull BPASchema schema) {
        repo.deleteBySchema(schema);
    }

    public void updateLabel(@NonNull UUID id, String label) {
        repo.updateLabel(id, label);
    }

    public Optional<BPARestrictions> findById(@NonNull UUID id) {
        return repo.findById(id);
    }

    @Data
    @NoArgsConstructor
    private static final class ResultWrapper {
        private RestrictionResponse config;
    }
}
