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
package org.hyperledger.oa.registry.impl;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.hyperledger.oa.registry.repository.RegistryRepository;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.annotation.Async;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@Requires(notEnv = { Environment.TEST })
public class StartupTasks {

    @Inject
    private RegistryRepository repo;

    @Async
    @EventListener
    public void onServiceStartedEvent(StartupEvent startupEvent) {
        log.info("Running DB tasks", startupEvent);

        // text idx
        repo.createTextIdx();

        // unique company name
        repo.createDidIdx();
    }
}
