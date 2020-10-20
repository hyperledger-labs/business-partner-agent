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
package org.hyperledger.oa.impl.aries;

import java.io.IOException;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.hyperledger.aries.api.wallet.WalletDidResponse;
import org.hyperledger.oa.api.DidDocAPI;
import org.hyperledger.oa.api.exception.NetworkException;
import org.hyperledger.oa.client.CachingAriesClient;
import org.hyperledger.oa.client.URClient;
import org.hyperledger.oa.config.runtime.RequiresAries;
import org.hyperledger.oa.impl.DidDocManager;

import io.micronaut.context.annotation.Value;

@Singleton
@RequiresAries
public class AriesDidDocManager implements DidDocManager {

    @Value("${oagent.did.prefix}")
    String didPrefix;

    @Inject
    CachingAriesClient ac;

    @Inject
    URClient ur;

    /**
     * In this case the did document is always on the ledger, so this method will
     * resolve the did document via the uniresolver.
     *
     * @return {@link DidDocAPI}.
     */
    @Override
    public Optional<DidDocAPI> getDidDocument() {
        try {
            final Optional<WalletDidResponse> pubDid = ac.walletDidPublic();
            if (pubDid.isPresent()) {
                return ur.getDidDocument(didPrefix + pubDid.get().getDid());
            }

        } catch (IOException e) {
            throw new NetworkException("aca-py not reachable", e);
        }
        return Optional.empty();
    }

}
