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

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.data.annotation.AutoPopulated;
import io.micronaut.data.annotation.DateCreated;
import io.micronaut.data.annotation.TypeDef;
import io.micronaut.data.model.DataType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.controller.api.prooftemplates.ProofTemplate;
import org.hyperledger.bpa.impl.verification.prooftemplates.SameGroupType;
import org.hyperledger.bpa.impl.verification.prooftemplates.SameSchemaType;
import org.hyperledger.bpa.persistence.model.prooftemplate.BPAAttribute;
import org.hyperledger.bpa.persistence.model.prooftemplate.BPAAttributeGroup;
import org.hyperledger.bpa.persistence.model.prooftemplate.BPAAttributeGroups;

import javax.persistence.*;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Introspected
@Entity
@SameSchemaType
@SameGroupType
@Table(name = "bpa_proof_template")
public class BPAProofTemplate {
    @Id
    @AutoPopulated
    private UUID id;

    @Nullable
    @DateCreated
    private Instant createdAt;

    @NotEmpty
    String name;

    @Enumerated(EnumType.STRING)
    private CredentialType type;

    @Valid
    @NotNull
    @Column(name = "attribute_groups_json")
    @TypeDef(type = DataType.JSON)
    // using a concrete class instead of a generic list does not unmarshal correctly
    // see https://github.com/micronaut-projects/micronaut-data/issues/1064
    private BPAAttributeGroups attributeGroups;

    public Stream<BPAAttributeGroup> streamAttributeGroups() {
        return attributeGroups.getAttributeGroups().stream();
    }

    public ProofTemplate toRepresentation() {
        return new ProofTemplate(
                id,
                createdAt,
                name,
                type,
                attributeGroups.toRepresentation());
    }

    public static BPAProofTemplate fromRepresentation(ProofTemplate proofTemplate) {
        UUID id = null;
        if (proofTemplate.getId() != null) {
            id = proofTemplate.getId();
        }
        return BPAProofTemplate.builder()
                .id(id)
                .name(proofTemplate.getName())
                .attributeGroups(BPAAttributeGroups.fromRepresentation(proofTemplate.getAttributeGroups()))
                .build();
    }

    public boolean typeIsIndy() {
        return CredentialType.INDY.equals(type);
    }

    public boolean typeIsJsonLD() {
        return CredentialType.JSON_LD.equals(type);
    }

    public boolean allowSelfAttested() {
        return streamAttributeGroups()
                .map(BPAAttributeGroup::allowSelfAttested)
                .findFirst()
                .orElse(Boolean.FALSE);
    }

    public Set<String> collectAttributes() {
        return streamAttributeGroups()
                .filter(BPAAttributeGroup::allowSelfAttested)
                .map(BPAAttributeGroup::getAttributes)
                .flatMap(List::stream)
                .map(BPAAttribute::getName)
                .collect(Collectors.toSet());

    }
}
