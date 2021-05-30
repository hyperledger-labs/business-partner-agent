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
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.aries.api.wallet.WalletDidResponse;
import org.hyperledger.bpa.api.ApiConstants;
import org.hyperledger.bpa.api.DidDocAPI;
import org.hyperledger.bpa.api.exception.NetworkException;
import org.hyperledger.bpa.client.CachingAriesClient;
import org.hyperledger.bpa.client.URClient;

import io.micronaut.core.annotation.Nullable;
import org.hyperledger.bpa.config.RuntimeConfig;
import org.hyperledger.bpa.impl.util.AriesStringUtil;

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

    @Inject
    RuntimeConfig rc;

    public @Nullable String getMyDid() {
        String myDid = null;
        if (webOnly) {
            myDid = ApiConstants.DID_METHOD_WEB + host;
        } else {
            try {
                Optional<WalletDidResponse> walletDid = acaCache.walletDidPublic();
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
            Optional<WalletDidResponse> walletDid = acaCache.walletDidPublic();
            if (walletDid.isEmpty()) {
                log.warn("No public did available, falling back to local did. VP can only be validated locally.");
                final Optional<List<WalletDidResponse>> walletDids = acaPy.walletDid();
                if (walletDids.isPresent() && !walletDids.get().isEmpty()) {
                    walletDid = Optional.of(walletDids.get().get(0));
                } else {
                    walletDid = acaPy.walletDidCreate();
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

    public boolean isSchemaCreator(@NonNull String schemaId, @NonNull String did) {
        // we can place logic in here that will deal with different did formats later
        final String schemaCreator = AriesStringUtil.schemaGetCreator(schemaId);
        String creatorDid = did;
        if (StringUtils.startsWith(did, rc.getLedgerPrefix())) {
            creatorDid = AriesStringUtil.getLastSegment(did);
        }
        return StringUtils.equals(schemaCreator, creatorDid);
    }

    public boolean isMySchema(@NonNull String schemaId) {
        String myDid = this.getMyDid();
        if (StringUtils.isNotEmpty(myDid)) {
            return this.isSchemaCreator(schemaId, myDid);
        }
        return false;
    }

}
