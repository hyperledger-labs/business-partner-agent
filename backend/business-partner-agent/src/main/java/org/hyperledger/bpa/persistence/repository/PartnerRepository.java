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
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import org.hyperledger.acy_py.generated.model.InvitationRecord;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.bpa.persistence.model.Partner;
import org.hyperledger.bpa.persistence.model.StateChangeDecorator;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface PartnerRepository extends CrudRepository<Partner, UUID> {

    // find

    @Override
    @NonNull
    @Join(value = "tags", type = Join.Type.LEFT_FETCH)
    Optional<Partner> findById(@NonNull UUID id);

    @Override
    @NonNull
    @Join(value = "tags", type = Join.Type.LEFT_FETCH)
    Iterable<Partner> findAll();

    @Join(value = "tags", type = Join.Type.LEFT_FETCH)
    Optional<Partner> findByConnectionId(String connectionId);

    @Join(value = "tags", type = Join.Type.LEFT_FETCH)
    Optional<Partner> findByConnectionIdOrInvitationMsgId(@Nullable String connectionId,
            @Nullable String invitationMsgId);

    @Join(value = "tags", type = Join.Type.LEFT_FETCH)
    Optional<Partner> findByDid(String did);

    List<Partner> findByDidIn(List<String> did);

    @Join(value = "tags", type = Join.Type.LEFT_FETCH)
    Optional<Partner> findByInvitationMsgId(String invitationMsgId);

    @Query("SELECT distinct partner.* FROM partner,jsonb_to_recordset(partner.supported_credentials->'wrapped') as items(seqno text) where items.seqno = :seqNo")
    List<Partner> findBySupportedCredential(String seqNo);

    List<Partner> findByStateInAndTrustPingTrueAndAriesSupportTrue(List<ConnectionState> state);

    // delete

    @Query("delete from partner_tag where partner_id = :id; delete from partner where id = :id")
    void deleteByPartnerId(@NonNull UUID id);

    // count

    Long countByStateNotEquals(ConnectionState state);

    Long countByCreatedAtAfter(Instant createdAt);

    Long countByStateNotEqualsAndCreatedAtAfter(ConnectionState state, Instant createdAt);

    // update

    void updateState(@Id UUID id, ConnectionState state,
            StateChangeDecorator.StateToTimestamp<ConnectionState> stateToTimestamp);

    void updateStateAndLabel(@Id UUID id, ConnectionState state,
            StateChangeDecorator.StateToTimestamp<ConnectionState> stateToTimestamp, @Nullable String label);

    int updateAlias(@Id UUID id, @Nullable String alias, @Nullable Boolean trustPing);

    int updateDid(@Id UUID id, String did);

    Number updateByDid(String did, Map<String, Object> supportedCredentials);

    Number updateVerifiablePresentation(@Id UUID id,
            Map<String, Object> verifiablePresentation, @Nullable Boolean valid,
            String label, String did);

    Number updateVerifiablePresentation(@Id UUID id,
            Map<String, Object> verifiablePresentation, @Nullable Boolean valid);

    Number updateInvitationRecord(@Id UUID is, @Nullable InvitationRecord invitationRecord);

    // The queries below are native queries to prevent changes to the last updated
    // timestamp. As this timestamp indicates user interaction, whereas the queries
    // below indicate changes made by jobs.

    @Query("UPDATE partner SET state = :newState WHERE connection_id = :connectionId AND (state IS NULL OR state != :newState)")
    void updateStateByConnectionId(String connectionId, ConnectionState newState);

    @Query("UPDATE partner SET state = :newState, last_seen = :lastSeen WHERE connection_id = :connectionId")
    void updateStateAndLastSeenByConnectionId(String connectionId, ConnectionState newState, Instant lastSeen);

}
