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
package org.hyperledger.bpa.config;

import io.micronaut.context.annotation.Value;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hyperledger.bpa.impl.activity.DidResolver;

import javax.inject.Singleton;

@Getter
@Singleton
@NoArgsConstructor
public class RuntimeConfig {

    @Value("${bpa.host}")
    String host;

    @Value("${bpa.resolver.url}")
    String uniResolverUrl;

    @Value("${bpa.ledger.browser}")
    String ledgerBrowser;

    @Value("${bpa.did.prefix}")
    String ledgerPrefix;

    @Value("${bpa.web.only}")
    Boolean webOnly;

    @Value("${bpa.name}")
    String agentName;

    @Value("${bpa.acapy.endpoint}")
    String acapyEndpoint;

    @Value("${bpa.imprint.url}")
    String imprint;

    @Value("${bpa.privacy.policy.url}")
    String dataPrivacyPolicy;

    @Value("${bpa.creddef.revocationRegistrySize}")
    Integer revocationRegistrySize;

    public String getAgentName() {
        return DidResolver.splitDidFrom(agentName).getLabel();
    }
}
