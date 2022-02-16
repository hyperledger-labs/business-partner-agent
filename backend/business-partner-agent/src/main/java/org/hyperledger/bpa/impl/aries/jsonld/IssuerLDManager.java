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
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.ExchangeVersion;
import org.hyperledger.aries.api.credentials.CredentialAttributes;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeRole;
import org.hyperledger.aries.api.issue_credential_v1.CredentialExchangeState;
import org.hyperledger.aries.api.issue_credential_v2.V20CredBoundOfferRequest;
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecord;
import org.hyperledger.aries.api.issue_credential_v2.V2CredentialExchangeFree;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.api.exception.EntityNotFoundException;
import org.hyperledger.bpa.api.exception.NetworkException;
import org.hyperledger.bpa.api.exception.WrongApiUsageException;
import org.hyperledger.bpa.config.BPAMessageSource;
import org.hyperledger.bpa.controller.api.issuer.CredEx;
import org.hyperledger.bpa.impl.aries.credential.IssuerManager;
import org.hyperledger.bpa.persistence.model.BPACredentialExchange;
import org.hyperledger.bpa.persistence.model.BPASchema;
import org.hyperledger.bpa.persistence.model.Partner;
import org.hyperledger.bpa.persistence.repository.BPASchemaRepository;
import org.hyperledger.bpa.persistence.repository.IssuerCredExRepository;
import org.hyperledger.bpa.persistence.repository.PartnerRepository;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Handles all credential holder logic that is specific to json-ld
 */
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
    LDContextHelper vcHelper;

    @Inject
    BPAMessageSource.DefaultMessageSource msg;

    public BPACredentialExchange issueLDCredential(UUID partnerId, UUID bpaSchemaId, JsonNode document) {
        Partner partner = partnerRepo.findById(partnerId).orElseThrow(EntityNotFoundException::new);
        BPASchema bpaSchema = schemaRepo.findById(bpaSchemaId).orElseThrow(EntityNotFoundException::new);
        try {
            V20CredExRecord exRecord = ac.issueCredentialV2Send(V2CredentialExchangeFree.builder()
                    .connectionId(UUID.fromString(Objects.requireNonNull(partner.getConnectionId())))
                    .filter(vcHelper.buildVC(bpaSchema, document))
                    .build())
                    .orElseThrow();

            BPACredentialExchange cex = BPACredentialExchange.builder()
                    .schema(bpaSchema)
                    .partner(partner)
                    .type(CredentialType.JSON_LD)
                    .role(CredentialExchangeRole.ISSUER)
                    .state(CredentialExchangeState.OFFER_SENT)
                    // same behaviour as indy, in this case proposal == offer == credential
                    .pushStateChange(CredentialExchangeState.OFFER_SENT, Instant.now())
                    .ldCredential(BPACredentialExchange.ExchangePayload.jsonLD(exRecord.resolveLDCredOffer()))
                    .credentialExchangeId(exRecord.getCredentialExchangeId())
                    .threadId(exRecord.getThreadId())
                    .exchangeVersion(ExchangeVersion.V2)
                    .build();
            return credExRepo.save(cex);
        } catch (IOException e) {
            log.error("aca-py is offline");
            throw new NetworkException(msg.getMessage("acapy.unavailable"), e);
        }
    }

    public void reIssueLDCredential() {
        throw new WrongApiUsageException(msg.getMessage("api.issuer.credential.send.not.supported"));
    }

    public CredEx revokeLDCredential() {
        throw new WrongApiUsageException(msg.getMessage("api.issuer.credential.send.not.supported"));
    }

    public CredEx sendOffer(@NonNull BPACredentialExchange credEx, @NotNull Map<String, String> attributes,
            @NonNull IssuerManager.IdWrapper ids) throws IOException {
        String schemaId = credEx.getSchema() != null ? credEx.getSchema().getSchemaId() : null;
        if (StringUtils.isNotEmpty(schemaId) && !StringUtils.equals(schemaId, ids.schemaId())) {
            BPASchema counterSchema = schemaRepo.findBySchemaId(ids.schemaId()).orElseThrow(
                    () -> new WrongApiUsageException(msg.getMessage("api.issuer.credential.send.offer.wrong.schema")));
            credEx.setSchema(counterSchema);
            credExRepo.update(credEx);
        }
        V2CredentialExchangeFree.V20CredFilter v20CredFilter = vcHelper.buildVC(credEx.getSchema(), attributes);
        V20CredExRecord v20CredExRecord = ac.issueCredentialV2RecordsSendOffer(credEx.getCredentialExchangeId(),
                V20CredBoundOfferRequest.builder()
                        .filter(v20CredFilter)
                        .counterPreview(V2CredentialExchangeFree.V2CredentialPreview.builder()
                                .attributes(CredentialAttributes.fromMap(attributes))
                                .build())
                        .build())
                .orElseThrow();
        credExRepo.updateCredential(credEx.getId(),
                BPACredentialExchange.ExchangePayload.jsonLD(v20CredExRecord.resolveLDCredOffer()));
        return CredEx.from(credEx);
    }

    public void handleCredentialProposal(@NonNull String schemaId,
            @NonNull BPACredentialExchange.BPACredentialExchangeBuilder b) {
        schemaRepo.findBySchemaId(schemaId).ifPresentOrElse(s -> {
            b.schema(s);
            b.type(CredentialType.JSON_LD);
            credExRepo.save(b.build());
        }, () -> {
            b.errorMsg(msg.getMessage("api.holder.issuer.has.no.creddef",
                    Map.of("id", schemaId)));
            credExRepo.save(b.build());
        });
    }
}