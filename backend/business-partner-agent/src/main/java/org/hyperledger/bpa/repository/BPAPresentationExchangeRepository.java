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
import org.hyperledger.bpa.model.BPAPresentationExchange;

import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface BPAPresentationExchangeRepository extends CrudRepository<BPAPresentationExchange, UUID> {

    @Join(value = "partner", type = Join.Type.LEFT_FETCH)
    Iterable<BPAPresentationExchange> findAll();

    @Join(value = "partner", type = Join.Type.LEFT_FETCH)
    Iterable<BPAPresentationExchange> findByPartnerId(@NonNull UUID partnerId);

    @Join(value = "partner", type = Join.Type.LEFT_FETCH)
    Optional<BPAPresentationExchange> findById(@NonNull UUID id);

    @Join(value = "partner", type = Join.Type.LEFT_FETCH)
    Optional<BPAPresentationExchange> findByPresentationExchangeId(@NonNull String presentationExchangeId);

    void updateState(@Id UUID id, PresentationExchangeState state);
}
