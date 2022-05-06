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
package org.hyperledger.bpa.persistence.model.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.core.convert.ConversionContext;
import io.micronaut.data.model.runtime.convert.AttributeConverter;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.api.present_proof.PresentProofRequest;
import org.hyperledger.aries.api.present_proof_v2.PresentationFormat;
import org.hyperledger.aries.api.present_proof_v2.V2DIFProofRequest;
import org.hyperledger.bpa.persistence.model.PartnerProof;

@Slf4j
@Singleton
@NoArgsConstructor
public class ProofRequestPayloadConverter implements AttributeConverter<PartnerProof.ProofRequestPayload, String> {

    public static final TypeReference<V2DIFProofRequest<V2DIFProofRequest.PresentationDefinition.InputDescriptors.SchemaInputDescriptorUriFilter>> DIF_TYPE = new TypeReference<>() {
    };

    @Inject
    ObjectMapper mapper;

    @Override
    public String convertToPersistedValue(PartnerProof.ProofRequestPayload pr, @NonNull ConversionContext context) {
        if (pr == null) {
            return null;
        }
        try {
            if (pr.typeIsDif()) {
                return mapper.writeValueAsString(pr.getDif());
            } else {
                return mapper.writeValueAsString(pr.getIndy());
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PartnerProof.ProofRequestPayload convertToEntityValue(String persistedValue,
            @NonNull ConversionContext context) {
        PartnerProof.ProofRequestPayload.ProofRequestPayloadBuilder b = PartnerProof.ProofRequestPayload.builder();
        if (persistedValue == null) {
            return null;
        }
        try {
            JsonNode node = mapper.readValue(persistedValue, JsonNode.class);
            if (node.has("presentation_definition")) {
                V2DIFProofRequest<V2DIFProofRequest.PresentationDefinition.InputDescriptors.SchemaInputDescriptorUriFilter> dif = mapper
                        .convertValue(node, DIF_TYPE);
                b.dif(dif);
                b.type(PresentationFormat.DIF);
            } else {
                PresentProofRequest.ProofRequest indy = mapper.convertValue(node,
                        PresentProofRequest.ProofRequest.class);
                b.indy(indy);
                b.type(PresentationFormat.INDY);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not deserialize credential exchange record");
        }
        return b.build();
    }
}
