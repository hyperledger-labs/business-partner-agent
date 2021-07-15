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
import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.Join;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.bpa.model.Partner;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface PartnerRepository extends CrudRepository<Partner, UUID> {

    @Override
    @Join(value = "tags", type = Join.Type.LEFT_FETCH)
    Optional<Partner> findById(UUID id);

    @Override
    @NonNull
    @Join(value = "tags", type = Join.Type.LEFT_FETCH)
    Iterable<Partner> findAll();

    @Override
    @Query("delete from partner_tag where partner_id = :id; delete from partner where id = :id")
    void deleteById(@NonNull UUID id);

    void updateState(@Id UUID id, ConnectionState state);

    int updateAlias(@Id UUID id, @Nullable String alias);

    int updateDid(@Id UUID id, String did);

    Number updateByDid(String did, Map<String, Object> supportedCredentials);

    Number updateVerifiablePresentation(@Id UUID id,
            Map<String, Object> verifiablePresentation, @Nullable Boolean valid,
            String label, String did);

    Number updateVerifiablePresentation(@Id UUID id,
            Map<String, Object> verifiablePresentation, @Nullable Boolean valid);

    @Join(value = "tags", type = Join.Type.LEFT_FETCH)
    Optional<Partner> findByDid(String did);

    @Join(value = "tags", type = Join.Type.LEFT_FETCH)
    Optional<Partner> findByConnectionId(String connectionId);

    @Join(value = "tags", type = Join.Type.LEFT_FETCH)
    List<Partner> findByDidIn(List<String> did);

    @Query("SELECT distinct partner.* FROM partner,jsonb_to_recordset(partner.supported_credentials->'wrapped') as items(seqno text) where items.seqno = :seqNo")
    List<Partner> findBySupportedCredential(String seqNo);

    // The queries below are native queries to prevent changes to the lastupdated
    // timestamp. As this timestamp indicates user interaction, whereas the queries
    // below indicate changes made by jobs.

    @Query("UPDATE partner SET state = :newState WHERE connection_id = :connectionId AND (state IS NULL OR state != :newState)")
    void updateStateByConnectionId(String connectionId, ConnectionState newState);

    @Query("UPDATE partner SET state = :newState, last_seen = :lastSeen WHERE connection_id = :connectionId")
    void updateStateAndLastSeenByConnectionId(String connectionId, ConnectionState newState, Instant lastSeen);

    Iterable<Partner> findByStateIn(List<ConnectionState> states);

}
