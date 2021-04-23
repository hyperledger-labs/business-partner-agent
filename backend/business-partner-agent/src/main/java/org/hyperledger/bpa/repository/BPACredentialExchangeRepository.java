package org.hyperledger.bpa.repository;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.data.annotation.Join;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import org.hyperledger.bpa.model.BPACredentialExchange;

import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface BPACredentialExchangeRepository extends CrudRepository<BPACredentialExchange, UUID> {
    @NonNull
    @Join(value = "schema", type = Join.Type.LEFT_FETCH)
    @Join(value = "credDef", type = Join.Type.LEFT_FETCH)
    @Join(value = "partner", type = Join.Type.LEFT_FETCH)
    Iterable<BPACredentialExchange> findAll();

    @NonNull
    @Join(value = "schema", type = Join.Type.LEFT_FETCH)
    @Join(value = "credDef", type = Join.Type.LEFT_FETCH)
    @Join(value = "partner", type = Join.Type.LEFT_FETCH)
    Optional<BPACredentialExchange> findById(@NonNull UUID id);

    @Join(value = "schema", type = Join.Type.LEFT_FETCH)
    @Join(value = "credDef", type = Join.Type.LEFT_FETCH)
    @Join(value = "partner", type = Join.Type.LEFT_FETCH)
    Optional<BPACredentialExchange> findByCredentialExchangeId(@NonNull String credentialExchangeId);
}
