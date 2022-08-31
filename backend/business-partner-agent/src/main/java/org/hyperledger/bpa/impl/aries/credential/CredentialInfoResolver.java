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
package org.hyperledger.bpa.impl.aries.credential;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.exception.AriesException;
import org.hyperledger.aries.api.jsonld.VerifiableCredential;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord;
import org.hyperledger.bpa.api.aries.AriesCredential;
import org.hyperledger.bpa.api.aries.AriesProofExchange;
import org.hyperledger.bpa.controller.api.proof.PresentationRequestCredentialsIndy;
import org.hyperledger.bpa.impl.aries.jsonld.LDContextHelper;
import org.hyperledger.bpa.impl.aries.schema.RestrictionsManager;
import org.hyperledger.bpa.impl.aries.schema.SchemaService;
import org.hyperledger.bpa.impl.util.AriesStringUtil;
import org.hyperledger.bpa.persistence.repository.HolderCredExRepository;

import java.io.IOException;

@Slf4j
@Singleton
public class CredentialInfoResolver {

    @Inject
    AriesClient ac;

    @Inject
    SchemaService schemaService;

    @Inject
    RestrictionsManager restrictionsManager;

    @Inject
    HolderCredExRepository holderCredExRepo;

    public AriesCredential.BPACredentialInfo populateCredentialInfo(
            @NonNull org.hyperledger.aries.api.present_proof.PresentationRequestCredentials.CredentialInfo ci) {

        AriesCredential.BPACredentialInfo.BPACredentialInfoBuilder builder = AriesCredential.BPACredentialInfo
                .builder();
        if (StringUtils.isNotEmpty(ci.getSchemaId())) {
            builder.schemaLabel(schemaService.getSchemaLabel(ci.getSchemaId()));
        }
        if (StringUtils.isNotEmpty(ci.getCredentialDefinitionId())) {
            builder.issuerLabel(generateIssuerLabel(ci.getCredentialDefinitionId()));
        }
        if (StringUtils.isNotEmpty(ci.getReferent())) {
            holderCredExRepo.findByReferent(ci.getReferent()).ifPresent(cred -> {
                builder.credentialId(cred.getId());
                builder.credentialLabel(cred.getLabel());
                if (cred.checkIfRevocable() && cred.checkIfNotRevoked()) {
                    try {
                        ac.credentialRevoked(ci.getReferent()).ifPresent(rev -> builder.revoked(rev.getRevoked()));
                    } catch (IOException | AriesException e) {
                        log.error("Could not check credential revocation status", e);
                    }
                } else {
                    builder.revoked(cred.getRevoked());
                }
            });
        }
        return builder.build();
    }

    public PresentationRequestCredentialsIndy.CredentialInfo populateCredentialInfo(
            @NonNull VerifiableCredential.VerifiableCredentialMatch matchingVC) {
        PresentationRequestCredentialsIndy.CredentialInfo.CredentialInfoBuilder builder = PresentationRequestCredentialsIndy.CredentialInfo
                .builder();

        builder.revoked(Boolean.FALSE);

        String schemaId = LDContextHelper.findSchemaId(matchingVC);
        if (StringUtils.isNotEmpty(schemaId)) {
            builder.schemaLabel(schemaService.getSchemaLabel(schemaId));
            builder.schemaId(schemaId);
        }
        if (StringUtils.isNotEmpty(matchingVC.getRecordId())) {
            holderCredExRepo.findByReferent(matchingVC.getRecordId()).ifPresent(cred -> {
                builder.credentialId(cred.getId());
                builder.credentialLabel(cred.getLabel());
                builder.attrs(cred.credentialAttributesToMap());
                builder.referent(matchingVC.getRecordId());
            });
        }
        if (StringUtils.isNotEmpty(matchingVC.getIssuer())) {
            builder.issuerLabel(generateIssuerLabel(matchingVC.getIssuer()));
        }

        return builder.build();
    }

    public AriesProofExchange.Identifier populateIdentifier(@NonNull PresentationExchangeRecord.Identifier identifier) {
        AriesProofExchange.Identifier.IdentifierBuilder builder = AriesProofExchange.Identifier.builder();
        if (StringUtils.isNotEmpty(identifier.getSchemaId())) {
            builder.schemaId(identifier.getSchemaId());
            builder.schemaLabel(schemaService.getSchemaLabel(identifier.getSchemaId()));
        }
        if (StringUtils.isNotEmpty(identifier.getCredentialDefinitionId())) {
            builder.credentialDefinitionId(identifier.getCredentialDefinitionId());
            builder.issuerLabel(generateIssuerLabel(identifier.getCredentialDefinitionId()));
        }
        return builder.build();
    }

    public AriesProofExchange.Identifier populateIdentifier(@NonNull String schemaId, @NonNull String issuerDid) {
        AriesProofExchange.Identifier.IdentifierBuilder builder = AriesProofExchange.Identifier.builder();

        builder.schemaId(schemaId);
        builder.schemaLabel(schemaService.getSchemaLabel(schemaId));

        String issuerLabel = generateIssuerLabel(issuerDid);
        builder.issuerLabel(issuerLabel == null ? issuerDid : issuerLabel);

        return builder.build();
    }

    private String generateIssuerLabel(@NonNull String expression) {
        String issuerLabel = restrictionsManager.findIssuerLabelByDid(expression);
        if (issuerLabel == null && AriesStringUtil.isCredDef(expression)) {
            issuerLabel = restrictionsManager
                    .prefixIssuerDid(AriesStringUtil.credDefIdGetDid(expression));
        }
        return issuerLabel;
    }
}
