package org.hyperledger.bpa.repository;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import org.hyperledger.bpa.model.BPACredentialDefinition;
import org.hyperledger.bpa.model.BPASchema;

import java.util.List;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface BPACredentialDefinitionRepository extends CrudRepository<BPACredentialDefinition, UUID> {

    List<BPACredentialDefinition> findBySchema(BPASchema schema);
}
