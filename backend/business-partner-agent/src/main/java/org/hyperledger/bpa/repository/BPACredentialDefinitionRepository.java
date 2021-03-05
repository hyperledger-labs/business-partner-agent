package org.hyperledger.bpa.repository;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import org.hyperledger.bpa.model.BPACredentialDefinition;
import org.hyperledger.bpa.model.BPASchema;

import javax.annotation.Nullable;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface BPACredentialDefinitionRepository extends CrudRepository<BPACredentialDefinition, UUID> {

    void deleteByIsReadOnly(Boolean isReadOnly);

    void deleteBySchema(BPASchema schema);

    void updateLabel(@Id UUID id, @Nullable String label);
}
