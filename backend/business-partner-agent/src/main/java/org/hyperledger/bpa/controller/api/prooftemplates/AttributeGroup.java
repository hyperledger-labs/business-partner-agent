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

package org.hyperledger.bpa.controller.api.prooftemplates;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.util.CollectionUtils;
import lombok.*;
import org.hyperledger.bpa.impl.verification.ValidUUID;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Introspected
public class AttributeGroup {
    @NotNull
    @ValidUUID
    private String schemaId;

    private String attributeGroupName;

    @Singular
    @Valid
    @NotNull
    private List<Attribute> attributes;

    @NotNull
    @Builder.Default
    private Boolean nonRevoked = Boolean.FALSE;

    @NotNull
    @Builder.Default
    @Valid
    private List<SchemaRestrictions> schemaLevelRestrictions = List.of(SchemaRestrictions.builder().build());

    public @NonNull List<SchemaRestrictions> getSchemaLevelRestrictions() {
        if (CollectionUtils.isEmpty(schemaLevelRestrictions)) {
            return List.of(SchemaRestrictions.builder().build());
        }
        return schemaLevelRestrictions;
    }
}
