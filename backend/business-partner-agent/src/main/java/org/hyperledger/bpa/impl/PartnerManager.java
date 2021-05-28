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
package org.hyperledger.bpa.impl;

import io.micronaut.cache.annotation.CacheInvalidate;
import io.micronaut.context.annotation.Value;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.bpa.api.PartnerAPI;
import org.hyperledger.bpa.api.exception.EntityNotFoundException;
import org.hyperledger.bpa.api.exception.PartnerException;
import org.hyperledger.bpa.core.RegisteredWebhook.WebhookEventType;
import org.hyperledger.bpa.impl.activity.PartnerLookup;
import org.hyperledger.bpa.impl.aries.ConnectionManager;
import org.hyperledger.bpa.impl.aries.PartnerCredDefLookup;
import org.hyperledger.bpa.impl.util.Converter;
import org.hyperledger.bpa.model.Partner;
import org.hyperledger.bpa.repository.MyCredentialRepository;
import org.hyperledger.bpa.repository.PartnerRepository;

import io.micronaut.core.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class PartnerManager {

    @Value("${bpa.did.prefix}")
    String ledgerPrefix;

    @Inject
    PartnerRepository repo;

    @Inject
    Converter converter;

    @Inject
    ConnectionManager cm;

    @Inject
    PartnerCredDefLookup credLookup;

    @Inject
    PartnerLookup partnerLookup;

    @Inject
    MyCredentialRepository myCredRepo;

    @Inject
    WebhookService webhook;

    public List<PartnerAPI> getPartners() {
        List<PartnerAPI> result = new ArrayList<>();
        repo.findAll().forEach(dbPartner -> result.add(converter.toAPIObject(dbPartner)));
        return result;
    }

    public Optional<PartnerAPI> getPartnerById(@NonNull UUID id) {
        Optional<PartnerAPI> result = Optional.empty();
        Optional<Partner> dbPartner = repo.findById(id);
        if (dbPartner.isPresent()) {
            result = Optional.of(converter.toAPIObject(dbPartner.get()));
        }
        return result;
    }

    public void removePartnerById(@NonNull UUID id) {
        repo.findById(id).ifPresent(p -> {
            if (p.getConnectionId() != null) {
                // Iterable<BPAPresentationExchange> peList = peRepo.findByPartnerId(id);
                // if (peList.iterator().hasNext()) {
                // peRepo.deleteAll(peList);
                // }
                cm.removeConnection(p.getConnectionId());
            }
        });
        repo.deleteById(id);
    }

    public PartnerAPI addPartnerFlow(@NonNull String did, @Nullable String alias) {
        Optional<Partner> dbPartner = repo.findByDid(did);
        if (dbPartner.isPresent()) {
            throw new PartnerException("Partner for did already exists: " + did);
        }
        PartnerAPI lookupP = partnerLookup.lookupPartner(did);

        String connectionLabel = UUID.randomUUID().toString();
        Partner partner = converter.toModelObject(did, lookupP)
                .setLabel(connectionLabel)
                .setAriesSupport(lookupP.getAriesSupport())
                .setAlias(alias)
                .setState(ConnectionState.REQUEST);
        Partner result = repo.save(partner); // save before creating the connection
        if (did.startsWith(ledgerPrefix) && lookupP.getAriesSupport()) {
            cm.createConnection(did, connectionLabel, alias);
            credLookup.lookupTypesForAllPartnersAsync();
        } else if (lookupP.getAriesSupport()) {
            cm.createConnection(lookupP.getDidDocAPI(), connectionLabel, alias);
        }
        final PartnerAPI apiPartner = converter.toAPIObject(result);
        webhook.convertAndSend(WebhookEventType.PARTNER_ADD, apiPartner);
        return apiPartner;
    }

    public Optional<PartnerAPI> updatePartner(@NonNull UUID id, @Nullable String alias) {
        Optional<PartnerAPI> result = Optional.empty();
        int count = repo.updateAlias(id, alias);
        if (count > 0) {
            final Optional<Partner> dbP = repo.findById(id);
            if (dbP.isPresent()) {
                result = Optional.of(converter.toAPIObject(dbP.get()));
                if (StringUtils.isNotBlank(alias)) {
                    myCredRepo.updateByConnectionId(dbP.get().getConnectionId(), dbP.get().getConnectionId(), alias);
                }
            }
        }
        return result;
    }

    public Optional<PartnerAPI> updatePartnerDid(@NonNull UUID id, @NonNull String did) {
        Optional<PartnerAPI> result = Optional.empty();
        int count = repo.updateDid(id, did);
        if (count > 0) {
            final Optional<Partner> dbP = repo.findById(id);
            if (dbP.isPresent()) {
                result = Optional.of(converter.toAPIObject(dbP.get()));
            }
        }
        return result;
    }

    /**
     * Same as add partner, with the difference that refresh only works on existing
     * partners
     *
     * @param id the id
     * @return {@link PartnerAPI}
     */
    public Optional<PartnerAPI> refreshPartner(@NonNull UUID id) {
        Optional<PartnerAPI> result = Optional.empty();
        final Optional<Partner> dbPartner = repo.findById(id);
        if (dbPartner.isPresent()) {
            Partner dbP = dbPartner.get();
            invalidatePartnerLookupCache();
            PartnerAPI pAPI = partnerLookup.lookupPartner(dbP.getDid());
            dbP.setValid(pAPI.getValid());
            dbP.setVerifiablePresentation(pAPI.getVerifiablePresentation() != null
                    ? converter.toMap(pAPI.getVerifiablePresentation())
                    : null);
            dbP = repo.update(dbP);
            result = Optional.of(converter.toAPIObject(dbP));
            webhook.convertAndSend(WebhookEventType.PARTNER_UPDATE, result.get());
        }
        return result;
    }

    // TODO use partner UUID as cache key
    @CacheInvalidate(cacheNames = { "partner-lookup-cache" }, all = true)
    public void invalidatePartnerLookupCache() {
        //
    }

    public void acceptPartner(@NonNull UUID partnerId) {
        String connectionId = repo.findById(partnerId)
                .map(Partner::getConnectionId)
                .orElseThrow(EntityNotFoundException::new);
        cm.acceptConnection(connectionId);
    }

}
