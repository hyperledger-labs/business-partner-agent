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
package org.hyperledger.bpa.persistence.repository.messaging;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.Join;
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.repository.CrudRepository;
import org.hyperledger.bpa.persistence.model.messaging.MessageTemplate;
import org.hyperledger.bpa.persistence.model.messaging.MessageTrigger;
import org.hyperledger.bpa.persistence.model.messaging.MessageTriggerConfig;
import org.hyperledger.bpa.persistence.model.messaging.MessageUserInfo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@JdbcRepository
public interface MessageTriggerConfigRepository extends CrudRepository<MessageTriggerConfig, UUID> {

    @Override
    @NonNull
    @Join(value = "userInfo", type = Join.Type.LEFT_FETCH)
    @Join(value = "template", type = Join.Type.LEFT_FETCH)
    Iterable<MessageTriggerConfig> findAll();

    @Override
    @NonNull
    @Join(value = "userInfo", type = Join.Type.LEFT_FETCH)
    @Join(value = "template", type = Join.Type.LEFT_FETCH)
    Optional<MessageTriggerConfig> findById(@NonNull UUID id);

    @Join(value = "userInfo", type = Join.Type.LEFT_FETCH)
    @Join(value = "template", type = Join.Type.LEFT_FETCH)
    List<MessageTriggerConfig> findByTrigger(MessageTrigger trigger);

    Number updateTriggerConfig(@Id UUID id, MessageTrigger trigger, @Nullable MessageTemplate template, MessageUserInfo userInfo);
}
