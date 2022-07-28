package org.hyperledger.bpa.impl.rules;

import io.micronaut.runtime.event.annotation.EventListener;
import io.micronaut.scheduling.annotation.Async;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.bpa.api.notification.*;
import org.hyperledger.bpa.impl.PartnerManager;
import org.hyperledger.bpa.impl.TagService;
import org.hyperledger.bpa.persistence.repository.PartnerRepository;
import org.hyperledger.bpa.persistence.repository.TagRepository;

import java.util.List;

@Slf4j
@Singleton
public class PartnerRuleEventHandler {

    @Inject
    RulesService rs;

    @Inject
    PartnerRepository partnerRepo;

    @Inject
    TagService tagService;

    @Inject
    PartnerManager partnerManager;

    @Inject
    TagRepository tagRepo;

    @EventListener
    @Async
    public void onPartnerAddedEvent(PartnerAddedEvent event) {
        log.debug("detected {}", event.getClass());
        runRule(event);
    }

    @EventListener
    @Async
    public void onPartnerAcceptedEvent(PartnerAcceptedEvent event) {
        log.debug("detected {}", event.getClass());
        runRule(event);
    }

    @EventListener
    @Async
    public void onPartnerRemovedEvent(PartnerRemovedEvent event) {
        log.debug("detected {}", event.getClass());
        runRule(event);
    }

    @EventListener
    @Async
    public void onPartnerRequestReceivedEvent(PartnerRequestReceivedEvent event) {
        log.debug("detected {}", event.getClass());
        runRule(event);
    }

    @EventListener
    @Async
    public void onPartnerRequestCompletedEvent(PartnerRequestCompletedEvent event) {
        log.debug("detected {}", event.getClass());
        runRule(event);
    }

    public void runRule(Event event) {
        List<RulesData> rules = rs.getAll();
        log.debug("Running event against {} active rules", rs.getAll().size());
        EventContext ctx = EventContext.builder().partnerRepo(partnerRepo)
                .tagService(tagService).partnerManager(partnerManager)
                .tagRepo(tagRepo).partner(((PartnerEvent) event).getPartner()).build();
        rules.parallelStream().forEach(r -> {
            if (r.getTrigger().apply(event)) {
                log.debug("Run rule with event id: {}", r.getRuleId());
                r.getAction().run(ctx);
            }
        });
    }
}
