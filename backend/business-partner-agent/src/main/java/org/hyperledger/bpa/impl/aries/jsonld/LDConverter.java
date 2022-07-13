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
package org.hyperledger.bpa.impl.aries.jsonld;

import com.google.gson.JsonObject;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.aries.api.present_proof.PresentProofRequest;
import org.hyperledger.aries.api.present_proof_v2.V2DIFProofRequest;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LDConverter {

    private static final String PATH = "$.credentialSubject.";

    public static PresentProofRequest.ProofRequest difToIndyProofRequest(@NonNull V2DIFProofRequest proofRequest) {
        Map<String, PresentProofRequest.ProofRequest.ProofRequestedAttributes> requestedAttributes = proofRequest
                .getPresentationDefinition().getInputDescriptors().stream().map(id -> {
                    PresentProofRequest.ProofRequest.ProofRequestedAttributes ra = PresentProofRequest.ProofRequest.ProofRequestedAttributes
                            .builder()
                            .names(id.getConstraints().getFields().stream()
                                    .map(f -> f.getPath().stream()
                                            .map(path -> path.replace(PATH, ""))
                                            .collect(Collectors.toList()))
                                    .flatMap(Collection::stream)
                                    .collect(Collectors.toList()))
                            .restrictions(restrictionFromInputDescriptor(id))
                            // TODO predicates
                            .build();
                    return new AbstractMap.SimpleEntry<>(id.getId(), ra);
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return PresentProofRequest.ProofRequest
                .builder()
                .name(proofRequest.getPresentationDefinition() != null
                        ? proofRequest.getPresentationDefinition().getName()
                        : null)
                .requestedAttributes(requestedAttributes)
                .build();
    }

    private static List<JsonObject> restrictionFromInputDescriptor(
            @NonNull V2DIFProofRequest.PresentationDefinition.InputDescriptors id) {
        String schemaId = id.getSchema()
                .stream()
                .map(V2DIFProofRequest.PresentationDefinition.InputDescriptors.SchemaInputDescriptorUri::getUri)
                .filter(uri -> !StringUtils.contains(uri, "VerifiableCredential"))
                .findFirst()
                .orElse(null);
        if (id.getConstraints() == null || id.getConstraints().getFields() == null) {
            if (schemaId != null) {
                return List.of(PresentProofRequest.ProofRequest.ProofRestrictions.builder()
                        .schemaId(schemaId)
                        .build()
                        .toJsonObject());
            }
            return null;
        }
        PresentProofRequest.ProofRequest.ProofRestrictions.ProofRestrictionsBuilder b = PresentProofRequest.ProofRequest.ProofRestrictions
                .builder();
        id.getConstraints().getFields().forEach(f -> {
            String path = f.getPath().stream()
                    .map(p -> p.replace(PATH, ""))
                    .findFirst()
                    .orElse(null);
            if (path != null && f.getFilter() != null && f.getFilter().get_const() != null) {
                b.addAttributeValueRestriction(path, (String) f.getFilter().get_const());
            }
        });
        return List.of(b
                .schemaId(schemaId)
                .build()
                .toJsonObject());
    }
}
