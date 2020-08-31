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

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import org.hyperledger.oa.model.Partner;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface PartnerRepository extends CrudRepository<Partner, UUID> {

    void updateVerifiablePresentation(@Id UUID id, Map<String, Object> verifiablePresentation);

    void updateState(@Id UUID id, String state);

    int updateAlias(@Id UUID id, @Nullable String alias);

    int updateByConnectionId(String connectionId, String state);

    Optional<Partner> findByDid(String did);

    Optional<Partner> findByLabel(String label);

    Optional<Partner> findByConnectionId(String connectionId);
}
