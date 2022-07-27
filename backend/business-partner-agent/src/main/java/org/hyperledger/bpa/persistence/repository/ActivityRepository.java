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
import io.micronaut.data.annotation.Join;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.PageableRepository;
import org.hyperledger.bpa.controller.api.activity.ActivityRole;
import org.hyperledger.bpa.controller.api.activity.ActivityType;
import org.hyperledger.bpa.persistence.model.Activity;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@JdbcRepository(dialect = Dialect.POSTGRES)
public interface ActivityRepository extends PageableRepository<Activity, UUID> {

    Optional<Activity> findByLinkIdAndTypeAndRole(@NonNull UUID linkId,
            @NonNull ActivityType type,
            @NonNull ActivityRole role);

    @Join(value = "partner", type = Join.Type.LEFT_FETCH)
    Page<Activity> listOrderByUpdatedAt(@NonNull Pageable pageable);

    @NonNull
    @Join(value = "partner", type = Join.Type.LEFT_FETCH)
    Page<Activity> findByType(@NonNull ActivityType type, @NonNull Pageable pageable);

    @Join(value = "partner", type = Join.Type.LEFT_FETCH)
    Page<Activity> findByCompletedFalse(@NonNull Pageable pageable);

    @Join(value = "partner", type = Join.Type.LEFT_FETCH)
    Page<Activity> findByTypeAndCompletedFalse(@NonNull ActivityType type, @NonNull Pageable pageable);

    @Join(value = "partner", type = Join.Type.LEFT_FETCH)
    Page<Activity> findByCompletedTrue(@NonNull Pageable pageable);

    @Join(value = "partner", type = Join.Type.LEFT_FETCH)
    Page<Activity> findByTypeAndCompletedTrue(@NonNull ActivityType type, @NonNull Pageable pageable);

    Long countByCompletedFalse();

    Long countByCompletedFalseAndCreatedAtAfter(Instant createdAt);

}
