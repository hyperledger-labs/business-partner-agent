/**
 * Copyright (c) 2020 - for information on the respective copyright owner
 * see the NOTICE file and/or the repository at
 * https://github.com/hyperledger-labs/organizational-agent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hyperledger.oa.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.hyperledger.aries.api.ledger.EndpointType;
import org.hyperledger.oa.api.DidDocAPI;
import org.hyperledger.oa.api.DidDocAPI.Service;
import org.hyperledger.oa.api.PartnerAPI;
import org.hyperledger.oa.api.exception.PartnerException;
import org.hyperledger.oa.client.URClient;
import org.hyperledger.oa.impl.aries.ConnectionManager;
import org.hyperledger.oa.impl.util.Converter;
import org.hyperledger.oa.impl.web.WebPartnerFlow;
import org.hyperledger.oa.model.Partner;
import org.hyperledger.oa.repository.PartnerRepository;

import io.micronaut.cache.annotation.Cacheable;
import lombok.NonNull;

@Singleton
public class PartnerManager {

    @Inject
    private PartnerRepository repo;

    @Inject
    private Converter converter;

    @Inject
    private URClient ur;

    @Inject // conditional bean
    private Optional<ConnectionManager> cm;

    @Inject
    private WebPartnerFlow webFlow;

    public List<PartnerAPI> getPartners() {
        List<PartnerAPI> result = new ArrayList<>();
        repo.findAll().forEach(dbPartner -> {
            result.add(converter.toAPIObject(dbPartner));
        });
        return result;
    }

    public Optional<PartnerAPI> getPartnerById(UUID id) {
        Optional<PartnerAPI> result = Optional.empty();
        Optional<Partner> dbPartner = repo.findById(id);
        if (dbPartner.isPresent()) {
            result = Optional.of(converter.toAPIObject(dbPartner.get()));
        }
        return result;
    }

    public void removePartnerById(UUID id) {
        repo.findById(id).ifPresent(p -> {
            if (p.getConnectionId() != null && cm.isPresent()) {
                cm.get().removeConnection(p.getConnectionId());
            }
        });
        repo.deleteById(id);
    }

    public PartnerAPI addPartnerFlow(@NonNull String did, @Nullable String alias) {
        Optional<Partner> dbPartner = repo.findByDid(did);
        if (dbPartner.isPresent()) {
            throw new PartnerException("Partner for did already exists: " + did);
        }
        PartnerAPI lookupP = lookupPartner(did);

        String connectionLabel = UUID.randomUUID().toString();
        Partner partner = converter.toModelObject(did, lookupP)
                .setLabel(connectionLabel)
                .setAriesSupport(lookupP.getAriesSupport())
                .setAlias(alias)
                .setState("requested");
        Partner result = repo.save(partner); // save before creating the connection
        if (lookupP.getAriesSupport().booleanValue() && cm.isPresent()) {
            cm.get().createConnection(did, connectionLabel, alias);
        }
        return converter.toAPIObject(result);
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
            PartnerAPI pAPI = lookupPartner(dbP.getDid());
            dbP.setValid(pAPI.getValid());
            dbP.setVerifiablePresentation(converter.toMap(pAPI.getVerifiablePresentation()));
            dbP = repo.update(dbP);
            result = Optional.of(converter.toAPIObject(dbP));
        }
        return result;
    }

    @Cacheable(cacheNames = { "partner-lookup-cache" })
    public PartnerAPI lookupPartner(@NonNull String did) {
        Optional<DidDocAPI> didDocument = ur.getDidDocument(did);
        if (didDocument.isPresent()) {
            Optional<Map<String, String>> services = filterServices(didDocument.get());
            if (services.isPresent()) {
                PartnerAPI partner = new PartnerAPI();
                if (services.get().containsKey(EndpointType.Profile.getLedgerName())) {
                    partner = webFlow.lookupPartner(
                            services.get().get(EndpointType.Profile.getLedgerName()),
                            didDocument.get().getPublicKey());
                }
                if (services.get().containsKey(EndpointType.Endpoint.getLedgerName())) {
                    partner.setAriesSupport(Boolean.TRUE);
                } else {
                    partner.setAriesSupport(Boolean.FALSE);
                }
                return partner;
            }
            throw new PartnerException("Could not resolve profile and/or aries endpoint from did document");
        }
        throw new PartnerException("Could not retreive did document from universal resolver");
    }

    static Optional<Map<String, String>> filterServices(@NonNull DidDocAPI doc) {
        Map<String, String> result = null;
        if (doc.getService() != null) {
            result = doc.getService().stream()
                    .filter(s -> EndpointType.Profile.getLedgerName().equals(s.getType())
                            || EndpointType.Endpoint.getLedgerName().equals(s.getType()))
                    .collect(Collectors.toMap(Service::getType, Service::getServiceEndpoint));
        }
        return Optional.ofNullable(result);
    }

}
