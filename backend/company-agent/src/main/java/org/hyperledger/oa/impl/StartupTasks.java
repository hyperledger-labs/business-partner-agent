package org.hyperledger.oa.impl;

import java.util.Iterator;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.hyperledger.oa.impl.aries.AriesStartupTasks;
import org.hyperledger.oa.impl.web.WebStartupTasks;
import org.hyperledger.oa.model.BPAState;
import org.hyperledger.oa.repository.BPAStateRepository;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.env.Environment;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.runtime.event.annotation.EventListener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@Requires(notEnv = { Environment.TEST })
public class StartupTasks {

    @Value("${oagent.web.only}")
    private Boolean envState;

    @Inject
    private BPAStateRepository stateRepo;

    @Inject
    private Optional<WebStartupTasks> webTasks;

    @Inject
    private Optional<AriesStartupTasks> ariesTasks;

    @EventListener
    public void onServiceStartedEvent(@SuppressWarnings("unused") StartupEvent startEvent) {
        checkModeChange();

        if (envState.booleanValue()) {
            log.info("Running in Web Only mode.");
            webTasks.ifPresent(at -> at.onServiceStartedEvent());
        } else {
            log.info("Running in Aries mode");
            ariesTasks.ifPresent(at -> at.onServiceStartedEvent());
        }
    }

    private void checkModeChange() {
        Optional<BPAState> dbState = getState();
        if (dbState.isPresent()) {
            Boolean state = dbState.get().getWebOnly();
            if (!state.equals(envState)) {
                String msg;
                if (state.equals(Boolean.TRUE) && envState.equals(Boolean.FALSE)) {
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
}
