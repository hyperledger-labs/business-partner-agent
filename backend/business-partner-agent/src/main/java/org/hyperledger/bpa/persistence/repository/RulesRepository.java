package org.hyperledger.bpa.persistence.repository;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import org.hyperledger.bpa.persistence.model.ActiveRules;

import java.util.UUID;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface RulesRepository extends CrudRepository<ActiveRules, UUID> {
    //
}
