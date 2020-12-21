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
package org.hyperledger.bpa.impl.mode.web;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.env.Environment;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.bpa.config.runtime.RequiresWeb;

import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
@RequiresWeb
@Requires(notEnv = { Environment.TEST })
public class WebStartupTasks {

    @Value("${bpa.host}")
    String host;

    @Inject
    WebDidDocManager dicDocMgmt;

    public void onServiceStartedEvent() {
        log.debug("Running web mode startup tasks...");

        log.info("Creating did document for host: {}", host);
        dicDocMgmt.createDidDocument(host);
    }
}
