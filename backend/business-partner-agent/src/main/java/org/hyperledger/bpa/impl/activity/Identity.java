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
package org.hyperledger.bpa.impl.activity;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.annotation.Nullable;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.acy_py.generated.model.DID;
import org.hyperledger.acy_py.generated.model.DIDCreate;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.bpa.api.ApiConstants;
import org.hyperledger.bpa.api.DidDocAPI;
import org.hyperledger.bpa.api.exception.NetworkException;
import org.hyperledger.bpa.client.CachingAriesClient;
import org.hyperledger.bpa.client.URClient;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Singleton
public class Identity {

    @Value("${bpa.host}")
    String host;

    @Value("${bpa.web.only}")
    boolean webOnly;

    @Getter
    @Value("${bpa.did.prefix}")
    String didPrefix;

    @Inject
    @Setter
    AriesClient acaPy;

    @Inject
    @Setter
    CachingAriesClient acaCache;

    @Inject
    URClient ur;

    @Inject
    ObjectMapper mapper;

    public @Nullable String getMyDid() {
        String myDid = null;
        if (webOnly) {
            myDid = ApiConstants.DID_METHOD_WEB + host;
        } else {
            try {
                Optional<DID> walletDid = acaCache.walletDidPublic();
                if (walletDid.isPresent()) {
                    myDid = didPrefix + walletDid.get().getDid();
                }
            } catch (IOException e) {
                log.error("aca-py not reachable", e);
            }
        }
        return myDid;
    }

    public @Nullable String getMyKeyId(String myDid) {
        String myKeyId = "no-public-did";
        if (myDid != null) {
            if (webOnly) {
                myKeyId = myDid + ApiConstants.DEFAULT_KEY_ID;
            } else {
                final Optional<DidDocAPI> didDoc = ur.getDidDocument(myDid);
                if (didDoc.isPresent()) {
                    Optional<DidDocAPI.VerificationMethod> vm = didDoc.get().getVerificationMethod(mapper).stream()
                            .filter(k -> ApiConstants.DEFAULT_VERIFICATION_KEY_TYPE.equals(k.getType())).findFirst();
                    if (vm.isPresent()) {
                        myKeyId = vm.get().getId();
                    }
                }
            }
        }
        return myKeyId;
    }

    public Optional<String> getVerkey() {
        Optional<String> verkey = Optional.empty();
        try {
            Optional<DID> walletDid = acaCache.walletDidPublic();
            if (walletDid.isEmpty()) {
                log.warn("No public did available, falling back to local did. VP can only be validated locally.");
                final Optional<List<DID>> walletDids = acaPy.walletDid();
                if (walletDids.isPresent() && !walletDids.get().isEmpty()) {
                    walletDid = Optional.of(walletDids.get().get(0));
                } else {
                    walletDid = acaPy.walletDidCreate(DIDCreate.builder().method(DIDCreate.MethodEnum.SOV).build());
                }
            }
            if (walletDid.isPresent()) {
                verkey = Optional.of(walletDid.get().getVerkey());
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            throw new NetworkException("aca-py not available");
        }
        return verkey;
    }
}
