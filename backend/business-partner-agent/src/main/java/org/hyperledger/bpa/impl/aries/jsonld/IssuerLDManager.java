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
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.ExchangeVersion;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecord;
import org.hyperledger.aries.api.issue_credential_v2.V2CredentialExchangeFree;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.api.exception.EntityNotFoundException;
import org.hyperledger.bpa.impl.aries.credential.BaseIssuerManager;
import org.hyperledger.bpa.persistence.model.BPACredentialExchange;
import org.hyperledger.bpa.persistence.model.BPASchema;
import org.hyperledger.bpa.persistence.model.Partner;
import org.hyperledger.bpa.persistence.repository.BPASchemaRepository;
import org.hyperledger.bpa.persistence.repository.IssuerCredExRepository;
import org.hyperledger.bpa.persistence.repository.PartnerRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Singleton
public class IssuerLDManager extends BaseIssuerManager {

    @Inject
    AriesClient ac;

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    BPASchemaRepository schemaRepo;

    @Inject
    IssuerCredExRepository credExRepo;

    @Inject
    LDContextHelper vcHelper;

    public String issueLDCredential(UUID partnerId, UUID bpaSchemaId, JsonNode document) {
        String credentialExchangeId = null;
        Partner partner = partnerRepo.findById(partnerId).orElseThrow(EntityNotFoundException::new);
        BPASchema bpaSchema = schemaRepo.findById(bpaSchemaId).orElseThrow(EntityNotFoundException::new);
        try {
            V20CredExRecord exRecord = ac.issueCredentialV2Send(V2CredentialExchangeFree.builder()
                    .connectionId(UUID.fromString(Objects.requireNonNull(partner.getConnectionId())))
                    .filter(vcHelper.buildVC(bpaSchema, document, Boolean.TRUE))
                    .build())
                    .orElseThrow();

            BPACredentialExchange cex = BPACredentialExchange.builder()
                    .schema(bpaSchema)
                    .partner(partner)
                    .type(CredentialType.JSON_LD)
                    .role(CredentialExchangeRole.ISSUER)
                    .state(CredentialExchangeState.OFFER_SENT)
                    .pushStateChange(CredentialExchangeState.OFFER_SENT, Instant.now())
                    .credentialOffer(BPACredentialExchange.ExchangePayload.jsonLD(exRecord.resolveLDCredOffer()))
                    .credentialExchangeId(exRecord.getCredentialExchangeId())
                    .threadId(exRecord.getThreadId())
                    .exchangeVersion(ExchangeVersion.V2)
                    .build();
            credExRepo.save(cex);
            credentialExchangeId = exRecord.getCredentialExchangeId();
        } catch (IOException e) {
            log.error("aca-py is offline");
        }
        return credentialExchangeId;
    }
}