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
package org.hyperledger.bpa.client;

import io.micronaut.cache.annotation.Cacheable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.Setter;
import org.hyperledger.acy_py.generated.model.DID;
import org.hyperledger.aries.AriesClient;

import java.io.IOException;
import java.util.Optional;

@Singleton
public class CachingAriesClient {

    @Inject
    @Setter
    private AriesClient ac;

    @Cacheable("did-lookup-cache")
    public Optional<DID> walletDidPublic() throws IOException {
        return ac.walletDidPublic();
    }
}
