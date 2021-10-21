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
import io.micronaut.data.annotation.Join;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;
import org.hyperledger.bpa.model.BPACredentialExchange;
import org.hyperledger.bpa.model.StateChangeDecorator;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface HolderCredExRepository extends CrudRepository<BPACredentialExchange, UUID> {

    // find

    List<BPACredentialExchange> findByRoleEquals(CredentialExchangeRole role);

    Optional<BPACredentialExchange> findByReferent(String referent);

    List<BPACredentialExchange> findByPartnerId(UUID partnerId);

    @Join(value = "partner", type = Join.Type.LEFT_FETCH)
    Optional<BPACredentialExchange> findByCredentialExchangeId(String credentialExchangeId);

    List<BPACredentialExchange> findByRoleAndIsPublicTrue(CredentialExchangeRole role);

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

    void updateState(@Id UUID id, CredentialExchangeState state);

    void updateStates(@Id UUID id, CredentialExchangeState state,
            StateChangeDecorator.StateToTimestamp<CredentialExchangeState> stateToTimestamp, @Nullable String errorMsg);

    void updateOnCredentialOfferEvent(@Id UUID id, CredentialExchangeState state,
            StateChangeDecorator.StateToTimestamp<CredentialExchangeState> stateToTimestamp,
            V1CredentialExchange.CredentialProposalDict.CredentialProposal credentialOffer);

    void updateLabel(@Id UUID id, String label);

    Number updateRevoked(@Id UUID id, Boolean revoked);

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
