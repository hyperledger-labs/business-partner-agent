/*
  Copyright (c) 2020 - for information on the respective copyright owner
  see the NOTICE file and/or the repository at
  https://github.com/hyperledger-labs/business-partner-agent

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package org.hyperledger.oa.impl.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.api.ledger.EndpointType;
import org.hyperledger.oa.api.ApiConstants;
import org.hyperledger.oa.api.DidDocAPI;
import org.hyperledger.oa.api.DidDocAPI.PublicKey;
import org.hyperledger.oa.api.DidDocAPI.Service;
import org.hyperledger.oa.config.runtime.RequiresWeb;
import org.hyperledger.oa.impl.DidDocManager;
import org.hyperledger.oa.impl.activity.Identity;
import org.hyperledger.oa.impl.util.Converter;
import org.hyperledger.oa.model.DidDocWeb;
import org.hyperledger.oa.repository.DidDocWebRepository;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Singleton
@RequiresWeb
public class WebDidDocManager implements DidDocManager {

    @Inject
    private DidDocWebRepository didRepo;

    @Inject
    private ObjectMapper mapper;

    @Inject
    private Identity id;

    public DidDocAPI createIfNeeded(String host) {
        Optional<DidDocAPI> dbDid = getDidDocument();
        if (dbDid.isPresent()) {
            return dbDid.get();
        }

        String verkey = null;
        final Optional<String> ver = id.getVerkey();
        if (ver.isPresent()) {
            verkey = ver.get();
        }

        String myDid = id.getMyDid();
        String myKeyId = id.getMyKeyId(myDid);

        DidDocAPI didDoc = DidDocAPI.builder()
                .id(myDid)
                .service(List.of(
                        Service.builder()
                                .serviceEndpoint("https://" + host + "/profile.jsonld")
                                .id(myDid + "#" + EndpointType.Profile.getLedgerName())
                                .type(EndpointType.Profile.getLedgerName())
                                .build()))
                .publicKey(List.of(
                        PublicKey.builder()
                                .id(myKeyId)
                                .type(ApiConstants.DEFAULT_VERIFICATION_KEY_TYPE)
                                .publicKeyBase58(verkey)
                                .build()))
                .build();

        try {
            Map<String, Object> didDocDb = mapper.convertValue(didDoc, Converter.MAP_TYPEREF);
            didRepo.save(DidDocWeb.builder().didDoc(didDocDb).build());
        } catch (IllegalArgumentException e) {
            log.error("", e);
        }

        return didDoc;
    }

    @Override
    public Optional<DidDocAPI> getDidDocument() {
        Optional<DidDocAPI> result = Optional.empty();
        Iterator<DidDocWeb> iterator = didRepo.findAll().iterator();
        if (iterator.hasNext()) {
            try {
                DidDocWeb db = iterator.next();
                DidDocAPI api = mapper.convertValue(db.getDidDoc(), DidDocAPI.class);
                result = Optional.of(api);
            } catch (IllegalArgumentException e) {
                log.error("", e);
            }
            if (iterator.hasNext()) {
                throw new IllegalStateException("More than one did document was found");
            }
        }
        return result;
    }
}
