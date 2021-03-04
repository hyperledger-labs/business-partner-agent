package org.hyperledger.bpa.impl.aries.config;

import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.controller.api.admin.CredentialDefinitionConfiguration;
import org.hyperledger.bpa.model.BPACredentialDefinition;
import org.hyperledger.bpa.model.BPASchema;
import org.hyperledger.bpa.repository.BPACredentialDefinitionRepository;
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
public class CredentialDefinitionManager {

    @Inject
    @Setter(AccessLevel.PACKAGE)
    AriesClient ac;

    @Inject
    BPACredentialDefinitionRepository repo;

    @Inject
    BPASchemaRepository schemaRepo;

    public Optional<CredentialDefinitionConfiguration> addCredentialDefinition(
            @NonNull UUID sId, @NonNull String credDefId, String label) {
        Optional<BPASchema> dbSchema = schemaRepo.findById(sId);
        if (dbSchema.isEmpty()) {
            throw new WrongApiUsageException("Schema with id: " + sId + " does not exist in the db");
        }
        return addCredentialDefinition(sId, dbSchema.get().getSeqNo(), Boolean.FALSE,
                List.of(Map.of("id", credDefId, "label", label != null ? label : "")));
    }

    Optional<CredentialDefinitionConfiguration> addCredentialDefinition(
            @NonNull UUID schemaId,
            @NonNull Integer schemaSeqNo,
            @NonNull Boolean isReadOnly,
            List<Map<String, String>> config) {
        ResultWrapper result = new ResultWrapper();
        if (CollectionUtils.isNotEmpty(config)) {
            config.forEach(c -> {
                String id = c.get("id");
                if (StringUtils.isNotEmpty(id)) {
                    try {
                        ac.credentialDefinitionsGetById(id).ifPresentOrElse(credDef -> {
                            // check that cred def is derived from the schema
                            if (Integer.valueOf(credDef.getSchemaId()).equals(schemaSeqNo)) {
                                BPACredentialDefinition def = BPACredentialDefinition
                                        .builder()
                                        .credentialDefinitionId(id)
                                        .label(c.get("label"))
                                        .schema(BPASchema.builder().id(schemaId).build())
                                        .isReadOnly(isReadOnly)
                                        .build();
                                BPACredentialDefinition db = repo.save(def);
                                System.out.println(db);
                                result.setConfig(CredentialDefinitionConfiguration
                                        .builder()
                                        .id(db.getId())
                                        .label(db.getLabel())
                                        .credentialDefinitionId(db.getCredentialDefinitionId())
                                        .build());
                            } else {
                                log.warn("Credential definition schema id: {}, does not match any schema id: {}",
                                        credDef.getSchemaId(), schemaSeqNo);
                            }
                        }, () -> log.warn("Credential definition id: {} does not exist on the ledger", id));
                    } catch (IOException e) {
                        log.error("aca-py not available", e);
                    }
                }
            });
        }
        return Optional.ofNullable(result.getConfig());
    }

    void resetReadOnly() {
        repo.deleteByIsReadOnly(Boolean.TRUE);
    }

    @Data
    @NoArgsConstructor
    private static final class ResultWrapper {
        private CredentialDefinitionConfiguration config;
    }
}
