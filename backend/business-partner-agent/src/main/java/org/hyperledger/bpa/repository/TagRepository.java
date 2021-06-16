package org.hyperledger.bpa.repository;

import io.micronaut.core.annotation.NonNull;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import org.hyperledger.bpa.model.Tag;

import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface TagRepository extends CrudRepository<Tag, UUID> {

    Optional<Tag> findByName(String name);

    @NonNull
    Iterable<Tag> findAll();

    void deleteByIsReadOnly(Boolean isReadOnly);

}
