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

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.bpa.model.BPACredentialExchange;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface HolderCredExRepository extends CrudRepository<BPACredentialExchange, UUID> {

    // find

    Optional<BPACredentialExchange> findByReferent(String referent);

    List<BPACredentialExchange> findByPartnerId(UUID partnerId);

    List<BPACredentialExchange> findByIsPublicTrue();

    Optional<BPACredentialExchange> findByCredentialExchangeId(String credentialExchangeId);

    @Query("SELECT * FROM my_credential WHERE credential->>'schemaId' = :schemaId "
            + "AND credential->>'credentialDefinitionId' = :credentialDefinitionId")
    List<BPACredentialExchange> findBySchemaIdAndCredentialDefinitionId(String schemaId, String credentialDefinitionId);

    @Query("SELECT * FROM my_credential WHERE type = 'INDY' AND referent IS NOT NULL AND (revoked IS NULL OR revoked = false)")
    List<BPACredentialExchange> findNotRevoked();

    // update

    void updateIsPublic(@Id UUID id, Boolean isPublic);

    void updateState(@Id UUID id, CredentialExchangeState state);

    void updateLabel(@Id UUID id, String label);

    Number updateRevoked(@Id UUID id, Boolean revoked);

    Number updateByPartnerId(UUID id, @Nullable UUID partnerId, @Nullable String issuer);

    Number updateByPartnerId(UUID id, @Nullable UUID partnerId);

    // count

    Long countByStateEquals(CredentialExchangeState state);

    Long countByStateEqualsAndCreatedAtAfter(CredentialExchangeState state, Instant issuedAt);

}
