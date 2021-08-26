/*
 * Copyright (c) 2020-2021 - for information on the respective copyright owner
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
package org.hyperledger.bpa.impl.aries;

import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.aries.api.present_proof.PresentationExchangeRecord;
import org.hyperledger.bpa.api.aries.AriesCredential;
import org.hyperledger.bpa.api.aries.AriesProofExchange;
import org.hyperledger.bpa.impl.aries.config.RestrictionsManager;
import org.hyperledger.bpa.impl.aries.config.SchemaService;
import org.hyperledger.bpa.repository.MyCredentialRepository;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class CredentialInfoResolver {

    @Inject
    SchemaService schemaService;

    @Inject
    RestrictionsManager restrictionsManager;

    @Inject
    MyCredentialRepository credentialRepository;

    public AriesCredential.BPACredentialInfo populateCredentialInfo(
            @NonNull org.hyperledger.aries.api.present_proof.PresentationRequestCredentials.CredentialInfo ci) {

        AriesCredential.BPACredentialInfo.BPACredentialInfoBuilder builder = AriesCredential.BPACredentialInfo
                .builder();
        if (StringUtils.isNotEmpty(ci.getSchemaId())) {
            builder.schemaLabel(schemaService.getSchemaLabel(ci.getSchemaId()));
        }
        if (StringUtils.isNotEmpty(ci.getCredentialDefinitionId())) {
            builder.issuerLabel(restrictionsManager.findIssuerLabelByDid(ci.getCredentialDefinitionId()));
        }
        if (StringUtils.isNotEmpty(ci.getReferent())) {
            credentialRepository.findByReferent(ci.getReferent()).ifPresent(cred -> {
                builder.credentialId(cred.getId());
                builder.credentialLabel(cred.getLabel());
            });
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
            builder.issuerLabel(restrictionsManager.findIssuerLabelByDid(identifier.getCredentialDefinitionId()));
        }
        return builder.build();
    }
}
