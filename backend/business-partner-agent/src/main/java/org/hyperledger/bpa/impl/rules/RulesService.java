package org.hyperledger.bpa.impl.rules;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.bpa.persistence.model.ActiveRules;
import org.hyperledger.bpa.persistence.repository.RulesRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Singleton
public class RulesService {

    @Inject
    RulesRepository rr;

    public RulesData add(@NonNull RulesData.Trigger trigger, RulesData.Action action) {
        log.debug("Add rule - trigger: {}, action: {}", trigger, action);
        ActiveRules ar = rr.save(ActiveRules
                .builder()
                .trigger(trigger)
                .action(action)
                .build());
        return RulesData.fromActive(ar);
    }

    public Optional<RulesData> update(
            @NonNull UUID id, @NonNull RulesData.Trigger trigger, @NonNull RulesData.Action action) {
        log.debug("Update rule - id: {}, trigger: {}, action: {}", id, trigger, action);
        Optional<ActiveRules> dbRule = rr.findById(id);
        return dbRule.map(activeRules -> RulesData.fromActive(
                rr.update(activeRules.setAction(action).setTrigger(trigger))));
    }

    public List<RulesData> getAll() {
        List<RulesData> result = new ArrayList<>();
        rr.findAll().forEach(active -> result.add(RulesData.fromActive(active)));
        return result;
    }

    public Optional<RulesData> get(@NonNull UUID ruleId) {
        Optional<ActiveRules> dbRule = rr.findById(ruleId);
        return dbRule.map(RulesData::fromActive);
    }

    public void delete(@NonNull UUID ruleId) {
        log.debug("Delete rule with id: {}", ruleId);
        rr.deleteById(ruleId);
    }
}