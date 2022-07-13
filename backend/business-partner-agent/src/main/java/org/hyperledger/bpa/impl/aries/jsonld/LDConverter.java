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
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.CollectionUtils;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.acy_py.generated.model.IndyProofReqPredSpec;
import org.hyperledger.aries.api.present_proof.PresentProofRequest;
import org.hyperledger.aries.api.present_proof_v2.DIFField;
import org.hyperledger.aries.api.present_proof_v2.V2DIFProofRequest;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class LDConverter {

    private static final String PATH = "$.credentialSubject.";

    public static PresentProofRequest.ProofRequest difToIndyProofRequest(@NonNull V2DIFProofRequest proofRequest) {
        Map<String, PresentProofRequest.ProofRequest.ProofRequestedAttributes> requestedAttributes = new HashMap<>();
        Map<String, PresentProofRequest.ProofRequest.ProofRequestedPredicates> requestedPredicates = new HashMap<>();

        List<V2DIFProofRequest.PresentationDefinition.InputDescriptors> inputDescriptors = proofRequest
                .getPresentationDefinition() != null
                        ? proofRequest.getPresentationDefinition().getInputDescriptors()
                        : null;

        if (inputDescriptors != null) {
            requestedAttributes = requestedAttributesFromInputDescriptor(inputDescriptors);
            requestedPredicates = requestedPredicatesFromInputDescriptor(inputDescriptors);
        }

        return PresentProofRequest.ProofRequest
                .builder()
                .name(proofRequest.getPresentationDefinition() != null
                        ? proofRequest.getPresentationDefinition().getName()
                        : null)
                .requestedAttributes(requestedAttributes)
                .requestedPredicates(requestedPredicates)
                .build();
    }

    private static Map<String, PresentProofRequest.ProofRequest.ProofRequestedPredicates> requestedPredicatesFromInputDescriptor(
            @NonNull List<V2DIFProofRequest.PresentationDefinition.InputDescriptors> ids) {
        Map<String, PresentProofRequest.ProofRequest.ProofRequestedPredicates> result = new LinkedHashMap<>();

        List<V2DIFProofRequest.PresentationDefinition.InputDescriptors> idsWithFields = ids.stream()
                .filter(id -> id.getConstraints() != null
                        && id.getConstraints().getFields() != null)
                .toList();

        Counter c = new Counter(new AtomicInteger(0));

        idsWithFields.forEach(id -> {
            String schemaId = getSchemaIdFromInputDescriptor(id);
            id.getConstraints().getFields().forEach(f -> {
                DIFField.Filter filter = f.getFilter();
                if (filter != null) {
                    TypeToValue typeToValue;
                    if (filter.getMinimum() != null) {
                        typeToValue = new TypeToValue(IndyProofReqPredSpec.PTypeEnum.LESS_THAN, filter.getMinimum());
                    } else if (filter.getMaximum() != null) {
                        typeToValue = new TypeToValue(IndyProofReqPredSpec.PTypeEnum.GREATER_THAN_OR_EQUAL_TO,
                                filter.getMaximum());
                    } else if (filter.getExclusiveMaximum() != null) {
                        typeToValue = new TypeToValue(IndyProofReqPredSpec.PTypeEnum.LESS_THAN,
                                filter.getExclusiveMaximum());
                    } else if (filter.getExclusiveMinimum() != null) {
                        typeToValue = new TypeToValue(IndyProofReqPredSpec.PTypeEnum.GREATER_THAN,
                                filter.getExclusiveMinimum());
                    } else {
                        typeToValue = new TypeToValue(null, null);
                    }
                    if (typeToValue.type != null && typeToValue.value != null) {
                        List<String> paths = f.getPath().stream()
                                .map(path -> path.replace(PATH, ""))
                                .collect(Collectors.toList());
                        if (CollectionUtils.isNotEmpty(paths)) {
                            paths.forEach(p -> {
                                PresentProofRequest.ProofRequest.ProofRequestedPredicates
                                        .ProofRequestedPredicatesBuilder rp = PresentProofRequest.ProofRequest.ProofRequestedPredicates
                                        .builder();
                                rp.name(p);
                                rp.pType(typeToValue.type);
                                rp.pValue(typeToValue.value.intValue());
                                rp.restriction(PresentProofRequest.ProofRequest.ProofRestrictions.builder()
                                        .schemaId(schemaId).build().toJsonObject());
                                result.put(id.getId() + "-" + c.counter().incrementAndGet(), rp.build());
                            });
                        }
                    }
                }
            });
        });
        return result;
    }

    private static Map<String, PresentProofRequest.ProofRequest.ProofRequestedAttributes> requestedAttributesFromInputDescriptor(
            @NonNull List<V2DIFProofRequest.PresentationDefinition.InputDescriptors> ids) {
        return ids.stream()
                .filter(id -> id.getConstraints() != null && id.getConstraints().getFields() != null)
                .map(id -> {
                    List<String> names = id.getConstraints().getFields().stream()
                            .filter(f -> f.getFilter() == null || f.getFilter().get_const() != null)
                            .map(f -> f.getPath().stream()
                                    .map(path -> path.replace(PATH, ""))
                                    .collect(Collectors.toList()))
                            .flatMap(Collection::stream)
                            .collect(Collectors.toList());
                    if (CollectionUtils.isNotEmpty(names)) {
                        PresentProofRequest.ProofRequest.ProofRequestedAttributes ra = PresentProofRequest.ProofRequest.ProofRequestedAttributes
                                .builder()
                                .names(names)
                                .restrictions(restrictionFromInputDescriptor(id))
                                .build();
                        return new AbstractMap.SimpleEntry<>(id.getId(), ra);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static List<JsonObject> restrictionFromInputDescriptor(
            @NonNull V2DIFProofRequest.PresentationDefinition.InputDescriptors id) {
        String schemaId = getSchemaIdFromInputDescriptor(id);
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

    @Nullable
    private static String getSchemaIdFromInputDescriptor(
            @NonNull V2DIFProofRequest.PresentationDefinition.InputDescriptors id) {
        return id.getSchema()
                .stream()
                .map(V2DIFProofRequest.PresentationDefinition.InputDescriptors.SchemaInputDescriptorUri::getUri)
                .filter(uri -> !StringUtils.contains(uri, "VerifiableCredential"))
                .findFirst()
                .orElse(null);
    }

    record Counter(AtomicInteger counter) {
    }

    record TypeToValue(IndyProofReqPredSpec.PTypeEnum type, Number value) {
    }
}
