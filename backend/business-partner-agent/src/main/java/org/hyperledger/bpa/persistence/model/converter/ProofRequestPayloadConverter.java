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
import io.micronaut.core.convert.ConversionContext;
import jakarta.inject.Singleton;
import lombok.NonNull;
import org.hyperledger.aries.api.present_proof.PresentProofRequest;
import org.hyperledger.aries.api.present_proof_v2.V2DIFProofRequest;
import org.hyperledger.bpa.api.CredentialType;

@Singleton
public class ProofRequestPayloadConverter extends
        BasePayloadConverter<PresentProofRequest.ProofRequest, V2DIFProofRequest<V2DIFProofRequest.PresentationDefinition.InputDescriptors.SchemaInputDescriptorUriFilter>> {

    public static final TypeReference<V2DIFProofRequest<V2DIFProofRequest.PresentationDefinition.InputDescriptors.SchemaInputDescriptorUriFilter>> DIF_TYPE = new TypeReference<>() {
    };

    @Override
    public ExchangePayload<PresentProofRequest.ProofRequest, V2DIFProofRequest<V2DIFProofRequest.PresentationDefinition.InputDescriptors.SchemaInputDescriptorUriFilter>> convertToEntityValue(
            String persistedValue, @NonNull ConversionContext context) {
        if (persistedValue == null) {
            return null;
        }
        ExchangePayload.ExchangePayloadBuilder<PresentProofRequest.ProofRequest, V2DIFProofRequest<V2DIFProofRequest.PresentationDefinition.InputDescriptors.SchemaInputDescriptorUriFilter>> b = ExchangePayload
                .builder();
        try {
            JsonNode node = mapper.readValue(persistedValue, JsonNode.class);
            if (node.has("presentationDefinition")) {
                V2DIFProofRequest<V2DIFProofRequest.PresentationDefinition.InputDescriptors.SchemaInputDescriptorUriFilter> dif = mapper
                        .convertValue(node, DIF_TYPE);
                b.ldProof(dif);
                b.type(CredentialType.JSON_LD);
            } else {
                PresentProofRequest.ProofRequest indy = mapper.convertValue(node,
                        PresentProofRequest.ProofRequest.class);
                b.indy(indy);
                b.type(CredentialType.INDY);
            }
        } catch (JsonProcessingException e) {
            throw new ConversionException("Could not deserialize proof exchange record");
        }
        return b.build();
    }
}
