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
    String schemaId;

    @Singular
    @Valid
    @NotNull
    List<Attribute> attributes;

    @NotNull
    @Builder.Default
    Boolean nonRevoked = Boolean.FALSE;
    @NotNull
    @Builder.Default
    @Valid
    SchemaRestrictions schemaLevelRestrictions = SchemaRestrictions.builder().build();
}
