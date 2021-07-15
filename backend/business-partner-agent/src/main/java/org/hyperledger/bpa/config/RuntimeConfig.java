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

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.event.ApplicationEventListener;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.bpa.impl.StartupTasks;
import org.hyperledger.bpa.impl.activity.DidResolver;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Map;

@Getter
@Singleton
@NoArgsConstructor
@Slf4j
public class RuntimeConfig implements ApplicationEventListener<StartupTasks.AcaPyReady> {
    @JsonIgnore
    @Inject
    AriesClient ac;

    Boolean tailsServerConfigured;

    @Value("${bpa.host}")
    String host;

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

    @Property(name = "bpa.ux")
    Map<String, Object> ux;

    @Value("${bpa.title}")
    String title;

    @Value("${bpa.i18n.locale}")
    String locale;

    @Value("${bpa.i18n.fallbackLocale}")
    String fallbackLocale;

    @Value("${bpa.endorser.role}")
    String endorserRole;

    public String getAgentName() {
        return DidResolver.splitDidFrom(agentName).getLabel();
    }

    @Override
    public void onApplicationEvent(StartupTasks.AcaPyReady event) {
        try {
            this.tailsServerConfigured = ac
                    .statusConfig()
                    .flatMap(c -> c.getAs("tails_server_base_url", String.class))
                    .isPresent();
        } catch (IOException e) {
            log.warn("aca-py is not reachable");
        }
    }

    public boolean hasEndorserRole() {
        return (getEndorserRole() != null && !getEndorserRole().trim().isEmpty());
    }
}
