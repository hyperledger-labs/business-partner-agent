package org.hyperledger.oa.repository;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import org.hyperledger.oa.model.MessageQueue;

import java.util.UUID;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface MessageQueueRepository extends CrudRepository<MessageQueue, UUID> {
}
