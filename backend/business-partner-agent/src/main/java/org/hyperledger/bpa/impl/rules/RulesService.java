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
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Singleton
@Named("rules2")
public class RulesService {

    @Inject
    RulesRepository rr;

    void register(@NonNull RulesData rule) {
        log.debug("Adding rule: {}", rule);
        rr.save(ActiveRules
                .builder()
                .trigger(rule.getTrigger())
                .action(rule.getAction())
                .build());
    }

    public List<RulesData> getRules() {
        List<RulesData> result = new ArrayList<>();
        rr.findAll().forEach(active -> result.add(RulesData
                .builder()
                .ruleId(active.getId())
                .trigger(active.getTrigger())
                .action(active.getAction())
                .build()));
        return result;
    }

    void remove(@NonNull UUID ruleId) {
        log.debug("Removing rule with id: {}", ruleId);
        rr.deleteById(ruleId);
    }
}
