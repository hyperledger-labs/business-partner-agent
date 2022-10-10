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
package org.hyperledger.bpa.persistence.model.prooftemplate;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.CollectionUtils;
import lombok.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hyperledger.bpa.controller.api.prooftemplates.AttributeGroup;
import org.hyperledger.bpa.impl.verification.prooftemplates.DistinctAttributeNames;
import org.hyperledger.bpa.impl.verification.prooftemplates.ValidAttributeGroup;
import org.hyperledger.bpa.impl.verification.prooftemplates.ValidBPASchemaId;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Introspected
@ValidAttributeGroup
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
// using a concrete class instead of a generic list does not unmarshal correctly
// see https://github.com/micronaut-projects/micronaut-data/issues/1064
public class BPAAttributeGroup {

    @NotNull
    @ValidBPASchemaId
    private UUID schemaId;

    @NotNull
    @Singular
    @Valid
    @DistinctAttributeNames
    private List<BPAAttribute> attributes;

    @NotNull
    @Builder.Default
    private Boolean nonRevoked = Boolean.FALSE;

    @Nullable
    @Valid
    private List<BPASchemaRestrictions> schemaLevelRestrictions;

    public AttributeGroup toRepresentation() {
        return AttributeGroup
                .builder()
                .schemaId(schemaId)
                .attributes(attributes.stream()
                        .map(BPAAttribute::toRepresentation)
                        .collect(Collectors.toList()))
                .nonRevoked(nonRevoked)
                .schemaLevelRestrictions(CollectionUtils.isNotEmpty(schemaLevelRestrictions) ? schemaLevelRestrictions
                        .stream()
                        .map(BPASchemaRestrictions::toRepresentation)
                        .collect(Collectors.toList())
                        : null)
                .build();
    }

    public static BPAAttributeGroup fromRepresentation(AttributeGroup attributeGroup) {
        return BPAAttributeGroup.builder()
                .schemaId(attributeGroup.getSchemaId())
                .attributes(attributeGroup.getAttributes().stream()
                        .map(BPAAttribute::fromRepresentation)
                        .collect(Collectors.toList()))
                .nonRevoked(attributeGroup.getNonRevoked())
                .schemaLevelRestrictions(CollectionUtils.isNotEmpty(attributeGroup.getSchemaLevelRestrictions())
                        ? attributeGroup.getSchemaLevelRestrictions().stream()
                                .map(BPASchemaRestrictions::fromRepresentation)
                                .collect(Collectors.toList())
                        : null)
                .build();
    }

    public Map<String, BPACondition> nameToCondition() {
        return attributes != null ? attributes.stream()
                .map(attr -> new ImmutablePair<>(attr.getName(),
                        // Frontend does not support more than one
                        CollectionUtils.isNotEmpty(attr.getConditions()) ? attr.getConditions().get(0) : null))
                .collect(HashMap::new, (m, v) -> m.put(v.getKey(), v.getValue()), HashMap::putAll)
                : Map.of();
    }
}
