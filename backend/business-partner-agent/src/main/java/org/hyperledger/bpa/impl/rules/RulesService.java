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

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.bpa.impl.rules.definitions.BaseRule;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class RulesService {

    private List<BaseRule> tasks = new ArrayList<>();

    void register(@NonNull BaseRule task) {
        log.debug("Adding task: {}", task);
        tasks.add(task);
    }

    List<BaseRule> getActive() {
        return tasks;
    }

    synchronized void removeIfDone(@NonNull UUID taskId) {
        log.debug("Removing task with id: {}", taskId);
        tasks = tasks
                .stream()
                .filter(t -> !t.getTaskId().equals(taskId))
                .collect(Collectors.toList());
    }
}
