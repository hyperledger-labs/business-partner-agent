/*
 * Copyright (c) 2020-2022 - for information on the respective copyright owner
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
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.event.ApplicationEventListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.bpa.impl.StartupTasks;
import org.hyperledger.bpa.impl.activity.DidResolver;
import org.hyperledger.bpa.persistence.model.messaging.MessageTrigger;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
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

    @Inject
    UxConfig ux;

    @Value("${bpa.title}")
    String title;

    @Value("${bpa.i18n.locale}")
    String locale;

    @Value("${bpa.i18n.fallbackLocale}")
    String fallbackLocale;

    /** only set when running from .jar */
    String buildVersion;

    @JsonIgnore
    Instant startTime;

    List<MessageTrigger> messageTrigger;

    public String getAgentName() {
        return DidResolver.splitDidFrom(agentName).getLabel();
    }

    public String getUptime() {
        Duration up = Duration.between(startTime, Instant.now());
        return DurationFormatUtils.formatDurationHMS(up.toMillis());
    }

    public List<MessageTrigger> getMessageTrigger() {
        return List.of(MessageTrigger.values());
    }

    @Override
    public void onApplicationEvent(StartupTasks.AcaPyReady event) {

        startTime = Instant.now();
        buildVersion = getClass().getPackage().getImplementationVersion();
        if (buildVersion != null) {
            log.info("BPA running with build version: {}", buildVersion);
        } else {
            buildVersion = "development";
        }

        try {
            this.tailsServerConfigured = ac
                    .statusConfig()
                    .flatMap(c -> c.getAs("tails_server_base_url", String.class))
                    .isPresent();
        } catch (IOException e) {
            log.warn("aca-py is not reachable");
        }
    }

    @Data
    @NoArgsConstructor
    @ConfigurationProperties("bpa.ux")
    public static final class UxConfig {
        private Map<String, Object> buttons;
        private Map<String, Object> theme;
        private Map<String, Object> favicon;
        private UxConfigNavigation navigation;
        private Map<String, Object> header;

        @Data
        @NoArgsConstructor
        @ConfigurationProperties("navigation")
        public static final class UxConfigNavigation {

            private UxConfigNavigationAvatar avatar;
            private UxConfigNavigationSettings settings;
            private UxConfigNavigationAbout about;
            private UxConfigNavigationLogout logout;

            @Data
            @NoArgsConstructor
            @ConfigurationProperties("avatar")
            public static final class UxConfigNavigationAvatar {

                private UxConfigNavigationAvatarAgent agent;

                @Data
                @NoArgsConstructor
                @ConfigurationProperties("agent")
                public static final class UxConfigNavigationAvatarAgent {
                    private Boolean enabled;
                    @JsonIgnore
                    private Boolean def;
                    private String src;
                    private Boolean showName;

                    public void setDefault(Boolean d) {
                        this.def = d;
                    }

                    public Boolean getDefault() {
                        return this.def;
                    }
                }
            }

            @Data
            @NoArgsConstructor
            @ConfigurationProperties("settings")
            public static final class UxConfigNavigationSettings {
                private String location;
            }

            @Data
            @NoArgsConstructor
            @ConfigurationProperties("about")
            public static final class UxConfigNavigationAbout {
                private Boolean enabled;
            }

            @Data
            @NoArgsConstructor
            @ConfigurationProperties("logout")
            public static final class UxConfigNavigationLogout {
                private Boolean enabled;
            }
        }

        @Data
        @NoArgsConstructor
        @ConfigurationProperties("header")
        public static final class UxConfigNavigationHeader {
            private UxConfigNavigation.UxConfigNavigationLogout logout;
        }
    }
}
