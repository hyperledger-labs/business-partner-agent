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
package org.hyperledger.bpa.persistence.model;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.*;
import io.micronaut.data.model.DataType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.persistence.model.type.CredentialTypeTranslator;

import javax.persistence.Id;
import javax.persistence.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * My document. Documents are just a collection of data. If documents are made
 * public they become part of the public profile. Documents can become
 * credentials when they are used in the context of aries, or when they are
 * transformed into verifiable credentials in the public profile. The partners
 * profile is saved with the {@link Partner}
 */
@NoArgsConstructor
@Data
@Accessors(chain = true)
@Entity
public class MyDocument implements CredentialTypeTranslator {

    @Id
    @AutoPopulated
    private UUID id;

    @DateCreated
    private Instant createdAt;

    @DateUpdated
    private Instant updatedAt;

    @Enumerated(EnumType.STRING)
    private CredentialType type;

    /** There for backwards compatibility, use reference to schema instead */
    @Nullable
    private String schemaId;

    @Nullable
    @ManyToOne
    @MappedProperty("fk_schema_id")
    private BPASchema schema;

    private Boolean isPublic = Boolean.FALSE;

    @Nullable
    private String label;

    @Nullable
    @TypeDef(type = DataType.JSON)
    private Map<String, Object> document;

}
