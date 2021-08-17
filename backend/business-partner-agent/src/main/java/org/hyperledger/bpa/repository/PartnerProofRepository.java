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
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.Join;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

import org.hyperledger.aries.api.present_proof.PresentationExchangeState;
import org.hyperledger.bpa.model.PartnerProof;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface PartnerProofRepository extends CrudRepository<PartnerProof, UUID> {

    @Override
    @NonNull
    @Join(value = "proofTemplate", type = Join.Type.LEFT_FETCH)
    Optional<PartnerProof> findById(@NonNull UUID id);

    @Override
    @NonNull
    @Join(value = "proofTemplate", type = Join.Type.LEFT_FETCH)
    Iterable<PartnerProof> findAll();

    @NonNull
    @Join(value = "proofTemplate", type = Join.Type.LEFT_FETCH)
    Optional<PartnerProof> findByPresentationExchangeId(String presentationExchangeId);

    @NonNull
    @Join(value = "proofTemplate", type = Join.Type.LEFT_FETCH)
    Optional<PartnerProof> findByThreadId(String threadId);

    @NonNull
    @Join(value = "proofTemplate", type = Join.Type.LEFT_FETCH)
    List<PartnerProof> findByPartnerId(UUID partnerId);

    @NonNull
    @Join(value = "proofTemplate", type = Join.Type.LEFT_FETCH)
    List<PartnerProof> findByPartnerIdOrderByRole(UUID partnerId);

    void updateState(@Id UUID id, PresentationExchangeState state);

    void updateProblemReport(@Id UUID id, String problemReport);

    long updateReceivedProof(@Id UUID id, Instant issuedAt, Boolean valid, PresentationExchangeState state,
            Map<String, Object> proof);

    Iterable<PartnerProof> findByStateIn(List<PresentationExchangeState> states);

    Long countByStateEquals(PresentationExchangeState state);

    Long countByStateEqualsAndCreatedAtAfter(PresentationExchangeState state, Instant createdAt);
}
