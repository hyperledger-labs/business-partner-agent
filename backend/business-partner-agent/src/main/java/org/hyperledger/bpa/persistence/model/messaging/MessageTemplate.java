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
package org.hyperledger.bpa.persistence.model.messaging;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.AutoPopulated;
import io.micronaut.data.annotation.DateCreated;
import io.micronaut.data.annotation.DateUpdated;
import lombok.*;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * User defined message templates that can be mapped to a trigger or used
 * standalone
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "message_template")
public class MessageTemplate {

    @Id
    @AutoPopulated
    private UUID id;

    @DateCreated
    private Instant createdAt;

    @DateUpdated
    private Instant updatedAt;

    /** Message subject */
    @Nullable
    private String subject;

    /** Message template */
    private String template;

    /** Human readable label */
    @Nullable
    private String label;

    @OneToMany(mappedBy = "template")
    private Set<MessageTriggerConfig> triggerConfig;
}
