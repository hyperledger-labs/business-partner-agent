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
import io.micronaut.data.annotation.Join;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.GenericRepository;
import org.hyperledger.bpa.model.Activity;

import java.util.List;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface ActivityRepository extends GenericRepository<Activity, UUID> {

    // @Join and @Query do not work together
    // Need @Query to select in 2 lists, but since not possible? let's take
    // advantage of the join to not have N+1 queries to get the partner
    // Will have to process the result set to match the states we want...
    // @Query("SELECT act.* FROM activity_vw as act where act.type in (:types) and
    // act.state in (:states)")
    @Join(value = "partner", type = Join.Type.LEFT_FETCH)
    Iterable<Activity> findByTypeIn(@NonNull List<String> types);

}
