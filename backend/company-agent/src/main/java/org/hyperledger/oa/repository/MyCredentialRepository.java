/**
 * Copyright (c) 2020 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/organizational-agent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hyperledger.oa.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import org.hyperledger.oa.model.MyCredential;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface MyCredentialRepository extends CrudRepository<MyCredential, UUID> {

    void updateIsPublic(@Id UUID id, Boolean isPublic);

    void updateState(@Id UUID id, String state);

    void updateLabel(@Id UUID id, String label);

    Number updateByConnectionId(String id, @Nullable String connectionId);

    Optional<MyCredential> findByThreadId(String threadId);

    List<MyCredential> findByConnectionId(String connectionId);

    List<MyCredential> findByIsPublicTrue();

    @Query("SELECT * FROM my_credential WHERE credential->>'schemaId' = :schemaId "
            + "AND credential->>'credentialDefinitionId' = :credentialDefinitionId")
    List<MyCredential> findBySchemaIdAndCredentialDefinitionId(String schemaId, String credentialDefinitionId);

}
