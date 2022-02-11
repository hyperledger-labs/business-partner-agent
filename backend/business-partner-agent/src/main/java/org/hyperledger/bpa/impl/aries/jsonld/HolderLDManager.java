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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.Setter;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.ExchangeVersion;
import org.hyperledger.aries.api.issue_credential_v1.BaseCredExRecord;
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecord;
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecordByFormat;
import org.hyperledger.aries.api.issue_credential_v2.V2CredentialExchangeFree;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.api.aries.SchemaAPI;
import org.hyperledger.bpa.impl.activity.LabelStrategy;
import org.hyperledger.bpa.impl.aries.schema.SchemaService;
import org.hyperledger.bpa.persistence.model.BPACredentialExchange;
import org.hyperledger.bpa.persistence.model.BPASchema;
import org.hyperledger.bpa.persistence.repository.BPASchemaRepository;
import org.hyperledger.bpa.persistence.repository.HolderCredExRepository;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles all credential holder logic that is specific to json-ld
 */
@Singleton
public class HolderLDManager {

    @Inject
    @Setter(AccessLevel.PROTECTED)
    AriesClient ac;

    @Inject
    BPASchemaRepository schemaRepo;

    @Inject
    HolderCredExRepository holderCredExRepo;

    @Inject
    SchemaService schemaService;

    @Inject
    LabelStrategy labelStrategy;

    @Inject
    LDContextHelper ldHelper;

    @Inject
    ObjectMapper mapper;

    public void sendCredentialProposal(
            @NonNull String connectionId,
            @NonNull BPASchema s,
            @NonNull Map<String, Object> document,
            @NonNull BPACredentialExchange.BPACredentialExchangeBuilder dbCredEx)
            throws IOException {
        JsonNode jsonNode = mapper.valueToTree(document);
        V2CredentialExchangeFree v2Request = V2CredentialExchangeFree.builder()
                .connectionId(UUID.fromString(connectionId))
                .filter(ldHelper.buildVC(s, jsonNode, Boolean.FALSE))
                .build();
        ac.issueCredentialV2SendProposal(v2Request).ifPresent(v2 -> dbCredEx
                .threadId(v2.getThreadId())
                .credentialExchangeId(v2.getCredentialExchangeId())
                .exchangeVersion(ExchangeVersion.V2)
                .credentialProposal(BPACredentialExchange.ExchangePayload.jsonLD(v2.resolveLDCredProposal())));
    }

    public BPASchema checkSchema(BaseCredExRecord credExBase) {
        BPASchema schema = null;
        if (credExBase instanceof V20CredExRecord) {
            V20CredExRecordByFormat.LdProof offer = ((V20CredExRecord) credExBase).resolveLDCredOffer();
            List<String> type = offer.getCredential().getType();
            type.removeAll(CredentialType.JSON_LD.getType());

            String schemaId = LDContextHelper.findSchemaId(offer);
            Optional<BPASchema> bpaSchema = schemaRepo.findBySchemaId(schemaId);
            if (bpaSchema.isPresent()) {
                schema = bpaSchema.get();
            } else {
                SchemaAPI schemaApi = schemaService.addJsonLDSchema(schemaId, null, null, type.get(0),
                        offer.getCredential().getCredentialSubject().keySet());
                schema = schemaRepo.findById(schemaApi.getId()).orElseThrow();
            }
        }
        return schema;
    }

    public void handleV2CredentialReceived(@NonNull V20CredExRecord v2, @NonNull BPACredentialExchange dbCred,
            String issuer) {
        String label = labelStrategy.apply(dbCred.getLdCredential());
        dbCred
                .pushStates(v2.getState(), v2.getUpdatedAt())
                .setLdCredential(BPACredentialExchange.ExchangePayload.jsonLD(v2.resolveLDCredOffer()))
                .setIssuer(issuer)
                .setLabel(label);
        holderCredExRepo.update(dbCred);
    }
}