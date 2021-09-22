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

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.AutoPopulated;
import io.micronaut.data.annotation.DateCreated;
import io.micronaut.data.annotation.TypeDef;
import io.micronaut.data.model.DataType;
import lombok.*;

import javax.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.SortedSet;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "bpaschema")
public class BPASchema {

    @Id
    @AutoPopulated
    private UUID id;

    @DateCreated
    private Instant createdAt;

    @Nullable
    private String label;

    private String schemaId;

    @TypeDef(type = DataType.JSON)
    @Singular
    private SortedSet<String> schemaAttributeNames;

    @Nullable
    private String defaultAttributeName;

    private Integer seqNo;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "schema", cascade = { CascadeType.PERSIST, CascadeType.REFRESH })
    private List<BPARestrictions> restrictions;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "schema", cascade = { CascadeType.PERSIST, CascadeType.REFRESH })
    private List<BPACredentialDefinition> credentialDefinitions;
}
