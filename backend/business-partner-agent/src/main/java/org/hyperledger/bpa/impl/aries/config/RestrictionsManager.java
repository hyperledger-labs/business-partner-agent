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

import io.micronaut.context.annotation.Value;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.exception.AriesException;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.controller.api.admin.TrustedIssuer;
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

    @Value("${bpa.did.prefix}")
    String didPrefix;

    @Inject
    @Setter(AccessLevel.PACKAGE)
    AriesClient ac;

    @Inject
    BPARestrictionsRepository repo;

    @Inject
    BPASchemaRepository schemaRepo;

    @Inject
    BPAMessageSource.DefaultMessageSource message;

    public Optional<TrustedIssuer> addRestriction(
            @NonNull UUID sId, @NonNull String issuerDid, @Nullable String label) {
        Optional<BPASchema> dbSchema = schemaRepo.findById(sId);
        if (dbSchema.isEmpty()) {
            throw new WrongApiUsageException("Schema with id: " + sId + " does not exist in the db");
        }
        return addRestriction(sId,
                List.of(Map.of("issuerDid", issuerDid, "label", label != null ? label : "")));
    }

    Optional<TrustedIssuer> addRestriction(
            @NonNull UUID schemaId,
            @Nullable List<Map<String, String>> config) {
        ResultWrapper result = new ResultWrapper();
        if (CollectionUtils.isNotEmpty(config)) {
            config.forEach(c -> {
                String issuerDid = c.get("issuerDid");
                if (StringUtils.isNotEmpty(issuerDid)) {
                    try {
                        // simple check to test if issuer exists on the ledger
                        ac.ledgerDidVerkey(issuerDid).ifPresent(verkey -> {
                            BPARestrictions def = BPARestrictions
                                    .builder()
                                    .issuerDid(issuerDid.startsWith("did:") ? issuerDid : didPrefix + issuerDid)
                                    .label(c.get("label"))
                                    .schema(BPASchema.builder().id(schemaId).build())
                                    .build();
                            BPARestrictions db = repo.save(def);
                            result.setConfig(TrustedIssuer
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
                            String msg = message.getMessage("api.schema.restriction.issuer.not.found",
                                    Map.of("did", issuerDid));
                            throw new WrongApiUsageException(msg);
                        }
                        throw new WrongApiUsageException(e.getMessage());
                    }
                }
            });
        }
        return Optional.ofNullable(result.getConfig());
    }

    public void deleteById(@NonNull UUID id) {
        repo.deleteById(id);
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
        private TrustedIssuer config;
    }
}
