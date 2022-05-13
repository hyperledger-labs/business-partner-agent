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
import org.hyperledger.aries.api.jsonld.VerifiableCredential;
import org.hyperledger.aries.api.jsonld.VerifiablePresentation;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord;
import org.hyperledger.bpa.api.CredentialType;

import java.util.Map;

@Singleton
public class ProofPayloadConverter extends
        BasePayloadConverter<Map<String, PresentationExchangeRecord.RevealedAttributeGroup>, VerifiablePresentation<VerifiableCredential>> {

    public static final TypeReference<Map<String, PresentationExchangeRecord.RevealedAttributeGroup>> INDY_TYPE = new TypeReference<>() {
    };

    public static final TypeReference<VerifiablePresentation<VerifiableCredential>> LD_TYPE = new TypeReference<>() {
    };

    @Override
    public ExchangePayload<Map<String, PresentationExchangeRecord.RevealedAttributeGroup>, VerifiablePresentation<VerifiableCredential>> convertToEntityValue(
            String persistedValue, @NonNull ConversionContext context) {
        if (persistedValue == null) {
            return null;
        }
        ExchangePayload.ExchangePayloadBuilder<Map<String, PresentationExchangeRecord.RevealedAttributeGroup>, VerifiablePresentation<VerifiableCredential>> b = ExchangePayload
                .builder();
        try {
            JsonNode node = mapper.readValue(persistedValue, JsonNode.class);
            if (node.has("verifiableCredential")) {
                VerifiablePresentation<VerifiableCredential> ld = mapper.convertValue(node, LD_TYPE);
                b.ldProof(ld);
                b.type(CredentialType.JSON_LD);
            } else {
                Map<String, PresentationExchangeRecord.RevealedAttributeGroup> indy = mapper.convertValue(node,
                        INDY_TYPE);
                b.indy(indy);
                b.type(CredentialType.INDY);
            }
        } catch (JsonProcessingException e) {
            throw new ConversionException("Could not deserialize proof exchange record");
        }
        return b.build();
    }
}
