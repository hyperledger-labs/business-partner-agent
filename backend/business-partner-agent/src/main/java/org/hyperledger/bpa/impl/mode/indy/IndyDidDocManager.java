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
package org.hyperledger.bpa.impl.mode.indy;

import org.hyperledger.acy_py.generated.model.DID;
import org.hyperledger.aries.api.resolver.DIDDocument;
import org.hyperledger.bpa.api.exception.NetworkException;
import org.hyperledger.bpa.client.CachingAriesClient;
import org.hyperledger.bpa.client.URClient;
import org.hyperledger.bpa.config.runtime.RequiresIndy;
import org.hyperledger.bpa.impl.DidDocManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Optional;

@Singleton
@RequiresIndy
public class IndyDidDocManager implements DidDocManager {

    @Inject
    CachingAriesClient ac;

    @Inject
    URClient ur;

    /**
     * In this case the did document is always on the ledger, so this method will
     * resolve the did document via aca-py.
     *
     * @return {@link DIDDocument}.
     */
    @Override
    public Optional<DIDDocument> getDidDocument() {
        try {
            final Optional<DID> pubDid = ac.walletDidPublic();
            if (pubDid.isPresent()) {
                return ur.getDidDocument("did:" + pubDid.get().getMethod().getValue() + ":" + pubDid.get().getDid());
            }

        } catch (IOException e) {
            throw new NetworkException("aca-py not reachable", e);
        }
        return Optional.empty();
    }

}
