package org.hyperledger.bpa.impl.rules;

import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.annotation.Async;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.bpa.api.notification.*;
import org.hyperledger.bpa.persistence.repository.PartnerRepository;

import java.util.List;

@Slf4j
@Singleton
public class CredentialRuleEventHandler {

    @Inject
    RulesService rs;

    @Inject
    ApplicationContext appCtx;

    @EventListener
    @Async
    public void onCredentialAcceptedEvent (CredentialAcceptedEvent event) {
        log.debug("detected {}", event.getClass());
    }

    @EventListener
    @Async
    public void onCredentialAddedEvent (CredentialAddedEvent event) {
        log.debug("detected {}", event.getClass());
    }

    @EventListener
    @Async
    public void onCredentialIssuedEvent (CredentialIssuedEvent event) {
        log.debug("detected {}", event.getClass());
    }

    @EventListener
    @Async
    public void onCredentialOfferedEvent (CredentialOfferedEvent event) {
        log.debug("detected {}", event.getClass());
    }

    @EventListener
    @Async
    public void onCredentialProblemEvent (CredentialProblemEvent event) {
        log.debug("detected {}", event.getClass());
    }

    @EventListener
    @Async
    public void onCredentialProposalEvent (CredentialProposalEvent event) {
        log.debug("detected {}", event.getClass());
    }

    public void runRule (Object event) {
        List<RulesData> rules = rs.getAll();
        log.debug("Running event against {} active rules", rules.size());
        rules.parallelStream().forEach(r -> {
            // if (r.getTrigger().apply(event)) {
            //    log.debug("Run rule with id: {}", r.getRuleId());
            //    r.getAction().run(event);
            //}
        });
    }
}
