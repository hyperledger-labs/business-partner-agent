package org.hyperledger.oa.repository;

import java.util.UUID;

import org.hyperledger.oa.model.BPAState;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface BPAStateRepository extends CrudRepository<BPAState, UUID> {
    //
}
