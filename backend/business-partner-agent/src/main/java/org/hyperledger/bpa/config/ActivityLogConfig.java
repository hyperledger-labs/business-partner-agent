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
package org.hyperledger.bpa.config;

import io.micronaut.context.annotation.Value;
import io.micronaut.core.util.StringUtils;
import lombok.NoArgsConstructor;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.aries.api.present_proof.PresentationExchangeState;
import org.jetbrains.annotations.NotNull;

import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Singleton
@NoArgsConstructor
public class ActivityLogConfig {

    @Value("${bpa.activityLog.connectionStates.activities}")
    private String connectionStatesForActivities;
    @Value("${bpa.activityLog.connectionStates.tasks}")
    private String connectionStatesForTasks;
    @Value("${bpa.activityLog.connectionStates.completed}")
    private String connectionStatesCompleted;

    @Value("${bpa.activityLog.presentationExchangeStates.activities}")
    private String presentationExchangeStatesForActivities;
    @Value("${bpa.activityLog.presentationExchangeStates.tasks}")
    private String presentationExchangeStatesForTasks;
    @Value("${bpa.activityLog.presentationExchangeStates.completed}")
    private String presentationExchangeStatesCompleted;

    public List<ConnectionState> getConnectionStatesForActivities() {
        return getConnectionStates(connectionStatesForActivities);
    }

    public List<ConnectionState> getConnectionStatesForTasks() {
        return getConnectionStates(connectionStatesForTasks);
    }

    public List<ConnectionState> getConnectionStatesCompleted() {
        return getConnectionStates(connectionStatesCompleted);
    }

    public List<PresentationExchangeState> getPresentationExchangeStatesForActivities() {
        return getPresentationExchangeStates(presentationExchangeStatesForActivities);
    }

    public List<PresentationExchangeState> getPresentationExchangeStatesForTasks() {
        return getPresentationExchangeStates(presentationExchangeStatesForTasks);
    }

    public List<PresentationExchangeState> getPresentationExchangeStatesCompleted() {
        return getPresentationExchangeStates(presentationExchangeStatesCompleted);
    }

    @NotNull
    private List<ConnectionState> getConnectionStates(String states) {
        if (StringUtils.isNotEmpty(states)) {
            return List.of(parseStates(states))
                    .stream()
                    .map(m -> ConnectionState.valueOf(getEnumName(m)))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @NotNull
    private List<PresentationExchangeState> getPresentationExchangeStates(String states) {
        if (StringUtils.isNotEmpty(states)) {
            return List.of(parseStates(states))
                    .stream()
                    .map(m -> PresentationExchangeState.valueOf(getEnumName(m)))
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private String getEnumName(String s) {
        return StringUtils.trimToNull(s.toUpperCase(Locale.getDefault()));
    }

    private String[] parseStates(String states) {
        return StringUtils.tokenizeToStringArray(states, ", ", true, true);
    }

}
