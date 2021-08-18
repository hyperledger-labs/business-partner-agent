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

import lombok.Getter;
import org.hyperledger.aries.api.connection.ConnectionState;
import org.hyperledger.aries.api.present_proof.PresentationExchangeState;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
@Getter
public class ActivityLogConfig {
    /*
     * For now, we are not sure what to do with this configuration. Will each
     * installation determine what their own definition of "complete" is, or which
     * events will be "tasks"? And tasks are going to be dependant in the
     * AcaPyConfig, which flags are set to auto respond...
     */

    private static final List<ConnectionState> CONNECTION_STATES_TASKS = List.of(ConnectionState.REQUEST);

    private static final List<ConnectionState> CONNECTION_STATES_COMPLETED = List.of(ConnectionState.ACTIVE,
            ConnectionState.RESPONSE,
            ConnectionState.COMPLETED,
            ConnectionState.PING_RESPONSE,
            ConnectionState.PING_NO_RESPONSE);

    private static final List<PresentationExchangeState> PRESENTATION_EXCHANGE_STATES_TASKS = List
            .of(PresentationExchangeState.REQUEST_RECEIVED);

    private static final List<PresentationExchangeState> PRESENTATION_EXCHANGE_STATES_COMPLETED = List.of(
            PresentationExchangeState.VERIFIED,
            PresentationExchangeState.PRESENTATION_ACKED);

    private final List<ConnectionState> connectionStatesForActivities;
    private final List<ConnectionState> connectionStatesForCompleted;
    private final List<ConnectionState> connectionStatesForTasks;
    private final List<PresentationExchangeState> presentationExchangeStatesForActivities;
    private final List<PresentationExchangeState> presentationExchangeStatesForCompleted;
    private final List<PresentationExchangeState> presentationExchangeStatesForTasks;

    private final AcaPyConfig acaPyConfig;

    @Inject
    ActivityLogConfig(AcaPyConfig acaPyConfig) {
        this.acaPyConfig = acaPyConfig;
        // 1. set the tasks lists first as they depend on aca py configuration
        connectionStatesForTasks = this.isConnectionRequestTask() ? CONNECTION_STATES_TASKS : List.of();
        presentationExchangeStatesForTasks = this.isPresentationExchangeTask() ? PRESENTATION_EXCHANGE_STATES_TASKS
                : List.of();

        // 2. set the completed state lists
        connectionStatesForCompleted = CONNECTION_STATES_COMPLETED;
        presentationExchangeStatesForCompleted = PRESENTATION_EXCHANGE_STATES_COMPLETED;

        // 3. build the activity lists based on task and completed lists
        connectionStatesForActivities = this.buildConnectionStatesForActivities();
        presentationExchangeStatesForActivities = this.buildPresentationExchangeStatesForActivities();
    }

    private List<ConnectionState> buildConnectionStatesForActivities() {
        List<ConnectionState> results = new ArrayList<>(this.getConnectionStatesForCompleted());
        results.addAll(this.getConnectionStatesForTasks());
        results.add(ConnectionState.INVITATION);
        return List.copyOf(results);
    }

    private boolean isConnectionRequestTask() {
        return !this.acaPyConfig.getAutoAcceptRequests();
    }

    public List<PresentationExchangeState> buildPresentationExchangeStatesForActivities() {
        List<PresentationExchangeState> results = new ArrayList<>(this.getPresentationExchangeStatesForCompleted());
        results.addAll(this.getPresentationExchangeStatesForTasks());
        results.add(PresentationExchangeState.REQUEST_SENT);
        return List.copyOf(results);
    }

    private boolean isPresentationExchangeTask() {
        return !this.acaPyConfig.getAutoRespondPresentationRequest();
    }

}
