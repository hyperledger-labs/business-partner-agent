/*
 * Copyright (c) 2020-2021 - for information on the respective copyright owner
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
package org.hyperledger.bpa.impl.rules;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.bpa.model.ActiveRules;
import org.hyperledger.bpa.repository.RulesRepository;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class RulesService {

    @Inject
    RulesRepository rr;

    public RulesData register(@NonNull RulesData.Trigger trigger, RulesData.Action action) {
        log.debug("Add rule - trigger: {}, action: {}", trigger, action);
        ActiveRules ar = rr.save(ActiveRules
                .builder()
                .trigger(trigger)
                .action(action)
                .build());
        return RulesData.fromActive(ar);
    }

    public Optional<RulesData> update(@NonNull UUID id, @NonNull RulesData.Trigger trigger, RulesData.Action action) {
        log.debug("Update rule - id: {}, trigger: {}, action: {}", id, trigger, action);
        Optional<ActiveRules> dbRule = rr.findById(id);
        if (dbRule.isPresent()) {
            return Optional.of(
                    RulesData.fromActive(
                            rr.update(dbRule.get().setAction(action).setTrigger(trigger))
                    )
            );
        }
        return Optional.empty();
    }

    public List<RulesData> getRules() {
        List<RulesData> result = new ArrayList<>();
        rr.findAll().forEach(active -> result.add(RulesData.fromActive(active)));
        return result;
    }

    public Optional<RulesData> getById(@NonNull UUID ruleId) {
        Optional<ActiveRules> dbRule = rr.findById(ruleId);
        if (dbRule.isPresent()) {
            return Optional.of(RulesData.fromActive(dbRule.get()));
        }
        return Optional.empty();
    }

    public void remove(@NonNull UUID ruleId) {
        log.debug("Removing rule with id: {}", ruleId);
        rr.deleteById(ruleId);
    }
}
