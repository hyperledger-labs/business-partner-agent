/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
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
package org.hyperledger.bpa.persistence.repository;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.Join;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.PageableRepository;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.persistence.model.MyDocument;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface MyDocumentRepository extends PageableRepository<MyDocument, UUID> {

    @Override
    @NonNull
    @Join(value = "schema", type = Join.Type.LEFT_FETCH)
    Optional<MyDocument> findById(@NonNull UUID id);

    /**
     * Find all my public credentials
     *
     * @return list of public credentials
     */
    @Join(value = "schema", type = Join.Type.LEFT_FETCH)
    List<MyDocument> findByIsPublicTrue();

    Page<MyDocument> findByTypeIn(List<CredentialType> type, Pageable pageable);

    boolean existsByTypeEqualsAndIsPublicTrue(CredentialType type);

    // this one is for migration of old agent versions only
    void updateSchemaId(@Id UUID id, @Nullable String schemaId);
}
