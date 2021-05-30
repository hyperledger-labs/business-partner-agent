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
import io.micronaut.data.annotation.TypeDef;
import io.micronaut.data.model.DataType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import io.micronaut.core.annotation.Nullable;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Cache for the profile.jsonld and .well-known/did.json documents
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class DidDocWeb {

    @Id
    @AutoPopulated
    private UUID id;

    @DateCreated
    private Instant createdAt;

    @DateUpdated
    private Instant updatedAt;

    /**
     * My did document when used in web mode, null otherwise
     */
    @Nullable
    @TypeDef(type = DataType.JSON)
    private Map<String, Object> didDoc;

    /**
     * My public profile, verifiable presentation structure
     */
    @Nullable
    @TypeDef(type = DataType.JSON)
    private Map<String, Object> profileJson;

}
