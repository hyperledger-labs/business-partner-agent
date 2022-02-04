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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.hyperledger.aries.api.issue_credential_v1.BaseCredExRecord;
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecord;
import org.hyperledger.aries.api.issue_credential_v2.V20CredExRecordByFormat;
import org.hyperledger.bpa.api.CredentialType;
import org.hyperledger.bpa.api.aries.SchemaAPI;
import org.hyperledger.bpa.impl.aries.credential.BaseHolderManager;
import org.hyperledger.bpa.impl.activity.LabelStrategy;
import org.hyperledger.bpa.impl.aries.schema.SchemaService;
import org.hyperledger.bpa.persistence.model.BPACredentialExchange;
import org.hyperledger.bpa.persistence.model.BPASchema;
import org.hyperledger.bpa.persistence.repository.BPASchemaRepository;
import org.hyperledger.bpa.persistence.repository.HolderCredExRepository;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

@Singleton
public class HolderLDManager extends BaseHolderManager {

    @Inject
    HolderCredExRepository credExRepo;

    @Inject
    BPASchemaRepository schemaRepo;

    @Inject
    HolderCredExRepository holderCredExRepo;

    @Inject
    SchemaService schemaService;

    @Inject
    LabelStrategy labelStrategy;

    @NotNull
    @Override
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

    public void handleCredentialReceived(V20CredExRecord v2) {
        holderCredExRepo.findByCredentialExchangeId(v2.getCredentialExchangeId()).ifPresent(dbCred -> {
            String label = labelStrategy.apply(dbCred.getLdCredential());
            dbCred
                    .pushStates(v2.getState(), v2.getUpdatedAt())
                    .setLdCredential(BPACredentialExchange.ExchangePayload.jsonLD(v2.resolveLDCredOffer()))
                    .setIssuer(resolveIssuer(dbCred.getPartner()))
                    .setLabel(label);
            BPACredentialExchange dbCredential = holderCredExRepo.update(dbCred);
            fireCredentialAddedEvent(dbCredential);
        });
    }
}