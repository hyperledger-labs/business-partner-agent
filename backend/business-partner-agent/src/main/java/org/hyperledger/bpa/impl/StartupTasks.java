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
package org.hyperledger.bpa.impl;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.env.Environment;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.annotation.Async;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.aries.AriesClient;
import org.hyperledger.bpa.impl.activity.VPManager;
import org.hyperledger.bpa.impl.aries.PartnerCredDefLookup;
import org.hyperledger.bpa.impl.aries.config.SchemaService;
import org.hyperledger.bpa.impl.mode.indy.IndyStartupTasks;
import org.hyperledger.bpa.impl.mode.web.WebStartupTasks;
import org.hyperledger.bpa.model.BPAState;
import org.hyperledger.bpa.repository.BPAStateRepository;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.Duration;
import java.util.Iterator;
import java.util.Optional;

@Slf4j
@Singleton
@Requires(notEnv = { Environment.TEST })
public class StartupTasks {

    @Value("${bpa.web.only}")
    Boolean envState;

    @Value("${bpa.host}")
    String host;

    @Inject
    BPAStateRepository stateRepo;

    @Inject
    AriesClient ac;

    @Inject
    SchemaService schemaService;

    @Inject
    PartnerCredDefLookup credLookup;

    @Inject
    VPManager vpMgmt;

    @Inject
    Optional<WebStartupTasks> webTasks;

    @Inject
    Optional<IndyStartupTasks> ariesTasks;

    @Async
    @EventListener
    public void onServiceStartedEvent(@SuppressWarnings("unused") StartupEvent startEvent) {
        checkModeChange();

        ac.statusWaitUntilReady(Duration.ofSeconds(60));

        createDefaultSchemas();

        if (envState) {
            log.info("Running in Web Only mode.");
            webTasks.ifPresent(WebStartupTasks::onServiceStartedEvent);
        } else {
            log.info("Running in Aries mode");
            ariesTasks.ifPresent(IndyStartupTasks::onServiceStartedEvent);
        }

        vpMgmt.getVerifiablePresentation().ifPresentOrElse(
                vp -> log.info("VP already exists, skipping: {}", host),
                () -> {
                    log.info("Creating default public profile for host: {}", host);
                    vpMgmt.recreateVerifiablePresentation();
                });

        credLookup.lookupTypesForAllPartnersAsync();
    }

    private void checkModeChange() {
        Optional<BPAState> dbState = getState();
        if (dbState.isPresent()) {
            Boolean state = dbState.get().getWebOnly();
            if (!state.equals(envState)) {
                String msg;
                if (state.equals(Boolean.TRUE)) {
                    msg = "Switching from web only mode to aries is not supported";
                } else {
                    msg = "Switching from aries to web mode is not supported";
                }
                log.error(msg);
                throw new RuntimeException(msg);
            }
            log.debug("Mode check succeeded");
        } else {
            stateRepo.save(new BPAState(envState));
        }
    }

    private Optional<BPAState> getState() {
        Optional<BPAState> result = Optional.empty();
        final Iterator<BPAState> it = stateRepo.findAll().iterator();
        while (it.hasNext()) {
            result = Optional.of(it.next());
            if (it.hasNext()) {
                throw new RuntimeException("More then one state entry found, db is corrupted");
            }
        }
        return result;
    }

    private void createDefaultSchemas() {
        log.debug("Purging and re-setting default schemas.");

        schemaService.resetWriteOnlySchemas();
    }
}
