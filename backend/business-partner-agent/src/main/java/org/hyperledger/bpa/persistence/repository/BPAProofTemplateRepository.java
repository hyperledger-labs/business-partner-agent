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

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.PageableRepository;
import lombok.NonNull;
import org.hyperledger.bpa.persistence.model.BPAProofTemplate;

import java.util.UUID;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface BPAProofTemplateRepository extends PageableRepository<BPAProofTemplate, UUID> {

  @Query(
    value = "SELECT * " +
      "FROM \"bpa_proof_template\" bpaproof_template_ " +
      "WHERE bpaproof_template_.name " +
      "ILIKE CONCAT('%',:name,'%')",
    countQuery = "SELECT COUNT(*) " +
      "FROM \"bpa_proof_template\" bpaproof_template_ " +
      "WHERE bpaproof_template_.name " +
      "ILIKE CONCAT('%',:name,'%')")
  Page<BPAProofTemplate> findByNameContains(@Nullable String name, @NonNull Pageable pageable);
}