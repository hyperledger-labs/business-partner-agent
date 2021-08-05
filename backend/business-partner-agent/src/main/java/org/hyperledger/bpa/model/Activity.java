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
package org.hyperledger.bpa.model;

import io.micronaut.data.annotation.AutoPopulated;
import io.micronaut.data.annotation.DateCreated;
import io.micronaut.data.annotation.DateUpdated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hyperledger.bpa.controller.api.activity.ActivityRole;
import org.hyperledger.bpa.controller.api.activity.ActivityState;
import org.hyperledger.bpa.controller.api.activity.ActivityType;

import javax.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "activity")
public class Activity {

    @Id
    @AutoPopulated
    private UUID id;

    private UUID linkId;

    @OneToOne
    private Partner partner;

    @Enumerated(EnumType.STRING)
    private ActivityType type;

    @Enumerated(EnumType.STRING)
    private ActivityRole role;

    @Enumerated(EnumType.STRING)
    private ActivityState state;

    private boolean completed;

    @DateCreated
    private Instant createdAt;
    @DateUpdated
    private Instant updatedAt;
}
