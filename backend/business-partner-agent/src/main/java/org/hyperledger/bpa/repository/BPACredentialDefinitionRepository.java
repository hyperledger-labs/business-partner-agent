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
package org.hyperledger.bpa.repository;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.data.annotation.Join;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import org.hyperledger.bpa.model.BPACredentialDefinition;

import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface BPACredentialDefinitionRepository extends CrudRepository<BPACredentialDefinition, UUID> {

    @NonNull
    @Join(value = "schema", type = Join.Type.LEFT_FETCH)
    Iterable<BPACredentialDefinition> findAll();

    @NonNull
    @Join(value = "schema", type = Join.Type.LEFT_FETCH)
    Optional<BPACredentialDefinition> findById(@NonNull UUID id);

    @Query("SELECT * FROM bpa_cred_def c " +
            "LEFT JOIN bpaschema s ON c.schema_id = s.id " +
            "WHERE s.schema_id = :schemaId")
    Optional<BPACredentialDefinition> findBySchemaId(String schemaId);

    @NonNull
    Optional<BPACredentialDefinition> findByCredentialDefinitionId(@NonNull String credentialDefinitionId);

}
