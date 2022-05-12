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
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.PageableRepository;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecordByFormat;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.persistence.model.BPACredentialExchange;
import org.hyperledger.bpa.persistence.model.StateChangeDecorator;
import org.hyperledger.bpa.persistence.model.converter.ExchangePayload;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface HolderCredExRepository extends PageableRepository<BPACredentialExchange, UUID> {

    // find

    @NonNull
    @Override
    @Join(value = "schema", type = Join.Type.LEFT_FETCH)
    Optional<BPACredentialExchange> findById(@NonNull UUID id);

    @Join(value = "schema", type = Join.Type.LEFT_FETCH)
    Page<BPACredentialExchange> findByRoleEqualsAndStateInAndTypeIn(
            CredentialExchangeRole role,
            List<CredentialExchangeState> state,
            List<CredentialType> type,
            Pageable pageable);

    Optional<BPACredentialExchange> findByReferent(String referent);

    List<BPACredentialExchange> findByPartnerId(UUID partnerId);

    @Join(value = "schema", type = Join.Type.LEFT_FETCH)
    @Join(value = "partner", type = Join.Type.LEFT_FETCH)
    Optional<BPACredentialExchange> findByCredentialExchangeId(String credentialExchangeId);

    List<BPACredentialExchange> findByRoleAndIsPublicTrue(CredentialExchangeRole role);

    Optional<BPACredentialExchange> findByRevRegIdAndCredRevId(String revRegId, String credRefId);

    @Query("SELECT * FROM bpa_credential_exchange WHERE credential->>'schemaId' = :schemaId "
            + "AND credential->>'credentialDefinitionId' = :credentialDefinitionId "
            + "AND role = 'HOLDER'")
    List<BPACredentialExchange> findBySchemaIdAndCredentialDefinitionId(String schemaId, String credentialDefinitionId);

    @Query("SELECT * FROM bpa_credential_exchange WHERE type = 'INDY' "
            + "AND referent IS NOT NULL AND (revoked IS NULL OR revoked = false) "
            + "AND role = 'HOLDER'")
    List<BPACredentialExchange> findNotRevoked();

    // update

    void updateIsPublic(@Id UUID id, Boolean isPublic);

    void updateStates(@Id UUID id, CredentialExchangeState state,
            StateChangeDecorator.StateToTimestamp<CredentialExchangeState> stateToTimestamp, @Nullable String errorMsg);

    void updateOnCredentialOfferEvent(@Id UUID id, CredentialExchangeState state,
            StateChangeDecorator.StateToTimestamp<CredentialExchangeState> stateToTimestamp,
            ExchangePayload<V1CredentialExchange.CredentialProposalDict.CredentialProposal, V20CredExRecordByFormat.LdProof> credentialOffer);

    void updateLabel(@Id UUID id, String label);

    Number updateRevoked(@Id UUID id, Boolean revoked,
            CredentialExchangeState state,
            StateChangeDecorator.StateToTimestamp<CredentialExchangeState> stateToTimestamp);

    Number updateReferent(@Id UUID id, @Nullable String referent);

    @Query("UPDATE bpa_credential_exchange SET issuer = :issuer WHERE partner_id = :partnerId AND role = 'HOLDER'")
    Number updateIssuerByPartnerId(UUID partnerId, @Nullable String issuer);

    @Query("UPDATE bpa_credential_exchange SET partner_id = null WHERE partner_id = :partnerId AND role = 'HOLDER'")
    Number setPartnerIdToNull(UUID partnerId);

    // count

    Long countByRoleEqualsAndStateEquals(CredentialExchangeRole role, CredentialExchangeState state);

    Long countByRoleEqualsAndStateEqualsAndCreatedAtAfter(
            CredentialExchangeRole role, CredentialExchangeState state, Instant issuedAt);

}
