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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.api.ledger.EndpointType;
import org.hyperledger.oa.api.ApiConstants;
import org.hyperledger.oa.api.DidDocAPI;
import org.hyperledger.oa.api.DidDocAPI.Service;
import org.hyperledger.oa.config.runtime.RequiresWeb;
import org.hyperledger.oa.impl.DidDocManager;
import org.hyperledger.oa.impl.activity.Identity;
import org.hyperledger.oa.impl.util.Converter;
import org.hyperledger.oa.model.DidDocWeb;
import org.hyperledger.oa.repository.DidDocWebRepository;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Singleton
@RequiresWeb
public class WebDidDocManager implements DidDocManager {

    @Value("${oagent.acapy.endpoint}")
    String acapyEndpoint;

    @Inject
    DidDocWebRepository didRepo;

    @Inject
    ObjectMapper mapper;

    @Inject
    Identity id;

    public void createDidDocument(String host) {

        String verkey = null;
        final Optional<String> ver = id.getVerkey();
        if (ver.isPresent()) {
            verkey = ver.get();
        }

        String myDid = id.getMyDid();
        String myKeyId = id.getMyKeyId(myDid);

        List<DidDocAPI.VerificationMethod> verificationMethods = List.of(DidDocAPI.VerificationMethod.builder()
                .id(myKeyId)
                .type(ApiConstants.DEFAULT_VERIFICATION_KEY_TYPE)
                .publicKeyBase58(verkey)
                .build());

        List<DidDocAPI.PublicKey> publicKey = List.of(DidDocAPI.PublicKey.builder()
                .id(myKeyId)
                .type(ApiConstants.DEFAULT_VERIFICATION_KEY_TYPE)
                .publicKeyBase58(verkey)
                .build());

        DidDocAPI didDoc = DidDocAPI.builder()
                .id(myDid)
                .service(List.of(
                        Service.builder()
                                .serviceEndpoint("https://" + host + "/profile.jsonld")
                                .id(myDid + "#" + EndpointType.Profile.getLedgerName())
                                .type(EndpointType.Profile.getLedgerName())
                                .build(),
                        Service.builder()
                                .serviceEndpoint(acapyEndpoint)
                                .id(myDid + "#" + EndpointType.Endpoint.getLedgerName())
                                .type(EndpointType.Endpoint.getLedgerName())
                                .build()))
                .verificationMethod(mapper.convertValue(verificationMethods, JsonNode.class))
                .publicKey(publicKey)
                .build();

        try {
            Map<String, Object> didDocDb = mapper.convertValue(didDoc, Converter.MAP_TYPEREF);
            didRepo.findDidDocSingle().ifPresentOrElse(
                    dd -> didRepo.updateDidDoc(dd.getId(), didDocDb),
                    () -> didRepo.save(DidDocWeb.builder().didDoc(didDocDb).build()));
        } catch (IllegalArgumentException e) {
            log.error("Could not convert did document", e);
        }
    }

    @Override
    public Optional<DidDocAPI> getDidDocument() {
        Optional<DidDocAPI> result = Optional.empty();
        Optional<DidDocWeb> didDocDB = didRepo.findDidDocSingle();
        if (didDocDB.isPresent()) {
            try {
                DidDocAPI api = mapper.convertValue(didDocDB.get().getDidDoc(), DidDocAPI.class);
                result = Optional.of(api);
            } catch (IllegalArgumentException e) {
                log.error("Could not convert persisted did document", e);
            }
        }
        return result;
    }
}
