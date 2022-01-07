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
package org.hyperledger.bpa.model.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.convert.ConversionContext;
import io.micronaut.data.model.runtime.convert.AttributeConverter;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecordByFormat;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.model.BPACredentialExchange;

@Slf4j
@Singleton
@NoArgsConstructor
public class ExchangePayloadConverter implements AttributeConverter<BPACredentialExchange.ExchangePayload, String> {

    @Inject
    ObjectMapper mapper;

    @Override
    public String convertToPersistedValue(BPACredentialExchange.ExchangePayload entityValue,
            @NonNull ConversionContext context) {
        if (entityValue == null) {
            return null;
        }
        try {
            if (entityValue.typeIsIndy()) {
                return mapper.writeValueAsString(entityValue.getIndy());
            } else if (entityValue.typeIsJsonLd()) {
                return mapper.writeValueAsString(entityValue.getLdProof());
            }
            return mapper.writeValueAsString(entityValue);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not serialise credential exchange record");
        }
    }

    @Override
    public BPACredentialExchange.ExchangePayload convertToEntityValue(String persistedValue,
            @NonNull ConversionContext context) {
        BPACredentialExchange.ExchangePayload.ExchangePayloadBuilder b = BPACredentialExchange.ExchangePayload
                .builder();
        if (persistedValue == null) {
            return null;
        }
        try {
            JsonNode node = mapper.readValue(persistedValue, JsonNode.class);
            if (node.has("attributes")) {
                V1CredentialExchange.CredentialProposalDict.CredentialProposal credentialProposal = mapper
                        .convertValue(node, V1CredentialExchange.CredentialProposalDict.CredentialProposal.class);
                b.indy(credentialProposal);
                b.type(CredentialType.INDY);
            } else if (node.has("credential")) {
                V20CredExRecordByFormat.LdProof ldProof = mapper.convertValue(node,
                        V20CredExRecordByFormat.LdProof.class);
                b.ldProof(ldProof);
                b.type(CredentialType.JSON_LD);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Could not deserialize credential exchange record");
        }
        return b.build();
    }
}