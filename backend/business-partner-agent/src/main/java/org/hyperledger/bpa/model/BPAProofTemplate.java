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


import io.micronaut.core.annotation.Introspected;
import io.micronaut.data.annotation.AutoPopulated;
import io.micronaut.data.annotation.TypeDef;
import io.micronaut.data.model.DataType;
import lombok.*;
import org.hyperledger.bpa.controller.api.prooftemplates.ProofTemplate;
import org.hyperledger.bpa.impl.verification.prooftemplates.ValidAttributeGroup;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Introspected
@Entity
@Table(name = "bpa_proof_template")
public class BPAProofTemplate {
    @Id
    @AutoPopulated
    UUID id;

    @NotEmpty
    String name;

    @NotEmpty
    @Valid
    @Singular
    @ValidAttributeGroup
    @TypeDef(type = DataType.JSON)
    List<@ValidAttributeGroup BPAAttributeGroup> attributeGroups;

    public ProofTemplate toRepresentation() {
        return new ProofTemplate(
                id.toString(),
                name,
                attributeGroups.stream()
                        .map(BPAAttributeGroup::toRepresentation)
                        .collect(Collectors.toList())
        );
    }

    public static BPAProofTemplate fromRepresentation(ProofTemplate proofTemplate) {
        UUID id = null;
        if (proofTemplate.getId() != null) {
            id = UUID.fromString(proofTemplate.getId());
        }
        return new BPAProofTemplate(
                id,
                proofTemplate.getName(),
                proofTemplate.getAttributeGroups().stream()
                        .map(BPAAttributeGroup::fromRepresentation)
                        .collect(Collectors.toList())
        );
    }
}
