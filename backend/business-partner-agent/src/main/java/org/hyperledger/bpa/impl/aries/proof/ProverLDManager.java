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
package org.hyperledger.bpa.impl.aries.proof;

import jakarta.inject.Singleton;
import lombok.NonNull;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hyperledger.acy_py.generated.model.*;
import org.hyperledger.aries.api.present_proof_v2.V20PresProposalByFormat;
import org.hyperledger.aries.api.present_proof_v2.V20PresProposalRequest;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.persistence.model.BPACredentialExchange;
import org.hyperledger.bpa.persistence.model.BPASchema;

import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class ProverLDManager {

    private static final String DEFAULT_PATH = "$.credentialSubject.";

    public static V20PresProposalRequest prepareProposal(@NonNull String connectionId, @NonNull BPACredentialExchange credEx) {
        BPASchema schema = credEx.getSchema();

        Map<UUID, DIFField> fields = buildDifFields(credEx.credentialAttributesToMap());

        InputDescriptors id1 = InputDescriptors.builder()
                .schema(SchemasInputDescriptorFilter.builder()
                        .uriGroups(List.of(buildSchemaInputDescriptor(Objects.requireNonNull(schema).getSchemaId())))
                        .build())
                .constraints(Constraints.builder()
                        .isHolder(buildDifHolder(fields.keySet()))
                        .fields(List.copyOf(fields.values()))
                        .build())
                .build();

        return V20PresProposalRequest.builder()
                .connectionId(connectionId)
                .autoPresent(Boolean.TRUE)
                .presentationProposal(V20PresProposalByFormat.builder()
                        .dif(DIFProofProposal.builder()
                                .inputDescriptors(List.of(id1))
                                .build())
                        .build())
                .build();
    }

    private static List<SchemaInputDescriptor> buildSchemaInputDescriptor(@NonNull String schemaId) {
        List<Object> ctx =new ArrayList<>(CredentialType.JSON_LD.getContext());
        ctx.add(schemaId);
        return ctx.stream().map(o -> SchemaInputDescriptor.builder().uri((String) o).build()).collect(Collectors.toList());
    }

    private static List<DIFHolder> buildDifHolder(@NonNull Set<UUID> fields) {
        return fields
                .stream().map(e -> DIFHolder.builder()
                        .directive(DIFHolder.DirectiveEnum.REQUIRED)
                        .fieldId(List.of(e.toString()))
                        .build())
                .collect(Collectors.toList());
    }

    private static Map<UUID, DIFField> buildDifFields(@NonNull Map<String, String> ldAttributes) {
        return ldAttributes.entrySet().stream()
                .map( e -> {
                    UUID key = UUID.randomUUID();
                    DIFField f = DIFField.builder()
                            .id(key.toString())
                            .path(List.of(DEFAULT_PATH + e.getKey()))
                            .filter(Filter.builder()
                                    ._const(e.getValue())
                                    .build())
                            .build();
                    return new ImmutablePair<>(key, f);
                })
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    }


}
