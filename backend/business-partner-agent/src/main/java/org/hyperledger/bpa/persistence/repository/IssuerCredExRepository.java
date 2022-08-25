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
import org.hyperledger.aries.api.credentials.Credential;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecordByFormat;
import org.hyperledger.bpa.persistence.model.BPACredentialExchange;
import org.hyperledger.bpa.persistence.model.Partner;
import org.hyperledger.bpa.persistence.model.StateChangeDecorator;
import org.hyperledger.bpa.persistence.model.converter.ExchangePayload;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface IssuerCredExRepository extends PageableRepository<BPACredentialExchange, UUID> {

    @Override
    @NonNull
    @Join(value = "schema", type = Join.Type.LEFT_FETCH)
    @Join(value = "credDef", type = Join.Type.LEFT_FETCH)
    @Join(value = "partner", type = Join.Type.LEFT_FETCH)
    @Deprecated // only use paging for exchanges
    List<BPACredentialExchange> findAll();

    @Override
    @NonNull
    @Join(value = "schema", type = Join.Type.LEFT_FETCH)
    @Join(value = "credDef", type = Join.Type.LEFT_FETCH)
    @Join(value = "partner", type = Join.Type.LEFT_FETCH)
    Optional<BPACredentialExchange> findById(@NonNull UUID id);

    @Join(value = "schema", type = Join.Type.LEFT_FETCH)
    @Join(value = "credDef", type = Join.Type.LEFT_FETCH)
    @Join(value = "partner", type = Join.Type.LEFT_FETCH)
    Optional<BPACredentialExchange> findByCredentialExchangeId(@NonNull String credentialExchangeId);

    int countIdByCredDefId(@NonNull UUID credDefId);

    @Join(value = "schema", type = Join.Type.LEFT_FETCH)
    @Join(value = "credDef", type = Join.Type.LEFT_FETCH)
    @Join(value = "partner", type = Join.Type.LEFT_FETCH)
    Page<BPACredentialExchange> findByRoleIn(@NonNull List<CredentialExchangeRole> role, @NonNull Pageable pageable);

    @Join(value = "schema", type = Join.Type.LEFT_FETCH)
    @Join(value = "credDef", type = Join.Type.LEFT_FETCH)
    @Join(value = "partner", type = Join.Type.LEFT_FETCH)
    Page<BPACredentialExchange> findByRoleInAndPartnerEquals(@NonNull List<CredentialExchangeRole> role,
            @NonNull Partner partner, @NonNull Pageable pageable);

    Number updateCredential(@Id UUID id, Credential indyCredential);

    Number updateCredential(@Id UUID id,
            ExchangePayload<V1CredentialExchange.CredentialProposalDict.CredentialProposal, V20CredExRecordByFormat.LdProof> ldCredential);

    void updateAfterEventWithRevocationInfo(@Id UUID id,
            CredentialExchangeState state,
            StateChangeDecorator.StateToTimestamp<CredentialExchangeState> stateToTimestamp,
            @Nullable String revRegId,
            @Nullable String credRevId,
            @Nullable String errorMsg);

    void updateAfterEventNoRevocationInfo(@Id UUID id,
            CredentialExchangeState state,
            StateChangeDecorator.StateToTimestamp<CredentialExchangeState> stateToTimestamp,
            @Nullable String errorMsg);

    void updateRevocationInfo(@Id UUID id, String revRegId, @Nullable String credRevId);

    void updateReferent(@Id UUID id, String referent);

    void updateByCredentialExchangeId(String credentialExchangeId, String referent);
}
