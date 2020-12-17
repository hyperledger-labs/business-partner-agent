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
    private String host;

    @Value("${bpa.resolver.url}")
    private String uniResolverUrl;

    @Value("${bpa.ledger.browser}")
    private String ledgerBrowser;

    @Value("${bpa.did.prefix}")
    private String ledgerPrefix;

    @Value("${bpa.web.only}")
    private Boolean webOnly;

    @Value("${bpa.name}")
    private String agentName;

    @Value("${bpa.acapy.endpoint}")
    private String acapyEndpoint;

    public String getAgentName() {
        return DidResolver.splitDidFrom(agentName).getLabel();
    }
}
