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

import io.micronaut.core.util.CollectionUtils;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hyperledger.bpa.impl.rules.definitions.BaseAriesTask;

import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class TaskService {

    private final Map<UUID, List<BaseAriesTask>> tasks = new ConcurrentHashMap<>();

    void register(@NonNull UUID id, @NonNull BaseAriesTask task) {
        log.debug("Adding task for partner: {}", id);
        List<BaseAriesTask> scheduled = tasks.get(id);
        if (scheduled == null) {
            tasks.put(id, new ArrayList<>(List.of(task)));
        } else {
            scheduled.add(task);
        }
    }

    Optional<List<BaseAriesTask>> getActive(@NonNull UUID id) {
        return Optional.ofNullable(tasks.get(id));
    }

    synchronized void removeIfDone(@NonNull UUID id, @NonNull UUID taskId) {
        List<BaseAriesTask> scheduled = tasks.get(id);
        if (scheduled != null) {
            log.debug("Removing task for partner: {}", id);
            List<BaseAriesTask> filtered = scheduled
                    .stream()
                    .filter(t -> !t.getTaskId().equals(taskId))
                    .collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(filtered)) {
                tasks.put(id, filtered);
            } else {
                tasks.remove(id);
            }
        }
    }
}
