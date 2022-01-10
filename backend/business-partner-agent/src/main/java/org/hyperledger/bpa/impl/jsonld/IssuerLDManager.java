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
package org.hyperledger.bpa.impl.jsonld;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.ExchangeVersion;
import org.hyperledger.aries.api.credentials.Credential;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecord;
import org.hyperledger.aries.api.issue_credential_v2.V2CredentialSendRequest;
import org.hyperledger.aries.api.jsonld.VerifiableCredential;
import org.hyperledger.aries.config.GsonConfig;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.impl.activity.Identity;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.impl.util.TimeUtil;
import org.hyperledger.bpa.model.BPACredentialExchange;
import org.hyperledger.bpa.model.BPASchema;
import org.hyperledger.bpa.model.Partner;
import org.hyperledger.bpa.repository.BPASchemaRepository;
import org.hyperledger.bpa.repository.IssuerCredExRepository;
import org.hyperledger.bpa.repository.PartnerRepository;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Singleton
public class IssuerLDManager {

    @Inject
    AriesClient ac;

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    BPASchemaRepository schemaRepo;

    @Inject
    IssuerCredExRepository credExRepo;

    @Inject
    Identity identity;

    @Inject
    Converter conv;

    public void issueLDCredential(UUID partnerId, UUID bpaSchemaId, JsonNode document) {
        Partner partner = partnerRepo.findById(partnerId).orElseThrow();
        BPASchema bpaSchema = schemaRepo.findById(bpaSchemaId).orElseThrow();

        Map<String, String> cred = conv.toStringMap(document);
        try {
            V20CredExRecord exRecord = ac.issueCredentialV2Send(V2CredentialSendRequest
                    .builder()
                    .connectionId(partner.getConnectionId())
                    .filter(V2CredentialSendRequest.V20CredFilter
                            .builder()
                            .ldProof(V2CredentialSendRequest.LDProofVCDetail
                                    .builder()
                                    .credential(VerifiableCredential
                                            .builder()
                                            .context(List.of(CredentialType.JSON_LD.getContext().get(0),
                                                    bpaSchema.getSchemaId()))
                                            .credentialSubject(
                                                    GsonConfig.defaultConfig().toJsonTree(cred).getAsJsonObject())
                                            .issuanceDate(TimeUtil.toISOInstantTruncated(Instant.now()))
                                            .issuer(identity.getDidKey())
                                            .type(List.of(CredentialType.JSON_LD.getType().get(0),
                                                    bpaSchema.getLdType()))
                                            .build())
                                    .options(V2CredentialSendRequest.LDProofVCDetailOptions.builder()
                                            .proofType(V2CredentialSendRequest.ProofType.BbsBlsSignature2020)
                                            .build())
                                    .build())
                            .build())
                    .build())
                    .orElseThrow();

            BPACredentialExchange cex = BPACredentialExchange.builder()
                    .schema(bpaSchema)
                    .partner(partner)
                    .type(CredentialType.JSON_LD)
                    .role(CredentialExchangeRole.ISSUER)
                    .state(CredentialExchangeState.OFFER_SENT)
                    .pushStateChange(CredentialExchangeState.OFFER_SENT, Instant.now())
                    .indyCredential(Credential.builder()
                            .attrs(cred)
                            .build())
                    .credentialExchangeId(exRecord.getCredentialExchangeId())
                    .threadId(exRecord.getThreadId())
                    .exchangeVersion(ExchangeVersion.V2)
                    .build();
            credExRepo.save(cex);
        } catch (IOException e) {
            log.error("aca-py not offline");
        }
    }
}