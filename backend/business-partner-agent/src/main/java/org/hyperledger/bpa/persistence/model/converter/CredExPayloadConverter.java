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
import com.fasterxml.jackson.databind.JsonNode;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.convert.ConversionContext;
import jakarta.inject.Singleton;
import org.hyperledger.aries.api.issue_credential_v1.V1CredentialExchange;
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecordByFormat;
import org.hyperledger.bpa.api.CredentialType;

@Singleton
public class CredExPayloadConverter extends
        BasePayloadConverter<V1CredentialExchange.CredentialProposalDict.CredentialProposal, V20CredExRecordByFormat.LdProof> {

    @Override
    public ExchangePayload<V1CredentialExchange.CredentialProposalDict.CredentialProposal, V20CredExRecordByFormat.LdProof> convertToEntityValue(
            String persistedValue, @NonNull ConversionContext context) {
        ExchangePayload.ExchangePayloadBuilder<V1CredentialExchange.CredentialProposalDict.CredentialProposal, V20CredExRecordByFormat.LdProof> b = ExchangePayload
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
                b.jsonLD(ldProof);
                b.type(CredentialType.JSON_LD);
            }
        } catch (JsonProcessingException e) {
            throw new ConversionException("Could not deserialize credential exchange record");
        }
        return b.build();
    }
}