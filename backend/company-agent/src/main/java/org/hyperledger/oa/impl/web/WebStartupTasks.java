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
package org.hyperledger.oa.impl.web;

import java.time.Duration;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.hyperledger.aries.AriesClient;
import org.hyperledger.oa.config.runtime.RequiresWeb;
import org.hyperledger.oa.impl.activity.VPManager;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.env.Environment;
import io.micronaut.scheduling.annotation.Async;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@RequiresWeb
@Requires(notEnv = { Environment.TEST })
public class WebStartupTasks {

    @Inject
    private VPManager vpMgmt;

    @Inject
    private WebDidDocManager dicDocMgmt;

    @Inject
    private AriesClient ac;

    @Value("${oagent.host}")
    private String host;

    @Async
    public void onServiceStartedEvent() {
        log.debug("Running web mode startup tasks...");
        vpMgmt.getVerifiablePresentation().ifPresentOrElse(vp -> {
            log.info("VP already exists, skipping: {}", host);
        }, () -> {
            ac.statusWaitUntilReady(Duration.ofSeconds(60));
            log.info("Creating default did document for host: {}", host);
            dicDocMgmt.createIfNeeded(host);
            log.info("Creating default public profile for host: {}", host);
            vpMgmt.recreateVerifiablePresentation();
        });
    }
}
