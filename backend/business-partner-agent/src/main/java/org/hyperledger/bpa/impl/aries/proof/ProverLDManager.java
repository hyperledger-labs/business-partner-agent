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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hyperledger.acy_py.generated.model.DIFHolder;
import org.hyperledger.acy_py.generated.model.DIFOptions;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.present_proof_v2.DIFField;
import org.hyperledger.aries.api.present_proof_v2.V20PresProposalByFormat;
import org.hyperledger.aries.api.present_proof_v2.V20PresProposalRequest;
import org.hyperledger.aries.api.present_proof_v2.V2DIFProofRequest;
import org.hyperledger.bpa.api.exception.NetworkException;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.persistence.model.BPACredentialExchange;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class ProverLDManager {

    private static final String DEFAULT_PATH = "$.credentialSubject.";

    @Inject
    AriesClient ac;

    @Inject
    BPAMessageSource.DefaultMessageSource ms;

    public V20PresProposalRequest prepareProposal(@NonNull String connectionId,
            @NonNull BPACredentialExchange credEx) {

        Map<UUID, DIFField> fields = buildDifFields(credEx.credentialAttributesToMap());

        String name = credEx.getSchema() != null ? credEx.getSchema().resolveSchemaLabelEscaped()
                : UUID.randomUUID().toString();
        V2DIFProofRequest.PresentationDefinition.InputDescriptors id1 = V2DIFProofRequest.PresentationDefinition.InputDescriptors
                .builder()
                .id(name)
                .name(name)
                .schema(buildSchemaInputDescriptor(Objects.requireNonNull(credEx.getReferent())))
                .constraints(V2DIFProofRequest.PresentationDefinition.Constraints.builder()
                        .isHolder(buildDifHolder(fields.keySet()))
                        .fields(List.copyOf(fields.values()))
                        .build())
                .build();

        return V20PresProposalRequest.builder()
                .connectionId(connectionId)
                .autoPresent(Boolean.FALSE) // is ignored with aca-py 0.7.4
                .presentationProposal(V20PresProposalByFormat.builder()
                        .dif(V20PresProposalByFormat.DIFProofProposal.builder()
                                .inputDescriptors(List.of(id1))
                                .options(DIFOptions.builder()
                                        .challenge(UUID.randomUUID().toString())
                                        .domain(UUID.randomUUID().toString())
                                        .build())
                                .build())
                        .build())
                .build();
    }

    private List<V2DIFProofRequest.PresentationDefinition.InputDescriptors.SchemaInputDescriptorUri> buildSchemaInputDescriptor(
            @NonNull String referent) {
        List<V2DIFProofRequest.PresentationDefinition.InputDescriptors.SchemaInputDescriptorUri> result = new ArrayList<>();
        try {
            ac.credentialW3C(referent).ifPresent(w3c -> w3c.getExpandedTypes().stream()
                    .map(u -> V2DIFProofRequest.PresentationDefinition.InputDescriptors.SchemaInputDescriptorUri
                            .builder().uri(u).build())
                    .forEach(result::add));
        } catch (IOException e) {
            throw new NetworkException(ms.getMessage("acapy.unavailable"), e);
        }
        return result;
    }

    private List<DIFHolder> buildDifHolder(@NonNull Set<UUID> fields) {
        return fields
                .stream().map(e -> DIFHolder.builder()
                        .directive(DIFHolder.DirectiveEnum.PREFERRED)
                        .fieldId(List.of(e.toString()))
                        .build())
                .collect(Collectors.toList());
    }

    private Map<UUID, DIFField> buildDifFields(@NonNull Map<String, String> ldAttributes) {
        return ldAttributes.entrySet().stream()
                .map(e -> {
                    UUID key = UUID.randomUUID();
                    DIFField f = DIFField.builder()
                            .id(key.toString())
                            .path(List.of(DEFAULT_PATH + e.getKey()))
                            .filter(DIFField.Filter.builder()
                                    ._const(e.getValue())
                                    .build())
                            .build();
                    return new ImmutablePair<>(key, f);
                })
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    }
}