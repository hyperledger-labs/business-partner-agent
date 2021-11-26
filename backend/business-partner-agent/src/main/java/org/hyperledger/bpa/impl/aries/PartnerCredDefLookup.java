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

import io.micronaut.context.annotation.Value;
import io.micronaut.scheduling.annotation.Async;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.*;
import org.hyperledger.bpa.api.PartnerAPI;
import org.hyperledger.bpa.client.LedgerExplorerClient;
import org.hyperledger.bpa.controller.api.partner.PartnerCredentialType;
import org.hyperledger.bpa.impl.util.AriesStringUtil;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.model.BPARestrictions;
import org.hyperledger.bpa.repository.BPARestrictionsRepository;
import org.hyperledger.bpa.repository.BPASchemaRepository;
import org.hyperledger.bpa.repository.PartnerRepository;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Singleton
public class PartnerCredDefLookup {

    @Value("${bpa.did.prefix}")
    @Setter(AccessLevel.PACKAGE)
    String didPrefix;

    @Inject
    @Setter(AccessLevel.PACKAGE)
    Optional<LedgerExplorerClient> ledger;

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    @Setter(AccessLevel.PACKAGE)
    BPASchemaRepository schemaRepo;

    @Inject
    @Setter(AccessLevel.PACKAGE)
    BPARestrictionsRepository restrictionsRepo;

    @Inject
    Converter conv;

    /**
     * Get/filter partners that can issue credentials that are based on the schema's
     * id.
     * 
     * @param schemaId the schema id
     * @return {@link PartnerAPI} list
     */
    public List<PartnerAPI> getIssuersFor(@NonNull String schemaId) {
        List<PartnerAPI> result = new ArrayList<>();
        ledger.ifPresentOrElse(
                l -> filterBySupportedCredential(schemaId, result),
                () -> filterByConfiguredCredentialDefs(schemaId, result));
        return result;
    }

    /**
     * If a ledger explorer is configured. Find partners that can issue credentials
     * that are based on the schema id.
     * 
     * @param schemaId the schema id
     * @param result   {@link List} that should contain the result
     */
    void filterBySupportedCredential(String schemaId, List<PartnerAPI> result) {
        schemaRepo.findBySchemaId(schemaId)
                .ifPresent(s -> partnerRepo.findBySupportedCredential(s.getSeqNo().toString()).forEach(
                        dbPartner -> result.add(conv.toAPIObject(dbPartner))));
    }

    /**
     * If NO ledger explorer is configured, statically match configured issuer did's
     * to partner did's to find partners that can issue a credential based on the
     * schema id.
     * 
     * @param schemaId the schema id
     * @param result   {@link List} that should contain the result
     */
    void filterByConfiguredCredentialDefs(@NonNull String schemaId, List<PartnerAPI> result) {
        schemaRepo.findBySchemaId(schemaId).ifPresent(s -> {
            List<String> did = restrictionsRepo.findBySchema(s)
                    .stream()
                    .map(BPARestrictions::getIssuerDid)
                    .collect(Collectors.toList());
            partnerRepo.findByDidIn(did).forEach(dbPartner -> result.add(conv.toAPIObject(dbPartner)));
        });
    }

    /**
     * If a BCGov ledger explorer is configured, looks up all credential definition
     * ids on the ledger that match a configured schema. If the did in the
     * credential definition id matches a partner's did, the partner is considered
     * an issuer of credentials that are based on that schema.
     */
    @Scheduled(cron = "0 15 2 ? * *")
    void lookupTypesForAllPartners() {
        ledger.ifPresent(l -> {
            Map<String, List<PartnerCredentialType>> didToTypes = new HashMap<>();
            StreamSupport.stream(schemaRepo.findAll().spliterator(), false).filter(s -> s.typeIsIndy()).forEach(
                    s -> l.queryCredentialDefinitions(s.getSeqNo().toString()).ifPresent(defs -> defs.forEach(def -> {
                        String did = AriesStringUtil.credDefIdGetDid(def.getCredentialDefinitionId());
                        if (didToTypes.containsKey(did)) {
                            didToTypes.get(did).add(def);
                        } else {
                            List<PartnerCredentialType> types = new ArrayList<>();
                            types.add(def);
                            didToTypes.put(did, types);
                        }
                    })));
            didToTypes.forEach(
                    (did, types) -> partnerRepo.updateByDid(didPrefix + did,
                            conv.toMap(new CredentialTypeWrapper(types))));
        });
    }

    @Async
    public void lookupTypesForAllPartnersAsync() {
        lookupTypesForAllPartners();
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static final class CredentialTypeWrapper {
        List<PartnerCredentialType> wrapped;
    }
}
