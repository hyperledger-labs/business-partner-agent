/*
  Copyright (c) 2020 - for information on the respective copyright owner
  see the NOTICE file and/or the repository at
  https://github.com/hyperledger-labs/organizational-agent

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
package org.hyperledger.oa;

import io.micronaut.context.annotation.Value;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.AriesClient;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URL;

@Slf4j
@Testcontainers
@MicronautTest
public abstract class RunWithAries extends BaseTest {

    private static final String ARIES_VERSION = "bcgovimages/aries-cloudagent:py36-1.15-0_0.5.6";
    /** Container local port, the mapped port is random */
    private static final Integer ARIES_ADMIN_PORT = 8031;

    protected AriesClient ac;

    @Value("${oagent.acapy.url}")
    private String url;

    @Container
    private final GenericContainer<?> ariesContainer;

    @SuppressWarnings("resource")
    public RunWithAries() {
        ariesContainer = new GenericContainer<>(ARIES_VERSION)
                .withExposedPorts(ARIES_ADMIN_PORT)
                .withCommand("start"
                        + " -it http 0.0.0.0 8030"
                        + " -ot http --admin 0.0.0.0 " + ARIES_ADMIN_PORT
                        + " --admin-insecure-mode"
                        + " -e http://0.0.0.0"
                        + " --log-level info"
                        + " --no-ledger"
                        + " --plugin aries_cloudagent.messaging.jsonld")
                .waitingFor(Wait.defaultWaitStrategy())
                .withLogConsumer(new Slf4jLogConsumer(log));

        runWithProxyIfConfigured(ariesContainer);
    }

    @BeforeEach
    void setupAries() throws Exception {
        URL endp = new URL(url);
        final String ariesTestUrl = "http://"
                + endp.getHost() + ":" + ariesContainer.getMappedPort(ARIES_ADMIN_PORT);
        log.debug("Aries test URL: {}", ariesTestUrl);
        ac = new AriesClient(ariesTestUrl);
    }

    protected void runWithProxyIfConfigured(GenericContainer<?> container) {
        String http_proxy = System.getProperty("http_proxy", "");
        if (!http_proxy.equals("")) {
            container.withEnv("http_proxy", http_proxy);
        }
        String https_proxy = System.getProperty("https_proxy", "");
        if (!https_proxy.equals("")) {
            container.withEnv("https_proxy", https_proxy);
        }
    }
}
